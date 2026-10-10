package dev.pkdiv.spendtracker.ingestion.email

data class EmailMessage(
    val sender: String,
    val subject: String,
    val body: String,
    val rawMessageRef: String,
    val messageId: String?,
    val receivedAtMillis: Long,
)

interface EmailClient {
    suspend fun fetchRecent(): List<EmailMessage>
}
