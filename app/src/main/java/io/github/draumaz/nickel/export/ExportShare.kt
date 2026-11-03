package io.github.draumaz.nickel.export

import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.chooser.ChooserAction
import androidx.core.content.FileProvider
import io.github.draumaz.nickel.R
import java.io.File

/**
 * Sharing an export: the CSV is written to the cache, then offered through the system
 * share sheet with a "Save to files" option alongside the usual apps.
 */
object ExportShare {

    /** Matches the manifest's `${applicationId}.exports`, so debug and release can coexist. */
    private fun authority(context: Context) = "${context.packageName}.exports"

    /** Where exports wait to be shared. Each export replaces the last. */
    fun fileFor(context: Context, name: String): File {
        val dir = File(context.cacheDir, "exports")
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        return File(dir, name)
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.name)
            // ClipData lets the share sheet preview the file and passes on the read grant.
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val save = SaveExportActivity.intent(context, file)
        val chooser = Intent.createChooser(send, "Export ${file.name}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ shows custom actions as buttons along the top of the share sheet.
            val pending = PendingIntent.getActivity(
                context, 0, save, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val action = ChooserAction.Builder(
                Icon.createWithResource(context, R.drawable.ic_save_file),
                "Save to files",
                pending,
            ).build()
            chooser.putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, arrayOf(action))
        } else {
            // Older share sheets list extra targets among the apps instead.
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(save))
        }
        context.startActivity(chooser)
    }
}
