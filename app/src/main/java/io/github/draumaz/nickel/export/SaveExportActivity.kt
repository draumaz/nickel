package io.github.draumaz.nickel.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The share sheet's "Save to files" option: an invisible activity that asks where to save
 * the waiting export, copies it there, and closes.
 */
class SaveExportActivity : ComponentActivity() {

    private val file: File? by lazy {
        intent.getStringExtra(EXTRA_PATH)?.let(::File)?.takeIf { it.exists() }
    }

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) finish() else save(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = file
        if (source == null) {
            finish()
            return
        }
        // Only launch the picker once; after a rotation the result arrives on its own.
        if (savedInstanceState == null) createDocument.launch(source.name)
    }

    private fun save(uri: Uri) {
        val source = file ?: return finish()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        source.inputStream().use { it.copyTo(out) }
                    } ?: error("no stream")
                }.isSuccess
            }
            Toast.makeText(
                this@SaveExportActivity,
                if (ok) "Saved ${source.name}" else "Couldn't save that file",
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    companion object {
        private const val EXTRA_PATH = "path"

        fun intent(context: Context, file: File): Intent =
            Intent(context, SaveExportActivity::class.java).putExtra(EXTRA_PATH, file.absolutePath)
    }
}
