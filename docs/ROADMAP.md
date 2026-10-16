# Spend Tracker — Future Features Roadmap

A prioritized backlog of planned future work for Spend Tracker. It is organized by
priority tier and presented as a living checklist. "Not yet" does not mean
"never" — it means "not v1, and here is why it is deferred."

This doc is derived from `AGENTS.md` (notably §2 Current status, §4 Tech stack,
§8 Reports, §9 Releases, §10 Non-goals, §11 Open decisions) and verified against
the current codebase. Items marked with an ⚠️ are open decisions that need an
explicit call before implementation; do not assume the answer.

Priority tiers:
- **P0 Critical** — blocks a core promise or a real correctness/user need.
- **P1 High** — high user value, clearly within the privacy/minimal-permissions
  charter.
- **P2 Medium** — useful enhancements; nice-to-have, watch scope.
- **P3 Future / out of scope v1** — valid ideas that conflict with v1 constraints
  (privacy, permissions, scope) or that are explicitly non-goals.


## P0 — Critical (must-have)

- [ ] **Email ingestion — Gmail API (read-only)**
  - `GmailClient` currently returns `emptyList`; `EmailProcessor` is fully wired
    but idle. Implement the OAuth2 flow scoped to `gmail.readonly` and a
    periodic sync worker that feeds emails through the existing
    `EmailProcessor` → `ParserEngine` → `TransactionRepository.insertEmail`
    pipeline.
  - Store Gmail tokens in encrypted SharedPreferences (Keystore), never in the
    Room DB; exclude from backup.
  - ⚠️ **Open decision (AGENTS §11):** Gmail-only vs. IMAP fallback for v1.
    IMAP is a new on-device socket connection and must remain read-only; it
    also needs credential storage. Decide before designing the OAuth worker
    cadence and how fetched messages are deduped against the `rawMessageRef`
    uniqueness index.
  - Acceptance: unit test `EmailProcessor` over a fake `EmailClient`; the
    `parse → insertEmail` path already has a seam to test against.

- [x] **Retroactively categorize existing transactions**
  - All rows inserted before categorization was wired are stuck as `OTHER`.
    Add an idempotent backfill (a `OneTimeWorkRequest` or an in-DB update) that
    resolves `OTHER` rows using the same `resolveCategory` precedence
    (user overrides first, then `TransactionCategorizer`) and updates them.
    Must be resumable and must not re-touch rows the user has manually edited
    since.
  - Acceptance: a migration-style or worker test proving `OTHER` rows are
    reclassified and non-`OTHER` rows are left alone.
  - Done: `BackfillCategoriesWorker` (one-shot, `KEEP`) →
    `TransactionRepository.recategorizeOtherTransactions()` walking `OTHER` rows
    by id cursor. Only `OTHER` rows are candidates, so manually edited rows are
    untouched. Covered by `TransactionRepositoryCategorizationTest`.

- [x] **Merchant → category editor UI**
  - The `merchant_categories` table and `categoryFor()` DAO exist and are
    consulted on insert, but there is no way to populate it. Build an editable
    list (add/edit/delete merchant → category) surfaced from **Settings** or
    the change-category flow, writing through `MerchantCategoryDao.upsert`.
  - Should normalize the merchant key (trim/uppercase) at write time so the
    exact-match `categoryFor` is reliable.
  - Acceptance: Compose UI test editing a mapping then re-classifying a
    transaction through the repository.
  - Done: `MerchantCategoryScreen`/`MerchantCategoryViewModel` (Settings →
    *Merchant categories*), with a stateless `MerchantCategoryRow`. Writes go
    through `TransactionRepository.setMerchantCategory`/`deleteMerchantCategory`,
    which normalize keys via `MerchantKey.normalize` (trim + uppercase); the
    `categoryFor` lookup is case-insensitive so existing/directly-written keys
    still resolve. Editing a mapping then re-classifying is covered by
    `MerchantCategoryRepositoryTest`, the ViewModel by
    `MerchantCategoryViewModelTest`, and the editor interactions by
    `MerchantCategoryScreenTest` (Compose, `src/testDebug`).


## P1 — High (strong value, within charter)

- [ ] **Teach-a-rule flow for the Unrecognized queue**
  - The Alerts tab surfaces unrecognized messages but offers no way to turn one
    into a rule. Provide manual tagging (pick category; record the sender/body
    snippet as a hint) and/or a guided regex builder.
  - ⚠️ **Open decision (AGENTS §11):** regex builder vs. manual tagging UX.
    Regex is powerful but risky to ship without a dry-run/preview.
  - Tie into the explainability principle: the taught rule must keep a reference
    back to the source message.

- [ ] **Expand supported bank/PSP rule set**
  - Today: HDFC, ICICI, SBI, generic UPI. Add rules for other common Indian
    senders (e.g. IDFC, Axis, Kotak, RBL, Yes Bank, PhonePe/Paytm app
    notifications, SBI/ICICI UPI, international cards).
  - Each rule ships with anonymized sample messages and a unit test (per
    `AGENTS.md` §7/§12).

- [ ] **Large/unusual transaction flagging (EOM)**
  - `AGENTS.md` §8 lists "unusual/large transactions flagged" for EOM as a
    *verify-against-code* item. The current `MonthlyReport` does **not** include
    it. Implement a simple heuristic (e.g. above a user-set or
    statistic-of-the-month threshold) surfaced in the Insights screen.
  - Keep local + opt-in: no server-side ML.

- [ ] **Manual transaction entry**
  - Let users record a transaction by hand (amount, date, merchant, category,
    account) for cash/offline spending. Persisted via `TransactionRepository`
    with `source = SMS` semantics but a synthetic `rawMessageRef`.
  - Acceptance: ViewModel state test + a Compose UI test for the entry form.

- [ ] **Transaction notes / rename merchant**
  - Let users add a free-text note and/or rename a merchant on a transaction;
    feed the rename into the user `merchant_categories` mapping to improve
    future auto-categorization.

- [ ] **Subscriptions / recurring-spend detection (passive)**
  - Detect recurring merchants (same amount + merchant at regular cadence) as a
    passive Insight only — no budgeting/alerting. Surfaces patterns, doesn't
    nag.

- [ ] **Export (opt-in, user-initiated only)**
  - A manual "Export" action (CSV or JSON of transactions for a selected period),
    gated behind a deliberate tap and respecting the no-backup stance (the DB
    stays excluded from system backup; export is an explicit user action).
  - Must not transmit data; the share sheet stays on-device.


## P2 — Medium (valuable polish / growth)

- [ ] **On-device model as a parsing fallback (stretch goal)**
  - `AGENTS.md` §3/§10: deterministic rules first; an optional on-device model is
    a stretch goal, not v1. If rules plateau, explore a tiny on-device model to
    recover the unrecognized queue — with the same explainability guarantee
    (reference back to the source message).
  - Not a blocker; revisit only after rule-based accuracy is acceptable.

- [ ] **Extended Insights views**
  - Daily/weekly spend charts (Canvas — no chart library, per taste), trend vs.
    last month on the monthly tab, and the "See all categories" screen that the
    Home top-categories chip already links to (`onNavigateToInsights`).

- [ ] **Home-screen widget**
  - A glanceable widget showing month-to-date spend, updated from the same
    repository Flow the Home screen uses.

- [ ] **Category & account editors**
  - Let users add/remove/rename user-defined categories/accounts. Requires
    migrating `merchant_categories`/account storage into a user-editable set.

- [ ] **Multi-currency / locale awareness**
  - Today the app is INR-first (`Formatters` uses `Locale("en","IN")`). Generalize
    currency detection in `AmountParser` and per-currency formatting if/when the
    supported banks span multiple currencies.

- [ ] **Search & filter refinements (Activity)**
  - Already has merchant/amount search + category filter. Extend: date range,
    amount range, direction (in/out), exclude duplicates.

- [ ] **Receipt attachment (local only)**
  - Optionally attach a photo per transaction, stored as a file URI (never in
    Room as a BLOB). Respects "sensitive content on-device only."


## P3 — Future / out of scope for v1

These are intentionally deferred because they conflict with the
`AGENTS.md` §3 "minimal permissions" or §10 "Non-goals" constraints.

- [ ] **Cloud sync / multi-device** — `AGENTS.md` §10 explicit non-goal.
- [ ] **Bill-splitting** — explicit non-goal (spend visibility only).
- [ ] **Budgeting alerts / spending-limit notifications** — explicit non-goal;
    alerts are intentionally not "budgets."
- [ ] **Investment tracking** — explicit non-goal.
- [ ] **iOS version** — explicit non-goal (no iOS in v1).
- [ ] **NotificationListenerService for app notifications** — `AGENTS.md` §11
    flags this as a new permission class that conflicts with the minimal-
    permissions principle. Do **not** add without an explicit decision and an
    update to §3 permissions + the in-app rationale screen. (This is what
    "UPI notifications" would require — app-notification reading is NOT the
    current SMS-based path.)
- [ ] **LLM-based parsing in v1** — `AGENTS.md` §10: deterministic rules must
    reach acceptable accuracy first; revisit only if rule-based parsing
    plateaus (then see P2 on-device fallback).


## Release & CI coordination (non-negotiable checklist)

From `AGENTS.md` §9 and repo taste:

- [ ] CI builds **only** on `v*` tags and manual dispatch (do not add
      branch-push triggers).
- [ ] Releases are tagged with semantic-version tags (`v0.x.0`) and the APK is
      attached to a GitHub Release from CI secrets (never committed).
- [ ] **Never** commit the keystore or signing credentials.
- [ ] Keep `AGENTS.md` §2/§3, `README.md`, and these docs in sync as features
      land.
- [ ] No "Co-Authored-By" trailers in commit messages; clean history is
      preferred (force-push/rewrite of shared refs is acceptable).

---

Last verified against the codebase on 2026-10-16. Re-evaluate priority tiers as
email ingestion lands, since it unlocks follow-on items.
