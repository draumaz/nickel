package io.github.draumaz.nickel.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.draumaz.nickel.data.Categories
import io.github.draumaz.nickel.data.Category
import io.github.draumaz.nickel.data.ImportException
import io.github.draumaz.nickel.data.ImportPlan
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.data.LedgerCsv
import io.github.draumaz.nickel.data.Settings
import io.github.draumaz.nickel.data.SettingsRepository
import io.github.draumaz.nickel.data.Transaction
import io.github.draumaz.nickel.data.TransactionRepository
import io.github.draumaz.nickel.export.ExportShare
import io.github.draumaz.nickel.widget.NickelWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class Filter { ALL, INCOME, EXPENSE }

data class Totals(val netCents: Long, val incomeCents: Long, val expenseCents: Long)

/** Figures for the banner while the list is narrowed to one kind, or one category of it. */
data class FilterSummary(
    val kind: Kind,
    /** The category being filtered to, or `null` for every category of [kind]. */
    val category: Category?,
    val totalCents: Long,
    val thisMonthCents: Long,
    val lastMonthCents: Long,
    /** [category]'s share of everything of [kind], from 0 to 1; `null` without a category. */
    val share: Float?,
)

data class HomeState(
    val loading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val customCategories: List<Category> = emptyList(),
    val totals: Totals = Totals(0, 0, 0),
    val filter: Filter = Filter.ALL,
    /** Categories that have transactions of the filtered kind, offered as a second filter. */
    val filterCategories: List<Category> = emptyList(),
    /** Id of the category being filtered to, if any. */
    val categoryFilter: String? = null,
    /** `null` while showing everything. */
    val summary: FilterSummary? = null,
    val isEmpty: Boolean = true,
    val count: Int = 0,
)

sealed interface TransferState {
    data object Idle : TransferState
    data object Reading : TransferState
    data class Preview(val plan: ImportPlan, val fileName: String?) : TransferState
    data object Importing : TransferState
    data class Failed(val message: String) : TransferState
}

class NickelViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TransactionRepository(app)
    private val settingsRepository = SettingsRepository(app)
    private val filter = MutableStateFlow(Filter.ALL)
    private val categoryFilter = MutableStateFlow<String?>(null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val settings: StateFlow<Settings> = settingsRepository.settings
    val dynamicColorSupported: Boolean = settingsRepository.dynamicColorSupported

    private val _transfer = MutableStateFlow<TransferState>(TransferState.Idle)
    val transfer: StateFlow<TransferState> = _transfer.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-off messages for a snackbar. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    val state: StateFlow<HomeState> = combine(repository.ledger, filter, categoryFilter) { ledger, filter, categoryId ->
        if (ledger == null) return@combine HomeState(loading = true, filter = filter)
        val all = ledger.transactions
        val income = all.filter { it.kind == Kind.INCOME }.sumOf { it.amountCents }
        val expense = all.filter { it.kind == Kind.EXPENSE }.sumOf { it.amountCents }
        val ofKind = when (filter) {
            Filter.ALL -> all
            Filter.INCOME -> all.filter { it.kind == Kind.INCOME }
            Filter.EXPENSE -> all.filter { it.kind == Kind.EXPENSE }
        }
        val filterCategories = when (filter) {
            Filter.ALL -> emptyList()
            else -> {
                val used = ofKind.mapTo(HashSet()) { it.category.id }
                val kind = if (filter == Filter.INCOME) Kind.INCOME else Kind.EXPENSE
                Categories.forKind(kind, ledger.customCategories).filter { it.id in used }
            }
        }
        // A category with nothing left in it (say, its last transaction was deleted) stops filtering.
        val activeCategory = categoryId?.takeIf { id -> filterCategories.any { it.id == id } }
        val shown = if (activeCategory == null) ofKind else ofKind.filter { it.category.id == activeCategory }
        HomeState(
            loading = false,
            transactions = shown,
            customCategories = ledger.customCategories,
            totals = Totals(income - expense, income, expense),
            filter = filter,
            filterCategories = filterCategories,
            categoryFilter = activeCategory,
            summary = if (filter == Filter.ALL) null else summarize(
                kind = if (filter == Filter.INCOME) Kind.INCOME else Kind.EXPENSE,
                category = filterCategories.firstOrNull { it.id == activeCategory },
                shown = shown,
                ofKind = ofKind,
            ),
            isEmpty = all.isEmpty(),
            count = all.size,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    init {
        viewModelScope.launch { repository.load() }
        // Redraw home-screen widgets whenever the ledger changes after the first load.
        viewModelScope.launch {
            repository.ledger.filterNotNull().drop(1).collect {
                runCatching { NickelWidget().updateAll(getApplication()) }
            }
        }
    }

    private fun summarize(kind: Kind, category: Category?, shown: List<Transaction>, ofKind: List<Transaction>): FilterSummary {
        val zone = ZoneId.systemDefault()
        val thisMonth = YearMonth.now(zone)
        fun monthOf(t: Transaction) = YearMonth.from(Instant.ofEpochMilli(t.timestamp).atZone(zone))
        val total = shown.sumOf { it.amountCents }
        val kindTotal = ofKind.sumOf { it.amountCents }
        return FilterSummary(
            kind = kind,
            category = category,
            totalCents = total,
            thisMonthCents = shown.filter { monthOf(it) == thisMonth }.sumOf { it.amountCents },
            lastMonthCents = shown.filter { monthOf(it) == thisMonth.minusMonths(1) }.sumOf { it.amountCents },
            share = if (category == null || kindTotal == 0L) null else total.toFloat() / kindTotal,
        )
    }

    fun setFilter(value: Filter) {
        if (filter.value != value) categoryFilter.value = null
        filter.value = value
    }

    /** Filters to [id] within the current kind, or clears the category filter with `null`. */
    fun setCategoryFilter(id: String?) {
        categoryFilter.value = id
    }

    fun setDynamicColor(enabled: Boolean) = settingsRepository.setDynamicColor(enabled)

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            repository.load()
            // Let the loading indicator finish a morph or two so the refresh reads as intentional.
            delay(700)
            _refreshing.value = false
        }
    }

    fun save(transaction: Transaction) {
        viewModelScope.launch { repository.upsert(transaction) }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repository.delete(transaction.id) }
    }

    /** Reads a CSV and prepares a preview; nothing is written until [confirmImport]. */
    fun prepareImport(uri: Uri) {
        _transfer.value = TransferState.Reading
        viewModelScope.launch {
            // Keep the spinner up for a beat even on tiny files, so it doesn't just flash.
            val minimumShow = async { delay(600) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    val text = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: throw ImportException("Couldn't open that file.")
                    val ledger = repository.ledger.value
                    LedgerCsv.plan(text, ledger?.transactions.orEmpty(), ledger?.customCategories.orEmpty()) to
                        displayName(uri)
                }
            }
            minimumShow.await()
            _transfer.value = result.fold(
                onSuccess = { (plan, name) ->
                    if (plan.transactions.isEmpty() && plan.duplicates == 0) {
                        TransferState.Failed("No transactions found in that file.")
                    } else {
                        TransferState.Preview(plan, name)
                    }
                },
                onFailure = { e ->
                    TransferState.Failed(
                        (e as? ImportException)?.message ?: "That doesn't look like a CSV Nickel can read."
                    )
                },
            )
        }
    }

    fun confirmImport() {
        val preview = _transfer.value as? TransferState.Preview ?: return
        _transfer.value = TransferState.Importing
        viewModelScope.launch {
            val minimumShow = async { delay(500) }
            repository.import(preview.plan)
            minimumShow.await()
            _transfer.value = TransferState.Idle
            val n = preview.plan.transactions.size
            _messages.send("Imported $n transaction${if (n == 1) "" else "s"}")
        }
    }

    fun dismissTransfer() {
        _transfer.value = TransferState.Idle
    }

    private val _shareExports = Channel<File>(Channel.BUFFERED)
    /** Export files ready to hand to the share sheet. */
    val shareExports: Flow<File> = _shareExports.receiveAsFlow()

    /** Writes every transaction to a CSV in the cache, then offers it via [shareExports]. */
    fun export() {
        viewModelScope.launch {
            val transactions = repository.ledger.value?.transactions.orEmpty()
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    ExportShare.fileFor(getApplication(), "nickel-${LocalDate.now()}.csv").apply {
                        writeText(LedgerCsv.export(transactions), Charsets.UTF_8)
                    }
                }.getOrNull()
            }
            if (file != null) _shareExports.send(file) else _messages.send("Couldn't prepare the export")
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
}
