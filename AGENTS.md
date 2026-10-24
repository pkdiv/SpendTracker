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

- **Implemented:** SMS → parse → store → report flow, covered by unit tests. Supported rule sets: HDFC, ICICI, SBI, generic UPI (each has a unit test with anonymized samples). Near-duplicate SMS (same sender + amount + direction + merchant within 2 minutes) are auto-flagged as duplicates; transactions can also be manually marked/unmarked as duplicate, and duplicates are excluded from totals/reports.
- **Scaffolded, not functional:** email ingestion. `GmailClient` returns an empty list; there is no OAuth flow and no email sync worker. IMAP fallback is not started. v1 is **SMS-only**; email is demoted to post-v1 (P3 in `docs/ROADMAP.md`).
- **Implemented:** categorization. On insert, a transaction is categorized from the user-editable `merchant_categories` table (exact merchant match) when present, otherwise by an ordered keyword-based default categorizer (`TransactionCategorizer`), and finally falls back to `OTHER`. Rows stored before categorization existed are re-categorized by a one-shot backfill (`BackfillCategoriesWorker`) on app start; it only touches `OTHER` rows, so manual corrections are preserved. The mapping table is editable in-app via **Settings → Merchant categories** (`MerchantCategoryScreen`, add/edit/delete); writes normalize the merchant key (trim + uppercase) and the `categoryFor` lookup is case-insensitive, so the mapping affects both future inserts and the backfill.
- **Implemented:** Activity (history) search & filters. Free-text merchant/amount search plus category, direction (money in/out), date range, amount range, and hide-duplicates filters, applied in-memory in `HistoryViewModel` through a pure `filterTransactions` helper (no DAO/schema change).
- **Implemented:** home-screen widget. A classic `RemoteViews` widget (no Glance dependency) shows month-to-date spend, rendered by `SpendWidgetUpdater` from the same `TransactionRepository.observeBetween` Flow the Home screen uses (debits only, minus self transfers and duplicates, via the pure `WidgetSpend.from`). A one-shot `WidgetUpdateWorker` (`KEEP`) is enqueued from the widget provider's `onUpdate`, on app start, and after each parsed SMS, so it reflects live and backfilled transactions; a system ~30-minute update cycle handles month rollover. Register with long-press → Widgets → Spend Tracker.
- **Implemented:** manual transaction entry. An **Add** FAB on the Activity (history) screen opens `AddTransactionScreen` (`ui/addtransaction/`) to record cash/offline spending by hand (amount, direction, merchant, category, account, date); validation is the pure `ManualTransactionForm.errors()`. Saved via `TransactionRepository.insertManual` with a synthetic `manual:<uuid>` reference and a new `TransactionSource.MANUAL` (no schema migration); the chosen category is stored as-is rather than re-derived, and SMS duplicate detection is bypassed. The detail screen labels these "Manual" and omits the original-message/reference rows, and the home-screen widget is refreshed after a save.
- **Implemented:** export (opt-in, user-initiated). **Settings → Export** (`ExportScreen`, `ui/export/`) exports a CSV of This month / Last month / All time. The file is built by the pure `TransactionCsv` (parsed fields only — never raw message content) and written to the app **cache** by `ExportFileWriter`, then handed to the on-device share sheet through a `FileProvider` (`${applicationId}.fileprovider`). Nothing is uploaded, and it runs only on an explicit tap.
- **Implemented:** transaction notes & rename merchant. The detail screen's overflow opens `RenameMerchantDialog` / `TransactionNoteDialog` (`ui/detail/`), writing through `TransactionRepository.renameMerchant` / `setNote`. Notes are a new nullable `note` column (schema v9, `MIGRATION_8_9`, no backfill needed) and show in a `Note` card on the detail screen only — list rows are unchanged. A rename also upserts a `merchant_categories` override (new name → the transaction's category, normalized via the shared path) so future transactions from that merchant auto-categorize; blank names are ignored.
- **Unrecognized queue:** messages are surfaced in-app and can be dismissed individually or cleared all at once (with confirmation) from the Alerts tab. Dismissing is a **soft delete** (`dismissed` flag, schema v8): the row is kept so the inbox re-scan can't resurface it, and a content guard stops the live receiver and the backfill (which derive different `rawMessageRef`s) from storing the same SMS twice. The "teach a new rule" flow (regex builder / manual tagging) is not implemented. **Gap closed:** only transaction-shaped messages reach the queue — `SmsFilter` requires an amount plus a transaction signal and rejects OTP/verification messages, so unrelated personal SMS are never stored. Bodies are capped at 64 KB (`Privacy.capBody`) and OTP codes are masked in the Alerts list (`Privacy.maskSensitive`).
- **Implemented:** first-run prominent disclosure + consent. `MainActivity` renders `DisclosureScreen` (`ui/disclosure/`) before any runtime permission prompt; `READ_SMS`/`RECEIVE_SMS` are requested only from its **Allow SMS access** action, and **Continue without SMS** keeps the app fully usable. Consent is persisted in `SettingsStore` (`sms_disclosure_shown`), and the `POST_NOTIFICATIONS` rationale dialog is shown once before that prompt (`notification_rationale_shown`). Settings › Permissions rows open the system app settings so permission can be re-granted.
- **Implemented:** privacy policy + versioning. `docs/privacy-policy.md` is the in-repo policy (linked in-app from **Settings › About**), and `versionCode`/`versionName` are derived from the `vX.Y.Z` tag (`X*10000 + Y*100 + Z`), with release builds no longer falling back to the debug keystore.
- **Implemented:** incremental SMS scan. `SmsReader` persists the highest processed SMS `_ID` in `SettingsStore` (`last_sms_id`) and only queries messages above it; the full inbox is read once (first install / upgrade), then only new messages. `MainActivity` no longer re-scans on every resume — the initial import runs once and the manual Sync action uses the same incremental scan. `SmsReceiver` uses `goAsync()` so an insert cannot be lost if the process is killed mid-processing.
- **Implemented:** report reliability. Changing the EOD time or the notification toggle reschedules `EodReportWorker` (`SettingsViewModel` → `ReportScheduling.scheduleEod`). On app start, `ReportScheduler.catchUpMissedReports()` queues catch-up runs when the EOD time has passed without a report today, or the prior month has no EOM run; both workers guard against duplicate posts via `lastEodDate` / `lastEomMonth`. `EomReportWorker` no longer requires running on the 1st, so a delayed run still reports the intended prior month. Notifications use `Formatters.formatInr` and the monochrome `ic_notification` icon.
- **Partially implemented / known gaps** (tracked in `docs/PLAY_STORE_RELEASE.md` §4–5; fix and then update this list):
  - `SmsReceiver`'s `goAsync()` window is ~10 seconds; if parsing/insert ever grows beyond a single SMS, move it to WorkManager.
  - Reports: Doze/reboot/force-stop behavior is reconciled on app start but has not been exercised on a physical device.
  - Parsing coverage is limited to HDFC, ICICI, SBI and generic UPI; review against real samples before launch.

## 3. Core principles (non-negotiable)

- **Local-first / privacy-first.** Message content and parsed data never leave the device by default. No backend service, no third-party analytics SDKs, no ad libraries.
- **On-device parsing only.** No message content is sent to any LLM API or cloud service for parsing. Regex/rule-based parsing first; an optional on-device model is a stretch goal, not a dependency for v1.
- **Data minimization.** Ingest only transaction-shaped SMS. Unrelated personal messages must never be copied into the app database. If a non-matching message must be retained for the Alerts queue, store the minimum (sender, timestamp, hash/reference, length-capped body via `Privacy.capBody`), and mask sensitive tokens (`Privacy.maskSensitive`) wherever message text is displayed. The local database stays excluded from cloud backup and device transfer (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`), and raw message bodies are never logged (`NoRawBodyLoggingTest`).
- **Consent before sensitive permissions.** Show a prominent in-app disclosure (what is accessed, why, that it stays on-device) and require an affirmative action *before* the `READ_SMS`/`RECEIVE_SMS` runtime prompt (Google Play requirement). The same applies, contextually, to `POST_NOTIFICATIONS` on Android 13+. The app must remain usable (manual entry, history, reports) if SMS is denied; never hard-block behind a permission.
- **Minimal permissions.** The app declares only:
  - `READ_SMS` / `RECEIVE_SMS` — SMS ingestion
  - `POST_NOTIFICATIONS` — EOD report notification (runtime-requested on Android 13+)
  - WorkManager's manifest merger adds `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`,
    `FOREGROUND_SERVICE` and `ACCESS_NETWORK_STATE`; these are used for report
    scheduling only and never to transmit data.
  - Email access (future): Gmail API OAuth scoped to `gmail.readonly`, or on-device read-only IMAP. Add `INTERNET` only when that ships, and only for the fetch itself; no message content may ever be sent over it.
- **No network today.** `INTERNET` has been removed from the manifest. Do not re-add it until opt-in email ingestion ships.
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
- **Compile/target SDK:** 36 (Android 16). `minSdk = 26`. Google Play requires API 36 for new apps (see §9).

## 5. Build environment

- **JDK 21 required.** AGP 8.13 does not support JDK 24/25; newer Homebrew default JDKs will fail the build.
- **Android SDK 36 required.** `compileSdk = 36` and `targetSdk = 36`; install `platforms;android-36`. Keep `minSdk = 26`, and re-test Android 16 behavior changes (edge-to-edge, predictive back, notification/permission changes) when bumping.
- Build + test: `./gradlew test assembleDebug`
- Release-shaped verification: `./gradlew test bundleRelease --no-daemon` (JDK 21), plus `./gradlew lint` (or `lintRelease`).
- Versioning: the `vX.Y.Z` tag is the source of truth. `app/build.gradle.kts` derives `versionName = X.Y.Z` and `versionCode = X*10000 + Y*100 + Z` from the nearest git tag, or from `-PversionName=X.Y.Z` (which CI passes). The build fails if the version cannot be parsed; never hardcode or reuse a `versionCode`.
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
docs/               # ROADMAP.md, USER_GUIDE.md, DEVELOPMENT.md, PLAY_STORE_RELEASE.md, STORE_LISTING.md, privacy-policy.md
```

## 7. Parsing strategy

- Maintain a registry of **sender/pattern rules** (`ParsingRule` implementations held by `ParserEngine`) rather than one generic regex — bank message formats vary too much for a single pattern. A rule decides whether it can handle a message from sender + body.
- Each rule outputs: `amount`, `direction (debit/credit)`, `merchant/counterparty`, `account/card last-4`, `timestamp`, `rawMessageRef`.
- Results are a sealed `ParseResult`: `Parsed`, `Unrecognized`, or `Ignored` (filtered out before parsing and never stored).
- Unmatched **transaction-shaped** messages are **not silently dropped** — they land in the in-app "Unrecognized" queue. Messages that are not transaction-shaped (decided by a sender/content pre-filter in `SmsReader`/`SmsProcessor`) are never imported or stored (§3 data minimization). Planned: let the user teach the app a new rule (regex builder or manual tagging).
- The SMS scan is **incremental** (`SmsReader` + `SmsScanWindow`): track the highest processed SMS `_ID` in `SettingsStore` and resume from it; do not re-read the whole inbox on every launch/resume. Keep a manual Sync action; run the initial import once. `SmsReceiver` reads the live message and uses `goAsync()` to finish the insert.
- Categorization is a user-editable merchant → category mapping table, not a black-box classifier. It is applied at insert time (`resolveCategory`, see §2) and the table is edited in-app via **Settings → Merchant categories**.

## 8. Reports

**EOD (End of Day)**
- Total spend, count of transactions, top 3 by amount, category split.
- Delivered as a local notification at a user-configurable time; tapping opens the in-app detail view.
- Generated by a `WorkManager` periodic job. Missed runs (Doze, reboot, force-stop) are reconciled on app start by `ReportScheduler.catchUpMissedReports()` against `lastEodDate`; `EodReportWorker` skips a day it has already reported. Changing the EOD time or the notification toggle reschedules `EodReportWorker`.

**EOM (End of Month)**
- Total spend, category breakdown (chart), comparison vs. previous month(s), top merchants, unusual/large transactions flagged.
- Runs on the 1st of the month for the prior month; also viewable on-demand for any past month from the Reports screen. A delayed WorkManager run still reports the intended prior month: the `dayOfMonth != 1` early return is gone, and `lastEomMonth` prevents duplicate posts.

> Verify against code: README only mentions total + count for EOD and does not mention large-transaction flagging for EOM. Confirm which of the above are implemented and update §2 accordingly.

## 9. Releases

**GitHub release (sideload channel)**
- Pushing a `v*` tag (e.g. `v0.1.0`) or a manual `workflow_dispatch` (with a `version` input) runs the GitHub Actions workflow (`.github/workflows/build-and-publish.yml`): build, test, build the AAB + APK, and attach both to a GitHub Release.
- The release artifacts are signed in CI from a keystore stored as repository secrets (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Without them, release builds are unsigned — never debug-signed.
- **Never commit the keystore or signing credentials.** Agents must not add signing config with inline secrets.
- Release builds must **never fall back to the debug keystore**; the fallback has been removed in `app/build.gradle.kts`.

**Google Play**
- The Google Play submission steps and the full release gate live in [`docs/PLAY_STORE_RELEASE.md`](docs/PLAY_STORE_RELEASE.md). Follow it for a store release; a `v*` tag alone is not a Play release. Do not submit until every **P0** item there is green.
- **Artifact:** Play takes an Android App Bundle. CI must build `bundleRelease` and attach `app/build/outputs/bundle/release/*.aab` (the APK may remain for sideloading). Promote the tested artifact to production; never rebuild a different one.
- **Target API:** `compileSdk`/`targetSdk` must be 36 for new Play apps (deadline 31 Aug 2026, extension to 1 Nov 2026). Expect API 37 next cycle; keep `targetSdk` current annually.
- **Versioning:** tags (`vX.Y.Z`) are the source of truth. `versionCode = X*10000 + Y*100 + Z` and `versionName = X.Y.Z` are injected at build time (tag parse or `-PversionName`); the build fails if the version can't be parsed. `versionCode` must be unique and monotonically increasing per upload — never reuse or decrease.
- **Signing:** enroll in Play App Signing; the upload key is not the app signing key. Keep keystore/upload key backed up and access-controlled. `applicationId = dev.pkdiv.spendtracker` is immutable after first upload.
- **Policy gates (all P0 for an SMS app):**
  - Public privacy policy URL, linked in the listing and in-app (Settings › About); it must match actual behavior (on-device only, no off-device transmission, backup exclusion, user-initiated CSV export, retention/deletion, no third parties/analytics/ads).
  - Prominent disclosure + consent before the SMS prompt (§3).
  - Play Console **Permissions Declaration Form** (App content › Sensitive app permissions › SMS/Call Log), use case **SMS-based money management**, with a demo video (disclosure → SMS import → parsed transaction → report). Re-submit if SMS usage changes. Approval is required before the release goes live.
  - Accurate **Data safety**, content rating, target audience and ads declarations; account for SMS access and user-initiated CSV export.
  - Do not ship the listing/README/USER_GUIDE claiming "UPI notifications" (§11) or broader bank coverage than HDFC, ICICI, SBI and generic UPI.
- **Testing track requirement:** personal developer accounts created after 13 Nov 2023 need ≥12 testers opted in continuously for 14 days on physical devices before production access. Use staged rollout (10% → 50% → 100%) with halt criteria (crash/ANR rate, permission-denial reports, parse failures).
- Optional: enable R8 (`isMinifyEnabled = true`) with keep rules; release builds are currently unminified.

## 10. Non-goals (v1)

- No cloud sync / multi-device support.
- No bill-splitting, budgeting alerts, or investment tracking — spend visibility only.
- No LLM-based parsing in v1 (deterministic rules must reach acceptable accuracy first; revisit only if rule-based parsing plateaus).
- No iOS version.

## 11. Open decisions for the agent to flag, not assume

- **v1 scope: SMS-only vs. email.** v1 is **SMS-only** (confirmed for the Play release); email ingestion is post-v1 (P3 in `docs/ROADMAP.md`). If that changes, decide whether the IMAP fallback ships or Gmail-only is acceptable, and re-add `INTERNET` + update the privacy policy and Play declarations.
- Email sync design: OAuth flow, sync worker cadence, and how fetched messages enter the same parse pipeline.
- UX and storage for user-taught parsing rules (regex builder vs. manual tagging).
- Whether "UPI notifications" ever means reading app notifications (`NotificationListenerService`). That is a new permission class and conflicts with the minimal-permissions principle; do not add it without an explicit decision and an update to §3. Until then, README and `docs/USER_GUIDE.md` must not claim it.
- Expansion of the supported bank/PSP list beyond HDFC, ICICI, SBI, and generic UPI.
- Play category (Finance vs. Productivity), and whether to enable R8 for the first release.

## 12. Coding conventions

- Prefer sealed classes over enums where states carry data (e.g. `ParseResult`).
- All parsing rules must have a corresponding unit test with real (anonymized) sample message text (`SbiBankRule` is currently missing one).
- No hardcoded strings in UI — use string resources from day one (multi-currency/locale may matter later, given INR-first usage).
- Format money with the shared `Formatters.formatInr` everywhere, including notifications.
- Every Room migration must ship with a test that verifies old-schema data survives.
- Never log raw message content; show raw message text only through `Privacy.maskSensitive`.

## 13. Definition of done for a PR

- [ ] Builds and passes `./gradlew test` (and `bundleRelease` / lint for release-affecting changes)
- [ ] New parsing rules have unit tests with sample messages
- [ ] No new permission added without updating §3 "Minimal permissions", the in-app permission disclosure/rationale screen, the privacy policy, and the Play Data safety / Permissions declarations
- [ ] No network call added for message content processing
- [ ] No new storage, display, or logging of raw message bodies beyond §3 data minimization
- [ ] No change to SMS usage or data flows without updating the privacy policy and re-checking the Play Permissions Declaration
- [ ] No `versionCode` reuse/decrease; no signing secrets or debug-keystore fallback introduced
- [ ] `README.md`, `docs/USER_GUIDE.md` and §2 "Current status" updated if the change alters what is implemented (and the "Known gaps" list pruned when a gap is closed)