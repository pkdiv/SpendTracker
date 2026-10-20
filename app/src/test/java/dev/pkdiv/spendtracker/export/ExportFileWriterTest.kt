package dev.pkdiv.spendtracker.export

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the real cache + [androidx.core.content.FileProvider] writer. Kept to a single
 * test method: FileProvider caches its path strategy statically, while Robolectric gives
 * each test method its own cache directory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ExportFileWriterTest {

    @Test
    fun writesIntoTheCacheAndExposesAContentUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val writer = CacheExportFileWriter(context)

        val exported = writer.write("spendtracker-all.csv", "date,amount\n2026-10-14,450.00\n")

        assertEquals("spendtracker-all.csv", exported.fileName)
        assertTrue(exported.uri.toString().startsWith("content://"))
        assertEquals("${context.packageName}.fileprovider", exported.uri.authority)

        val written = File(File(context.cacheDir, CacheExportFileWriter.DIR), exported.fileName)
        assertEquals("date,amount\n2026-10-14,450.00\n", written.readText())
    }
}
