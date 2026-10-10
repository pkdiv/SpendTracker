package dev.pkdiv.spendtracker.util

/** Converts an HTML email body to readable plain text. Never produces markup. */
object HtmlToText {

    private val SCRIPT_STYLE = Regex("(?is)<(script|style)[^>]*>.*?</\\1>")
    private val TAG = Regex("(?s)<[^>]+>")
    private val BR = Regex("(?i)<br\\s*/?>")
    private val BLOCK_CLOSE = Regex("(?i)</(p|div|tr|li|h[1-6]|table|blockquote)>")
    private val CELL_CLOSE = Regex("(?i)</(td|th)>")

    fun convert(html: String): String {
        var text = SCRIPT_STYLE.replace(html, "")
        text = BR.replace(text, "\n")
        text = BLOCK_CLOSE.replace(text, "\n")
        text = CELL_CLOSE.replace(text, "\t")
        text = TAG.replace(text, "")
        text = decodeEntities(text)
        return text
            .replace("\r\n", "\n")
            .split('\n')
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
}
