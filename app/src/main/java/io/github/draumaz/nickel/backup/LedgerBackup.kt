package io.github.draumaz.nickel.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.draumaz.nickel.data.BackupFiles
import io.github.draumaz.nickel.data.LedgerCsv
import io.github.draumaz.nickel.data.SettingsRepository
import io.github.draumaz.nickel.data.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Writes the ledger as a dated CSV into the folder the user picked through the system
 * folder picker. Access goes through a persisted Storage Access Framework grant, so no
 * storage permission is needed and the folder can live on any provider that offers trees.
 */
object LedgerBackup {

    enum class Outcome { SAVED, EMPTY, NO_FOLDER, FAILED }

    private const val WORK_NAME = "ledger-backup"

    /** Keeps the grant across reboots; without it the URI stops working once the app dies. */
    fun persistAccess(context: Context, tree: Uri, previous: String?) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val resolver = context.contentResolver
        resolver.takePersistableUriPermission(tree, flags)
        // Grants are capped per app, so give back the old folder's when switching.
        previous?.let(Uri::parse)?.takeIf { it != tree }?.let { old ->
            runCatching { resolver.releasePersistableUriPermission(old, flags) }
        }
    }

    fun folderName(context: Context, tree: Uri): String? = runCatching {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    /**
     * (Re)schedules the periodic backup to match settings, or cancels it. The first periodic
     * run is held back one interval: picking a folder already backs up on the spot, and an
     * immediate second run would just rewrite the same file.
     */
    fun schedule(context: Context, settings: SettingsRepository) {
        val backup = settings.backup.value
        val work = WorkManager.getInstance(context)
        if (backup.folderUri == null || backup.intervalHours <= 0) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val hours = backup.intervalHours.toLong()
        val request = PeriodicWorkRequestBuilder<BackupWorker>(hours, TimeUnit.HOURS)
            .setInitialDelay(hours, TimeUnit.HOURS)
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Backs up now and records the result, which settings shows as the last backup. */
    suspend fun run(context: Context, settings: SettingsRepository): Outcome {
        val folder = settings.backup.value.folderUri ?: return Outcome.NO_FOLDER
        val repository = TransactionRepository.get(context)
        if (repository.ledger.value == null) repository.load()
        val transactions = repository.ledger.value?.transactions.orEmpty()
        // An empty ledger is never written: a corrupted read falls back to empty, and a week
        // of empty backups would prune away every good one.
        if (transactions.isEmpty()) return Outcome.EMPTY
        val ok = withContext(Dispatchers.IO) {
            runCatching { write(context, Uri.parse(folder), LedgerCsv.export(transactions)) }.isSuccess
        }
        settings.recordBackup(ok)
        return if (ok) Outcome.SAVED else Outcome.FAILED
    }

    private fun write(context: Context, tree: Uri, csv: String) {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)
        val existing = children(context, tree, treeId)
        val name = BackupFiles.nameFor(LocalDate.now())
        // Reuse today's file: creating it again would make the provider add " (1)".
        val target = existing[name]?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it) }
            ?: DocumentsContract.createDocument(resolver, parent, "text/csv", name)
            ?: error("could not create $name")
        resolver.openOutputStream(target, "wt")?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            ?: error("could not open $name")
        // Prune only after the new file is safely written.
        val pruned = BackupFiles.toPrune(existing.keys + name)
        pruned.forEach { old ->
            existing[old]?.let { id ->
                runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
            }
        }
    }

    /** Display name to document id for the folder's direct children. */
    private fun children(context: Context, tree: Uri, treeId: String): Map<String, String> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val out = HashMap<String, String>()
        context.contentResolver.query(uri, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                out[name] = id
            }
        } ?: error("folder unavailable")
        return out
    }
}
