package dev.pkdiv.spendtracker.privacy

import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Guards against raw message content ever being passed to a logging call. */
class NoRawBodyLoggingTest {

    private val srcRoot = when {
        File("src/main/java").isDirectory -> File("src/main/java")
        File("app/src/main/java").isDirectory -> File("app/src/main/java")
        else -> error("Cannot locate main source directory")
    }

    private val forbidden = listOf(
        Regex("""Log\.[a-z]\([^\n]*rawBody""", RegexOption.IGNORE_CASE),
        Regex("""Log\.[a-z]\([^\n]*rawSender""", RegexOption.IGNORE_CASE),
        Regex("""Timber\.[a-z]\([^\n]*rawBody""", RegexOption.IGNORE_CASE),
        Regex("""println\([^\n]*rawBody"""),
        Regex("""println\([^\n]*rawSender"""),
    )

    @Test
    fun rawMessageContentIsNeverLogged() {
        val violations = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val text = file.readText()
                forbidden.mapNotNull { pattern ->
                    if (pattern.containsMatchIn(text)) {
                        "${file.relativeTo(srcRoot)} -> ${pattern.pattern}"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Raw message content must never be logged:\n" + violations.joinToString("\n"),
        )
    }
}
