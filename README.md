# Spend Tracker

A privacy-first Android app that reads transaction messages on-device and turns them into a clear picture of your daily and monthly spending.

Spend Tracker watches incoming SMS and UPI notifications, detects which ones are about money, extracts the amount, merchant, account, and direction, and stores the result locally. At the end of the day it posts a spend summary notification; at the end of the month it shows a breakdown by category, top merchants, and how the month compares to the previous one.

Everything runs on-device. Message content is never sent to a backend, an analytics SDK, or a cloud parsing service.

## Documentation

- **User guide** — see [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md).
- **Developer guide** — see [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md).
- **Project constraints & conventions** — see [`AGENTS.md`](AGENTS.md).

## Features

- **On-device SMS/UPI parsing** — rule-based, not an LLM, so results are deterministic and explainable
- **Bank-specific rules** — HDFC, ICICI, SBI, and generic UPI are supported today
- **Local-first storage** — Room for transactions, DataStore for settings
- **Unrecognized queue** — messages that don't match a rule are surfaced in-app instead of silently dropped
- **Editable merchant → category mapping** — teach the app which category a merchant belongs to from Settings; your overrides win over the built-in defaults
- **End-of-day report** — local notification with total spend and transaction count at a configurable time
- **End-of-month report** — total, category breakdown, top merchants, and previous-month comparison
- **Home-screen widget** — month-to-date spend at a glance, updated as transactions arrive
- **Manual entry** — add cash/offline spending by hand from the Activity screen
- **Notes & rename** — add a free-text note to a transaction and rename its merchant; a rename also teaches the merchant → category mapping
- **Export** — save or share a CSV of your transactions (opt-in, on-device share sheet)
- **Minimal permissions** — only SMS read/receive, notifications, and internet (reserved for future email fetch)

## Tech stack

| Concern | Choice |
| --- | --- |
| Language | Kotlin (coroutines + Flow) |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, unidirectional data flow |
| DI | Hilt |
| Local storage | Room (SQLite) + Preferences DataStore |
| Background work | WorkManager |
| Testing | JUnit 5, Turbine, Robolectric |
| Min SDK | 26 (Android 8.0) |

## Requirements

- JDK 21 — AGP 8.13 does not support JDK 24/25, so newer Homebrew default JDKs will fail
- Android SDK 35

## Build

```bash
./gradlew test assembleDebug
```

If your system default `java` is newer than 21, point Gradle at JDK 21 explicitly:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew test assembleDebug
```

## Project structure

```
app/src/main/java/dev/pkdiv/spendtracker/
  ingestion/
    sms/            # BroadcastReceiver, backfill reader, processor
    email/          # Email client interface (Gmail implementation is a stub)
  parsing/
    rules/          # Per-bank/PSP regex rules
    ParserEngine.kt # Routes messages through the rule registry
  data/
    db/             # Room entities, DAOs, converters
    SettingsStore.kt
  repository/       # TransactionRepository
  reports/          # EOD/EOM aggregation models and logic
  work/             # WorkManager workers and scheduler
  ui/               # Compose screens, one package per feature
  di/               # Hilt modules
app/src/test/       # Unit tests for parsing rules and report aggregation
```

## How parsing works

`ParserEngine` holds a list of `ParsingRule` implementations. Each rule decides whether it can handle a message by inspecting the sender and body, then extracts:

- amount
- direction (debit/credit)
- merchant/counterparty
- account/card last-4
- timestamp
- `rawMessageRef` (back-reference to the source message)

Matched messages become `ParseResult.Parsed`. Unmatched messages become `ParseResult.Unrecognized` and land in the in-app Unrecognized queue.

## Current status

The core SMS → parse → store → report flow is implemented and covered by unit tests. Email ingestion is scaffolded but not functional yet: `GmailClient` currently returns an empty list, and there is no OAuth flow or email sync worker. Categorization is wired in: a transaction is categorized from the user-editable `merchant_categories` table when an exact merchant match exists, otherwise by a built-in keyword-based default categorizer, and finally falls back to `OTHER`. You can teach the app a merchant's category from **Settings › Merchant categories** — entries are stored with a normalized key so matching is reliable. Rows stored before categorization existed are re-categorized automatically by a one-shot backfill on app start (only `OTHER` rows are touched, so manual corrections are never overwritten). A home-screen widget shows this month's spend, refreshed when transactions arrive and on a system update cycle. You can also add a transaction by hand from the Activity screen — manual entries are kept separate (source "Manual"), keep the category you pick, and are excluded from SMS duplicate detection. On a transaction's detail screen you can add a free-text note and rename the merchant; a rename is also taught to the merchant → category mapping, so future transactions from that merchant name categorize automatically. **Settings › Export** can write a CSV of this month, last month, or all time and hand it to the on-device share sheet; nothing is uploaded, and it only ever runs when you tap it.

## Releases

Pushing a `v*` tag (for example `v0.1.0`) — or a manual `workflow_dispatch` run — triggers the GitHub Actions workflow to build, run tests, and attach a release APK to a GitHub Release. The release APK is signed in CI from a keystore stored as repository secrets.

## Contributing

Parsing rules are the easiest place to add value. Each rule lives in `parsing/rules/` and must ship with a unit test using real, anonymized sample message text. See `AGENTS.md` for the full conventions and definition of done.
