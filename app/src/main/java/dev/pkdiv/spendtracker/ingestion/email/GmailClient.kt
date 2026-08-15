package dev.pkdiv.spendtracker.ingestion.email

/**
 * Placeholder for the Gmail API client. Message content processing never uses
 * this in v1; it only fetches read-only transaction email bodies on-device.
 */
class GmailClient : EmailClient {
    override suspend fun fetchRecent(): List<EmailMessage> = emptyList()
}
