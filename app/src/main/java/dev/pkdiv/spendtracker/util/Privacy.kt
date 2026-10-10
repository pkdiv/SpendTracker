package dev.pkdiv.spendtracker.util

/**
 * Pure helpers for keeping raw message content safe for display. Raw bodies are never
 * logged; masking here is display-only (the stored body is untouched).
 */
object Privacy {

    const val DEFAULT_BODY_LIMIT = 64 * 1024

    data class CapResult(val text: String, val truncated: Boolean)

    /** OTP-like codes: "OTP 123456", "OTP is 123456", "code: 1234", "verification code 12345678", etc. */
    private val OTP = Regex("""(?i)\b(otp|code|verification(?:\s*code)?)\b[^\d\n]{0,15}?(\d{4,8})""")

    fun maskSensitive(body: String): String = OTP.replace(body) { match ->
        val digits = match.groups[2]?.value ?: return@replace match.value
        match.value.replace(digits, "••••")
    }

    fun capBody(text: String, limit: Int = DEFAULT_BODY_LIMIT): CapResult =
        if (text.length <= limit) CapResult(text, false)
        else CapResult(text.take(limit), true)
}
