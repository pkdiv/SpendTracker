# Spend Tracker — Developer Documentation

Architecture, data flow, and conventions for the native Android Spend Tracker app.
The companion user-facing guide lives at [`USER_GUIDE.md`](./USER_GUIDE.md).
The in-repo source-of-truth for constraints/conventions is [`AGENTS.md`](../AGENTS.md);
treat this doc as the elaboration.

---

## 1. What it is, at a glance

A Kotlin Android app that reads incoming SMS (and, eventually, email) on-device,
detects transaction/spend messages, parses amount/merchant/direction/timestamp,
stores them locally in Room, auto-categorizes them, and produces an end-of-day
(EOD) notification and end-of-month (EOM) insight report.

Privacy constraints govern every design decision (see §9). Message content never
leaves the device, is never sent to a cloud/LLM, and the local DB is excluded
from backup.

### Tech stack

| Layer              | Choice                                                      |
| ------------------ | ---------------------------------------------------------- |
| Language           | Kotlin 2.0.21                                              |
| UI                 | Jetpack Compose + Material 3                                |
| Architecture       | MVVM, unidirectional data flow, sealed UI state            |
| DI                 | Hilt                                                       |
| Local storage      | Room (SQLite); Preferences DataStore for settings           |
| Background work    | WorkManager (Hilt workers)                                 |
| Coroutines         | `kotlinx.coroutines` + `Flow` (no callbacks / no RxJava)   |
| MinSdk / target    | 26 / 35                                                    |
| Build              | AGP 8.13 / Gradle 8.13, JDK 21, `jvmTarget = 17`            |

`libs.versions.toml` is the single source of truth for dependency versions.

---

## 2. Project layout

```
app/src/main/java/dev/pkdiv/spendtracker/
  data/           SettingsStore.kt          Preferences DataStore + AppSettings
  data/db/        SpendTrackerDatabase.kt   Room DB (v9) + migrations + TypeConverters
                  TransactionEntity / DAO    transactions table
                  UnrecognizedMessageEntity / DAO
                  MerchantCategoryEntity / DAO   merchant -> category override table
                  DuplicateDetector.kt       near-duplicate matching window
                  TransactionSource.kt       SMS | EMAIL | MANUAL enum
                  Converters.kt (inner)      BigDecimal/Instant/enum Room converters
  parsing/        ParserEngine.kt            routes a message to the first matching rule
                  AmountParser.kt            amount/direction/timestamp extraction
                  ParsedTransaction.kt       parse output model
                  ParseResult.kt             sealed Parsed | Unrecognized
                  TransactionCategory.kt     categories + TransactionDirection
                  TransactionCategorizer.kt  default keyword -> category rules
                  rules/                    one ParsingRule per bank/PSP
                    HdfcBankRule.kt, IciciBankRule.kt, SbiBankRule.kt, UpiRule.kt
  ingestion/
    sms/          SmsReceiver.kt            real-time SMS_RECEIVED broadcast receiver
                  SmsReader.kt              one-shot inbox backfill
                  SmsProcessor.kt           parse + persist for a single SMS
                  SmsBodyResolver.kt        re-fetch an SMS body by provider row id
    email/        EmailClient.kt            EmailMessage data class + EmailClient iface
                  GmailClient.kt             stub (returns emptyList) — planned
                  EmailProcessor.kt          parse + persist for a single email
  repository/     TransactionRepository.kt  single write path (dedup + categorize + persist)
                  TransactionDetail.kt      UI-ready detail model + MessageUnavailableReason
                  MerchantCategoryMapping.kt UI-ready merchant -> category override
  reports/        ReportGenerator.kt        loads rows for a day/month
                  ReportAggregator.kt       pure aggregation (totals, top, by-category)
                  ReportModels.kt           DailyReport / MonthlyReport / CategorySpend / MerchantSpend
  export/         TransactionCsv.kt           pure CSV serialization (parsed fields only)
                  ExportPeriod.kt            pure export window (this/last/all time)
                  ExportFileWriter.kt        writes CSV to cache + FileProvider content uri
  work/           EodReportWorker.kt         daily notification
                  EomReportWorker.kt          month-end notification
                  ReportScheduler.kt          schedules the two periodic workers
                  RawMessageBackfill.kt       fills in bodies missing from transactions
                  BackfillRawMessagesWorker.kt Hilt worker wrapper around backfill
                  BackfillCategoriesWorker.kt  one-shot re-categorize of `OTHER` rows
                  WidgetUpdateWorker.kt       one-shot home-screen widget render
                  WidgetScheduler.kt          enqueues the widget refresh (KEEP)
  widget/         SpendWidgetProvider.kt     home-screen AppWidgetProvider
                  SpendWidgetUpdater.kt       reads month-to-date + renders RemoteViews
                  WidgetSpend.kt              pure month-to-date model (WidgetSpend.from)
                  WidgetRefresher.kt          refresh seam bound to WidgetScheduler
  ui/             SpendTrackerApp.kt          NavHost + bottom bar (hoisted nav callbacks)
                  Formatters.kt              INR formatting (Indian grouping), date labels
                  components/              stateless composables (hoisted state)
                  home/, detail/, reports/, settings/, unrecognized/,
                  history/                 Activity list + HistoryFilters/HistoryFilterSheet
                  merchantcategory/        merchant -> category editor
                  addtransaction/          manual-entry form (AddTransactionScreen/ViewModel)
                  export/                  opt-in CSV export (ExportScreen/ViewModel)
  di/             AppModule.kt               DB + worker factory providers
                  BindingModule.kt           SmsBodyResolver + WidgetRefresher bindings
  util/           HtmlToText.kt               strip HTML for email bodies
                  Privacy.kt                 cap bodies + mask OTP codes
                  MerchantKey.kt             canonical merchant key (trim + uppercase)
                  AmountInput.kt             pure amount input sanitize/parse
  MainActivity.kt, MainViewModel.kt        permissions + trigger backfill
  SpendTrackerApp.kt (root)  Application, Hilt, WorkManager config + schedule on launch
```

Tests: `app/src/test` (JUnit 5 + JUnit 4/Robolectric + Turbine + WorkManager test)
and `app/src/testDebug` (Compose UI tests with `createComposeRule`, debug-only).

---

## 3. Data flow

```
SMS arrives  ──► SmsReceiver.onReceive                 (real time, background coroutine)
                 │
                 ▼
           SmsProcessor.process(sender, body, ref, ts)
                 │
                 ├── ParserEngine.parse(sender, body, ref)        ──► ParseResult.Parsed | Unrecognized
                 │       Parsed  ──► TransactionRepository.insert
                 │       Unrecognized ──► insertUnrecognized
                 │
                 │  (periodic / on launch)
                 ▼
           SmsReader.backfill() scans the full inbox through SmsProcessor
           BackfillRawMessagesWorker fills missing bodies via SmsBodyResolver

Email (future) ──► EmailProcessor.process(email) ──► TransactionRepository.insertEmail

Manual entry  ──► AddTransactionViewModel.save()  ──► TransactionRepository.insertManual
```

`TransactionRepository` is the **only** write path for transactions. Every insert
goes through `transactionDao.insertDeduplicated(...)`, which checks
`DuplicateDetector` against recent rows (same sender + direction + amount +
merchant within a 2-minute window) and sets `isDuplicate` instead of dropping.
If a duplicate is detected, the existing row's raw body/time are backfilled via
`updateRawMessage`.

`insertManual` is the exception: a hand-entered transaction (`source = MANUAL`)
bypasses duplicate detection and stores the user's chosen category verbatim
rather than running `resolveCategory`, so an explicit pick is never overridden by
the categorizer. It carries a synthetic `manual:<uuid>` `rawMessageRef`, and
`AddTransactionViewModel` refreshes the home-screen widget after the insert.

### Parsing model

`ParserEngine` holds an ordered registry of `ParsingRule` instances. A rule
decides `matches(sender, body)` and produces a `ParsedTransaction`
(amount, direction, merchant, account, timestamp, rawMessageRef). Rules are
deliberately per-sender (bank message formats diverge too much for one regex).
A parsed transaction is always traceable to its source message via `rawMessageRef`
(e.g. `sms:<id>`). Unmatched messages never vanish — they become
`ParseResult.Unrecognized` and surface in the **Alerts** screen.

`AmountParser` is a stateless helper: it detects spend keywords
(`debited/spent/paid/charged/withdrawn`), skips the "Avl" balance segment, and
parses dates/timestamps from several Indian formats. Pure and unit-tested.

---

## 4. Data layer

### Room schema (`SpendTrackerDatabase`, version 9)

Three tables, all with exported JSON schemas under `app/schemas/` and
migrations shipped as `Migration` objects (no destructive fallbacks):

- `transactions` — amount, direction, merchant, account, category, timestamp,
  rawMessageRef, smsId, rawSender, rawBody, source, sourceRef, rawSubject,
  receivedAt, bodyTruncated, isDuplicate, note (user free text, nullable).
  Unique index on `rawMessageRef`; index on `receivedAt`.
- `unrecognized_messages` — sender, body, receivedAt, smsId, rawMessageRef
  (unique), dismissed. Alerts are **soft-dismissed** (`dismissed = 1`) rather
  than deleted: the row stays so the unique `rawMessageRef` index stops the SMS
  backfill from re-inserting a message the user already cleared. Because the live
  `SmsReceiver` and the `SmsReader` backfill derive different `rawMessageRef`s for
  the same SMS, `insertUnrecognized` also skips a message whose
  sender + receivedAt + body is already known (dismissed or not).
- `merchant_categories` — `merchant` (PK) → `category`. User-editable override.

Migrations: `MIGRATION_1_2` (dedup), `_2_3` (smsId), `_3_4` (raw columns),
`_4_5` (drop rows missing raw columns), `_5_6` (message source metadata),
`_6_7` (duplicate flag + timestamp index), `_7_8` (alert soft-dismiss flag),
`_8_9` (per-transaction note column).
Every migration has a verification test (see `Migration6To7Test`,
`Migration7To8Test`, `Migration8To9Test`) asserting old-schema rows survive.

### Categorization

On **insert**, `TransactionRepository.resolveCategory(merchant, fallback)` decides
the category:

1. `MerchantCategoryDao.categoryFor(merchant)` — user-editable table, looked up
   by the normalized key (trim + uppercase, `MerchantKey.normalize`) with a
   case-insensitive match (highest precedence).
2. `TransactionCategorizer.categorize(merchant)` — ordered, case-insensitive
   keyword rules (built-in defaults).
3. Otherwise `ParsedTransaction.category` (currently `OTHER`).

`TransactionCategorizer` is a pure class with an ordered keyword list
(`"PRIME VIDEO"`/`"AMAZON PRIME"` before generic `"AMAZON"`; grocery apps
before generic `"MART"`; self-transfer phrases first). Adding a merchant to the
user table overrides the defaults without code changes.

**Editing the table.** **Settings → Merchant categories** opens
`MerchantCategoryScreen` (with `MerchantCategoryViewModel`) to add, edit and
delete overrides. Writes go through
`TransactionRepository.setMerchantCategory` / `deleteMerchantCategory`, which
normalize the merchant key, so a mapping applies to both future inserts and the
backfill resolver. Renaming a row's merchant deletes the old key (the merchant
is the primary key).

**Renaming a transaction's merchant.** From the transaction detail screen,
`TransactionRepository.renameMerchant(id, merchant)` updates the row's `merchant`
and *also* upserts a `merchant_categories` override (new name → that
transaction's current category), so the rename teaches future auto-categorization.
It reuses `setMerchantCategory`, so the new key is normalized the same way; blank
names are ignored. Notes are written by `TransactionRepository.setNote(id, note)`,
which trims and stores blank as `null` (clearing the note).

**Retroactive categorization.** Rows stored before categorization was wired keep
`OTHER` until `TransactionRepository.recategorizeOtherTransactions()` runs (via
`BackfillCategoriesWorker`, scheduled once with a `KEEP` policy on app start). It
walks `OTHER` rows in id order in batches via
`TransactionDao.otherCategoryTransactions(afterId, limit)` and resolves each with
the *same* `resolveCategory` precedence, so it is idempotent (a second run updates
nothing) and resumable (already-reclassified rows are no longer `OTHER`). Only
`OTHER` rows are candidates, so a transaction the user manually re-categorized is
never re-touched; rows the resolver can't place stay `OTHER`.

### Reports

`ReportAggregator` is pure logic: `spend()` filters to `DEBIT` && `!SELF_TRANSFER`
&& `!isDuplicate`. `daily()` and `monthly()` produce totals, top merchants, and
category splits (the monthly one also returns the previous month for a
month-over-month comparison). `ReportGenerator` wraps DAO time-window queries.
Both are unit-tested over synthetic entity lists.

### Background work

`SpendTrackerApp.onCreate` schedules:
- `EodReportWorker` — periodic (~daily), initial delay to the user-configured
  EOD time (`SettingsStore.eodHour/eodMinute`), posts a notification with the
  day's spend if any, then `markEodRun(today)`. WorkManager reconciles missed
  runs by checking `lastEodDate`.
- `EomReportWorker` — periodic daily; early-returns unless `dayOfMonth == 1`,
  then builds the prior month's report and posts a notification.
- `BackfillRawMessagesWorker` — one-shot, `KEEP` policy, gated on
  `READ_SMS`; `RawMessageBackfill` iteratively resolves pending rows in batches
  and stops when a batch resolves nothing (so unresolvable rows don't loop).
- `BackfillCategoriesWorker` — one-shot, `KEEP` policy; re-categorizes `OTHER`
  rows once through `recategorizeOtherTransactions()`. No permission needed and
  it is a no-op once every resolvable row has a category.
- `WidgetUpdateWorker` — one-shot, `KEEP` policy, enqueued by `WidgetScheduler`
  from the widget provider's `onUpdate`, on app start, and after each parsed SMS.
  `SpendWidgetUpdater` renders the current month's spend (`RemoteViews`) from the
  same `TransactionRepository.observeBetween` flow the Home screen uses; `KEEP`
  collapses the per-message calls during an inbox backfill into one pending job.

WorkManager is configured with Hilt's `WorkerFactory` via a manifest
`Configuration.Provider`.

---

## 5. UI / Compose conventions

- Single-activity, bottom-bar navigation: **Home**, **Activity**,
  **Insights**, **Alerts**, **Settings**. Non-tab routes: `transaction/{id}`
  (detail) and `merchant_categories` (reached from Settings).
- Bottom-bar and click callbacks are **hoisted** to `SpendTrackerApp` (the NavHost);
  row composables take callbacks `(Long) -> Unit` and never reference ViewModels.
- Each screen has a `@HiltViewModel` exposing one `StateFlow<sealed UiState>`
  (`Loading/Content/Unavailable/NotFound`-style hierarchies); all states render,
  including skeleton/empty states.
- Activity filtering is factored into a pure `HistoryFilters` + `filterTransactions`
  (in `ui/history`) applied in the ViewModel; the controls live in a
  `HistoryFilterSheet` (`ModalBottomSheet`) hosting a stateless `HistoryFilterContent`.
- Components are stateless, `Modifier` is the first optional parameter, and
  stable/immutable params are used to keep recompositions cheap.
- `@Preview` coverage (light + dark, empty/loading/populated fakes via a
  `PreviewData` object) is expected for new composables.
- Accessibility: ≥48dp touch targets, merged row semantics, contentDescription on
  icons, no clipping at 200% font scale.
- INR formatted with Indian grouping; paise rendered smaller in hero amounts.
- Rows that are neither income nor spend — duplicates and `SELF_TRANSFER` — are
  rendered muted (`isMutedRow` in `CategoryVisuals.kt`): the amount and merchant
  drop the income/expense colour for `onSurfaceVariant`, so self transfers read as
  neutral grey rather than red/green. Applied in `TransactionRow` and the detail
  hero (`SummaryHeader`).
- Home shows the month hero (`HeroSpendCard`) plus a swipeable week card
  (`WeekSpendPager`, a `HorizontalPager` with dots) paging through the last four
  weeks, most recent first. It reads its own repository flow over that 4-week
  window, independent of the month selector; day bucketing comes from the pure
  `ReportAggregator.spendByDay`, and per-week totals/counts are assembled in
  `HomeViewModel` into `WeekSpend` pages.
- The **home-screen widget** is the one non-Compose UI surface: a classic
  `AppWidgetProvider` (`SpendWidgetProvider`) + `RemoteViews` layout
  (`res/layout/widget_spend.xml`, no Glance dependency). It shows the same
  month-to-date spend the Home hero does, aggregated by the pure
  `WidgetSpend.from`. `SpendWidgetUpdater` reads the repository and pushes views;
  text/background colors come from `values/colors.xml` with a `values-night`
  override so it follows the system theme. Its `previewLayout` in
  `res/xml/spend_widget_info.xml` is the widget-picker equivalent of a `@Preview`.
- Manual entry lives at `ui/addtransaction/`: an **Add** FAB on Activity
  (`onAddTransaction` hoisted to the NavHost) opens `AddTransactionScreen`. The
  form is a stateless `AddTransactionContent` over one `AddTransactionUiState`
  (`Editing(form, errors, saving)` → `Saved`), driven by
  `AddTransactionViewModel`; amount input is sanitized/parsed by the shared pure
  `AmountInput`, and validation is `ManualTransactionForm.errors()`.
- The detail screen special-cases `TransactionSource.MANUAL`: it shows "Manual"
  and drops the reference + "Original message" rows, since a hand-entered
  transaction has no source message.
- The detail screen's overflow menu also offers **Rename merchant** and
  **Add/Edit note**, opening `RenameMerchantDialog` / `TransactionNoteDialog`
  (mirroring `MerchantCategoryEditorDialog`). The merchant header re-renders live
  from the repository flow; a note shows in a `Note` card only when set.
- Export lives at `ui/export/`: **Settings → Export** opens `ExportScreen`, a
  stateless `ExportContent` over one `ExportUiState`
  (`Loading`/`Content(period, count, exporting)`/`Shareable`). The user picks a
  period, taps **Export CSV**, and `ExportViewModel` builds the file via the pure
  `TransactionCsv` + `ExportFileWriter`; the `Shareable` state drives a
  `LaunchedEffect` that starts `Intent.createChooser(ACTION_SEND)` for `text/csv`
  and then returns to the form. Nothing runs without the explicit tap.

---

## 6. Privacy & security

- **Local-first.** No network is used for parsing. `INTERNET` is declared only
  for the future email fetch path; no message content is ever sent over it.
- **No logging of raw message content.** `NoRawBodyLoggingTest` scans the source
  tree for `Log.*`/`Timber`/`println` calls that reference `rawBody`/`rawSender`
  and fails the build if any appear.
- **Display masking.** `Privacy.maskSensitive` redacts OTP/verification codes in
  the message card (tap-to-reveal); raw bodies are capped at 64KB
  (`Privacy.capBody`) and HTML email is stripped (`HtmlToText`).
- **No backups.** `backup_rules.xml` and `data_extraction_rules.xml` exclude
  `spendtracker.db*` from both cloud backup and device transfer.
- **Export is explicit and on-device.** `TransactionCsv` writes parsed fields only
  (never raw message content) into the app **cache** (never backed up), exposed to
  the system share sheet through a `FileProvider`. The app performs no upload — the
  user picks the destination — and the flow only runs on a deliberate tap.
- **Permissions** are runtime-requested (`POST_NOTIFICATIONS` on Android 13+) and
  surfaced with rationale strings; see §3 of `AGENTS.md` for the full list —
  adding one requires updating the permission-rationale screen.

---

## 7. Testing

`./gradlew test --no-daemon` (run with `JAVA_HOME` pointing at JDK 21 if the
system default is newer than 21). Compose UI tests live in `src/testDebug` so the
`debugImplementation`-scoped `ui-test-manifest` `ComponentActivity` doesn't leak
into production.

Coverage layers:
- **Parsing rules** — each rule (`Hdfc/Upi/...` `RuleTest`) with real anonymized
  samples; `ParserEngineTest` covers routing + unrecognized fallback.
- **Categorization** — `TransactionCategorizerTest` covers specificity ordering
  and the `OTHER` fallback; `TransactionRepositoryCategorizationTest` covers the
  retroactive backfill (reclassifies `OTHER`, leaves non-`OTHER` and user
  overrides alone, idempotent across batches).
  `MerchantCategoryRepositoryTest` covers the editor's data path (normalized
  writes, upsert/delete, re-classifying a transaction) and `MerchantKeyTest` the
  pure key normalization.
- **Data** — `Migration5To6Test`, `Migration6To7Test`, `Migration7To8Test`,
  `Migration8To9Test` (row survival); `DuplicateDetectorTest`.
- **Reports** — `ReportAggregatorTest`.
- **Privacy** — `NoRawBodyLoggingTest`, `PrivacyTest`, `HtmlToTextTest`.
- **Workers** — `BackfillRawMessagesWorkerTest` (permission-revoke + unresolved
  cases).
- **Widget** — `WidgetSpendTest` (pure: debits-only sum, excludes self
  transfers/duplicates/credits, empty → zero) and `SpendWidgetUpdaterTest`
  (Robolectric + in-memory Room: the month window read ignores prior months).
- **Manual entry** — `AmountInputTest` (pure sanitize/parse) and
  `ManualTransactionFormTest` (pure validation); `AddTransactionViewModelTest`
  (Robolectric + in-memory Room: stores a `MANUAL` row, the chosen category wins
  over the categorizer, blank amount/merchant block the save and refresh nothing);
  `AddTransactionScreenTest` (Compose, `src/testDebug`) drives the form end to end.
- **Export** — `TransactionCsvTest` (pure: header/rows, escaping, ordering, and
  that raw message content is never included) and `ExportPeriodTest` (pure window
  math); `ExportViewModelTest` (Robolectric: period counts, and that a tap hands the
  CSV to the injected writer and yields a `content://` uri, using a recording fake);
  `ExportFileWriterTest` (Robolectric: the real `CacheExportFileWriter` writes into
  the cache and exposes it through the `FileProvider`); `ExportContentTest` (Compose,
  `src/testDebug`). The unit-test manifest (`src/test/AndroidManifest.xml`) mirrors
  the app's `FileProvider` declaration so the writer test can resolve its
  `content://` uri.
- **Notes & rename** — `TransactionEditRepositoryTest` (Robolectric + in-memory
  Room: `renameMerchant` updates the row and teaches the normalized mapping, blank
  names are ignored; `setNote` trims and clears blank to null);
  `TransactionDetailViewModelTest` covers the rename/note actions re-emitting
  detail, and `TransactionDetailScreenTest` (Compose) the note section + dialogs.
- **ViewModels** — `TransactionDetailViewModelTest`,
  `MerchantCategoryViewModelTest`, `HistoryViewModelTest`,
  `UnrecognizedViewModelTest`, `FormattersTest`.
- **UI** — `TransactionDetailScreenTest` (Compose, Robolectric) for the row→detail
  flow and duplicate badge; `MerchantCategoryScreenTest` for the mapping
  add/edit/delete interactions; `HistoryFilterContentTest` for the Activity
  filter controls; `UnrecognizedContentTest` for the Alerts clear-all flow.
  `HistoryFiltersTest` covers the pure filter logic.

Flow/ViewModel tests await a terminal state via `first { predicate }` rather than
Turbine's item-by-item `awaitItem()`.

ViewModel tests extend `ViewModelTest` (`dev.pkdiv.spendtracker.testutil`), which
installs an unconfined test main dispatcher and, on teardown, cancels every tracked
ViewModel's scope *before* resetting `Dispatchers.Main`. Cancelling first matters: a
ViewModel coroutine can otherwise resume on `Dispatchers.Main` from a background
thread after the test body and race the next test's `Dispatchers.setMain`, which
`TestMainDispatcher` reports as a concurrent read/write `IllegalStateException`.
Register ViewModels with `track(...)` and do not call `Dispatchers.setMain` directly.

---

## 8. Building, running, releasing

```
# JDK 21 is required (AGP 8.13 won't build on JDK 24/25)
JAVA_HOME=/home/pkdiv/.jdks/jdk-21.0.11+10 ./gradlew test assembleDebug --no-daemon
```

- Room exports schemas to `app/schemas/` (already `.gitignore`d appropriately via
  `room.schemaLocation`).
- Release tagging & CI: push a `v*` tag or use `workflow_dispatch` on
  `build-and-publish.yml`. CI builds, tests, decodes the signing keystore from
  repo secrets (never committed), builds `assembleRelease`, and attaches the APK
  to a GitHub Release named after the tag.
- A versioned GitHub Release with the APK attached is created on each `v*` tag
  push. (A `main-latest` prerelease job is configured but its
  `if: github.ref == 'refs/heads/main'` never matches the tag-only trigger, so it
  doesn't currently fire on branch pushes — see `AGENTS.md` §4 on CI cadence.)
- **Never** commit signing credentials or a `signingConfigs` block with inline
  secrets.

### Conventions recap
- Sealed classes over enums for data-carrying states; enums for simple
  value sets (`TransactionCategory`, `TransactionDirection`, `TransactionSource`).
- No hardcoded UI strings — use `strings.xml` resources from day one.
- Every Room migration ships with a survival test.
- Presentation-only changes stay out of data/domain/parsing/ViewModel logic.

---

## 9. Known gaps & future work

- **Email ingestion** is stubbed (`GmailClient` returns `emptyList`); no OAuth,
  no sync worker, IMAP fallback not started.
- **Teach-a-rule** flow (regex builder / manual tagging) for the Unrecognized
  queue is not implemented.
- `versionName` is static (`0.1.0`); tags drive releases and do not auto-sync
  into the manifest version. Settings › **About** shows whatever `versionName`
  the build produced (read from the package manager, `BuildConfig` is off).
