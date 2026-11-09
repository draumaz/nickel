package io.github.draumaz.nickel.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.draumaz.nickel.data.SettingsRepository

/**
 * The periodic backup. A failure is recorded for settings to show rather than retried:
 * it almost always means the folder is gone or access was revoked, which retrying can't fix,
 * and the next period tries again anyway.
 */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        LedgerBackup.run(applicationContext, SettingsRepository(applicationContext))
        return Result.success()
    }
}
