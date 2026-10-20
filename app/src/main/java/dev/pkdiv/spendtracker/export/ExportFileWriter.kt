package dev.pkdiv.spendtracker.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** A written export, ready to hand to the share sheet. */
data class ExportedFile(val uri: Uri, val fileName: String)

/**
 * Writes an export and exposes it as a uri the on-device share sheet can read. The app
 * itself performs no upload — the user picks the destination.
 */
fun interface ExportFileWriter {
    fun write(fileName: String, contents: String): ExportedFile
}

/**
 * Writes into the app's **cache** directory (never backed up, never the DB) and shares
 * it through a [FileProvider].
 */
@Singleton
class CacheExportFileWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExportFileWriter {

    override fun write(fileName: String, contents: String): ExportedFile {
        val directory = File(context.cacheDir, DIR).apply { mkdirs() }
        val file = File(directory, fileName).apply { writeText(contents) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return ExportedFile(uri, fileName)
    }

    companion object {
        const val DIR = "exports"
    }
}
