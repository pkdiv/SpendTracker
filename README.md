# Spend Tracker

A privacy-first Android app that reads transaction messages on-device and turns them into a clear picture of your daily and monthly spending.

Spend Tracker watches incoming SMS and UPI notifications, detects which ones are about money, extracts the amount, merchant, account, and direction, and stores the result locally. At the end of the day it posts a spend summary notification; at the end of the month it shows a breakdown by category, top merchants, and how the month compares to the previous one.

Everything runs on-device. Message content is never sent to a backend, an analytics SDK, or a cloud parsing service.

## Features

- **On-device SMS/UPI parsing** — rule-based, not an LLM, so results are deterministic and explainable
- **Bank-specific rules** — HDFC, ICICI, SBI, and generic UPI are supported today
- **Local-first storage** — Room for transactions, DataStore for settings
- **Unrecognized queue** — messages that don't match a rule are surfaced in-app instead of silently dropped
- **End-of-day report** — local notification with total spend and transaction count at a configurable time
- **End-of-month report** — total, category breakdown, top merchants, and previous-month comparison
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

- JDK 21 — AGP 8.7 does not support JDK 24/25, so newer Homebrew default JDKs will fail
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

The core SMS → parse → store → report flow is implemented and covered by unit tests. Email ingestion is scaffolded but not functional yet: `GmailClient` currently returns an empty list, and there is no OAuth flow or email sync worker. Categorization still defaults to `OTHER`; the merchant→category mapping table exists but is not yet wired into the parser.

## Releases

Pushing a `v*` tag (for example `v0.1.0`) triggers the GitHub Actions workflow to build, run tests, and attach the release APK to a GitHub Release. The APK is unsigned until a signing keystore is configured.

## Contributing

Parsing rules are the easiest place to add value. Each rule lives in `parsing/rules/` and must ship with a unit test using real, anonymized sample message text. See `AGENTS.md` for the full conventions and definition of done.
