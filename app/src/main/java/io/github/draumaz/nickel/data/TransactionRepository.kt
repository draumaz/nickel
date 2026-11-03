package io.github.draumaz.nickel.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Ledger(
    val transactions: List<Transaction>,
    val customCategories: List<Category>,
)

/**
 * Stores the ledger as two small JSON files in app storage. A personal ledger stays
 * tiny, so reading and rewriting whole files is simpler than a database.
 */
class TransactionRepository(context: Context) {

    private val transactionsFile = File(context.filesDir, "transactions.json")
    private val categoriesFile = File(context.filesDir, "categories.json")
    private val writeLock = Mutex()

    private val _ledger = MutableStateFlow<Ledger?>(null)

    /** `null` until the first load from disk finishes. */
    val ledger: StateFlow<Ledger?> = _ledger.asStateFlow()

    suspend fun load() {
        _ledger.value = withContext(Dispatchers.IO) { read() }
    }

    suspend fun upsert(transaction: Transaction) = mutate { ledger ->
        ledger.copy(
            transactions = ledger.transactions.filterNot { it.id == transaction.id } + transaction,
        )
    }

    suspend fun delete(id: Long) = mutate { ledger ->
        ledger.copy(transactions = ledger.transactions.filterNot { it.id == id })
    }

    /** Adds a user-made category; a category with the same id already saved is left alone. */
    suspend fun addCategory(category: Category) = mutate { ledger ->
        if (ledger.customCategories.any { it.id == category.id }) ledger
        else ledger.copy(customCategories = ledger.customCategories + category)
    }

    suspend fun import(plan: ImportPlan) = mutate { ledger ->
        val existingIds = ledger.customCategories.mapTo(HashSet()) { it.id }
        ledger.copy(
            transactions = ledger.transactions + plan.transactions,
            customCategories = ledger.customCategories + plan.newCategories.filterNot { it.id in existingIds },
        )
    }

    private suspend fun mutate(block: (Ledger) -> Ledger) {
        writeLock.withLock {
            val current = _ledger.value ?: Ledger(emptyList(), emptyList())
            val next = block(current).let { l ->
                l.copy(transactions = l.transactions.sortedByDescending { it.timestamp })
            }
            _ledger.value = next
            withContext(Dispatchers.IO) { write(next) }
        }
    }

    private fun read(): Ledger {
        val custom = runCatching {
            if (!categoriesFile.exists()) return@runCatching emptyList()
            val array = JSONArray(categoriesFile.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Category(
                    id = o.getString("id"),
                    label = o.getString("label"),
                    kind = Kind.valueOf(o.getString("kind")),
                    iconKey = o.optString("icon", "category"),
                    shapeKey = o.optString("shape", "cookie9"),
                    tone = runCatching { Tone.valueOf(o.getString("tone")) }.getOrDefault(Tone.PRIMARY),
                )
            }
        }.getOrDefault(emptyList())

        val byId = (Categories.builtIns + custom).associateBy { it.id }
        val transactions = runCatching {
            if (!transactionsFile.exists()) return@runCatching emptyList()
            val array = JSONArray(transactionsFile.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val kind = Kind.valueOf(o.getString("kind"))
                Transaction(
                    id = o.getLong("id"),
                    amountCents = o.getLong("amountCents"),
                    kind = kind,
                    category = byId[o.getString("category")] ?: Categories.fallback(kind),
                    note = o.optString("note"),
                    timestamp = o.getLong("timestamp"),
                )
            }
        }.getOrDefault(emptyList())

        return Ledger(transactions.sortedByDescending { it.timestamp }, custom)
    }

    private fun write(ledger: Ledger) {
        val transactions = JSONArray()
        ledger.transactions.forEach { t ->
            transactions.put(
                JSONObject()
                    .put("id", t.id)
                    .put("amountCents", t.amountCents)
                    .put("kind", t.kind.name)
                    .put("category", t.category.id)
                    .put("note", t.note)
                    .put("timestamp", t.timestamp)
            )
        }
        val categories = JSONArray()
        ledger.customCategories.forEach { c ->
            categories.put(
                JSONObject()
                    .put("id", c.id)
                    .put("label", c.label)
                    .put("kind", c.kind.name)
                    .put("icon", c.iconKey)
                    .put("shape", c.shapeKey)
                    .put("tone", c.tone.name)
            )
        }
        // Categories first, so a crash between the two writes never leaves transactions
        // pointing at categories that weren't saved.
        writeAtomically(categoriesFile, categories.toString())
        writeAtomically(transactionsFile, transactions.toString())
    }

    // Write to a temp file then rename, so a crash mid-write can't corrupt the ledger.
    private fun writeAtomically(file: File, text: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        tmp.renameTo(file)
    }
}
