# AGENTS.md — Spend Tracker (Android)

This file gives coding agents (Claude Code, Copilot, Cursor, etc.) the context needed to work on this repo. Keep it updated as decisions change. `README.md` is the user-facing summary; this file is the source of truth for constraints and conventions.

## 1. What this app does

A native Android app that:
1. Reads incoming **SMS** (and, eventually, **email**) messages on-device.
2. Detects transaction/spend messages (bank debit alerts, UPI notifications, credit card charges, etc.).
3. Parses amount, merchant, account/card, and category from each message.
4. Stores parsed transactions locally.
5. Generates:
   - **EOD report** — a daily summary (pushed as a notification + viewable in-app) around a configurable time (default 11:00 PM local).
   - **EOM report** — a month-end summary with category breakdown, trend vs. previous month, and top merchants.

## 2. Current status

- **Implemented:** SMS → parse → store → report flow, covered by unit tests. Supported rule sets: HDFC, ICICI, SBI, generic UPI.
- **Scaffolded, not functional:** email ingestion. `GmailClient` returns an empty list; there is no OAuth flow and no email sync worker. IMAP fallback is not started.
- **Not wired:** categorization. The merchant→category mapping table exists but is not connected to the parser; all transactions currently default to `OTHER`.
- **Unrecognized queue:** messages are surfaced in-app. The "teach a new rule" flow (regex builder / manual tagging) is not implemented.

## 3. Core principles (non-negotiable)

- **Local-first / privacy-first.** Message content and parsed data never leave the device by default. No backend service, no third-party analytics SDKs, no ad libraries.
- **On-device parsing only.** No message content is sent to any LLM API or cloud service for parsing. Regex/rule-based parsing first; an optional on-device model is a stretch goal, not a dependency for v1.
- **Minimal permissions.** Current manifest permissions, and nothing broader:
  - `READ_SMS` / `RECEIVE_SMS` — SMS ingestion
  - `POST_NOTIFICATIONS` — EOD report notification (runtime-requested on Android 13+)
  - `INTERNET` — reserved for email fetch only; no message content may be sent over it
  - Email access (future): Gmail API OAuth scoped to `gmail.readonly`, or on-device read-only IMAP
- **Explainable parsing.** Every parsed transaction must be traceable back to the source message (store a reference, not just the extracted fields) so misparses are correctable by the user.
- **Offline-capable.** The app must fully function (view history, EOD/EOM reports) with no network connection. Email fetch is the only feature that needs connectivity.

## 4. Tech stack

- **Language:** Kotlin (idiomatic, coroutines + Flow — no callback-hell, no RxJava unless justified)
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM, unidirectional data flow (UI → ViewModel → Repository → Data source)
- **Local storage:** Room (SQLite) for transactions; Preferences DataStore for settings
- **Background work:** WorkManager for scheduled EOD/EOM report generation and (future) periodic email sync
- **SMS ingestion:** `BroadcastReceiver` on `SMS_RECEIVED_ACTION` + one-time backfill read via `ContentResolver` on `content://sms`
- **Email ingestion (planned):** Gmail API (OAuth2, `gmail.readonly`) as the default; generic read-only IMAP client as a possible fallback for non-Gmail accounts
- **DI:** Hilt
- **Testing:** JUnit 5 + Turbine (Flow testing) + Robolectric for parsing logic (parsing rules must be unit-testable without a device/emulator)
- **Min SDK:** 26 (Android 8.0)

## 5. Build environment

- **JDK 21 required.** AGP 8.13 does not support JDK 24/25; newer Homebrew default JDKs will fail the build.
- **Android SDK 35.**
- Build + test: `./gradlew test assembleDebug`
- If the system default `java` is newer than 21: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew test assembleDebug`
- Agents: do not "fix" a JDK-related build failure by bumping AGP or changing the toolchain; point `JAVA_HOME` at JDK 21.

## 6. Project structure

```
app/src/main/java/dev/pkdiv/spendtracker/
  ingestion/
    sms/            # BroadcastReceiver, backfill reader, processor
    email/          # Email client interface; Gmail implementation is a stub
  parsing/
    rules/          # Per-bank/PSP regex rules (one file per sender pattern)
    ParserEngine.kt # Routes messages through the rule registry
  data/
    db/             # Room entities, DAOs, converters
    SettingsStore.kt
  repository/       # TransactionRepository
  reports/          # EOD/EOM aggregation models and logic
  work/             # WorkManager workers and scheduler
  ui/               # Compose screens, one package per feature
  di/               # Hilt modules
app/src/test/       # Unit tests — parsing rules and report aggregation
```

## 7. Parsing strategy

- Maintain a registry of **sender/pattern rules** (`ParsingRule` implementations held by `ParserEngine`) rather than one generic regex — bank message formats vary too much for a single pattern. A rule decides whether it can handle a message from sender + body.
- Each rule outputs: `amount`, `direction (debit/credit)`, `merchant/counterparty`, `account/card last-4`, `timestamp`, `rawMessageRef`.
- Results are a sealed `ParseResult`: `Parsed` or `Unrecognized`.
- Unmatched messages are **not silently dropped** — they land in the in-app "Unrecognized" queue. Planned: let the user teach the app a new rule (regex builder or manual tagging).
- Categorization is a user-editable merchant → category mapping table, not a black-box classifier. **Wiring this into the parser is the next categorization task** (see §2).

## 8. Reports

**EOD (End of Day)**
- Total spend, count of transactions, top 3 by amount, category split.
- Delivered as a local notification at a user-configurable time; tapping opens the in-app detail view.
- Generated by a `WorkManager` periodic job — must be resilient to missed runs (Doze mode, reboots) by reconciling against the last-generated-date stored in DataStore.

**EOM (End of Month)**
- Total spend, category breakdown (chart), comparison vs. previous month(s), top merchants, unusual/large transactions flagged.
- Runs on the 1st of the month for the prior month; also viewable on-demand for any past month from the Reports screen.

> Verify against code: README only mentions total + count for EOD and does not mention large-transaction flagging for EOM. Confirm which of the above are implemented and update §2 accordingly.

## 9. Releases

- Pushing a `v*` tag (e.g. `v0.1.0`) or a manual `workflow_dispatch` runs the GitHub Actions workflow: build, test, attach a release APK to a GitHub Release.
- The release APK is signed in CI from a keystore stored as repository secrets.
- **Never commit the keystore or signing credentials.** Agents must not add signing config with inline secrets.

## 10. Non-goals (v1)

- No cloud sync / multi-device support.
- No bill-splitting, budgeting alerts, or investment tracking — spend visibility only.
- No LLM-based parsing in v1 (deterministic rules must reach acceptable accuracy first; revisit only if rule-based parsing plateaus).
- No iOS version.

## 11. Open decisions for the agent to flag, not assume

- Whether the IMAP fallback ships in v1 or Gmail-only is acceptable for the first release.
- Email sync design: OAuth flow, sync worker cadence, and how fetched messages enter the same parse pipeline.
- UX and storage for user-taught parsing rules (regex builder vs. manual tagging).
- Whether "UPI notifications" ever means reading app notifications (`NotificationListenerService`). That is a new permission class and conflicts with the minimal-permissions principle; do not add it without an explicit decision and an update to §3.
- Expansion of the supported bank/PSP list beyond HDFC, ICICI, SBI, and generic UPI.

## 12. Coding conventions

- Prefer sealed classes over enums where states carry data (e.g. `ParseResult`).
- All parsing rules must have a corresponding unit test with real (anonymized) sample message text.
- No hardcoded strings in UI — use string resources from day one (multi-currency/locale may matter later, given INR-first usage).
- Every Room migration must ship with a test that verifies old-schema data survives.

## 13. Definition of done for a PR

- [ ] Builds and passes `./gradlew test`
- [ ] New parsing rules have unit tests with sample messages
- [ ] No new permission added without updating §3 "Minimal permissions" and the in-app permission rationale screen
- [ ] No network call added for message content processing
- [ ] `README.md` and §2 "Current status" updated if the change alters what is implemented