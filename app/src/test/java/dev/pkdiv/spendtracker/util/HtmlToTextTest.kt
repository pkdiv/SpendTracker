package dev.pkdiv.spendtracker.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HtmlToTextTest {

    @Test
    fun stripsTagsAndPreservesLineBreaks() {
        val text = HtmlToText.convert("<html><body><p>Hello <b>World</b></p><p>Second line</p></body></html>")
        assertFalse(text.contains("<"))
        assertTrue(text.contains("Hello World"))
        assertTrue(text.contains("Second line"))
    }

    @Test
    fun stripsScriptAndStyle() {
        val text = HtmlToText.convert("<script>alert('x')</script><style>p{color:red}</style>Hello")
        assertFalse(text.contains("alert"))
        assertFalse(text.contains("color:red"))
        assertTrue(text.contains("Hello"))
    }

    @Test
    fun preservesTableCellsAsReadableText() {
        val text = HtmlToText.convert("<table><tr><td>A</td><td>B</td></tr></table>")
        assertTrue(text.contains("A"))
        assertTrue(text.contains("B"))
        assertTrue(text.contains("\t"))
    }
}
