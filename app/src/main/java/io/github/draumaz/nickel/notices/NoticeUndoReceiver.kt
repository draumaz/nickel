package io.github.draumaz.nickel.notices

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import io.github.draumaz.nickel.data.TransactionRepository
import io.github.draumaz.nickel.widget.NickelWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles the Delete button on the "transaction added" notification. A receiver rather
 * than something in the listener service because the action can land long after the
 * service's work is done, and [goAsync] keeps the process alive just long enough for the
 * ledger write to finish.
 */
class NoticeUndoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, 0L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                TransactionRepository(context).delete(transactionId)
                runCatching { NickelWidget().updateAll(context) }
            } finally {
                NotificationManagerCompat.from(context).cancel(notificationId)
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_UNDO = "io.github.draumaz.nickel.action.UNDO_NOTICE"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
