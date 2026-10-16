# Spend Tracker — User Guide

Spend Tracker watches your transaction SMS messages (and, in the future, email)
on-device, automatically detects the ones about money, and turns them into a
clear picture of how you spend — no manual entry, no accounts, no cloud sync.

The technical architecture is described in
[`DEVELOPMENT.md`](./DEVELOPMENT.md). Everything happens on your phone.

---

## What it does

- **Reads your SMS** as bank/UPI transaction alerts arrive.
- **Parses** the amount, who you paid, your account/card, and the direction
  (money in vs. money out).
- **Stores** transactions locally — never on a server.
- **Categorizes** each transaction automatically (e.g. *Food*, *Transport*,
  *Bills*) using sensible defaults for common Indian merchants; you can re-categorize any transaction.
- **Summarizes** your day with an end-of-day notification, and your month in the
  Insights screen (totals, category breakdown, top merchants, and a comparison
  with the previous month).
- **Surfaces** messages it didn't understand in the Alerts tab so you can dismiss them.

## What it needs

Spend Tracker asks for three permissions, all with plain-language reasons:

- **SMS access** — *Required.* Reads transaction alerts from your inbox so it can
  parse and store them. Without this it can't do anything.
- **Notifications** — *Required for the daily summary.* Needed on Android 13+; the
  EOD (end-of-day) report won't post without it.
- **Internet** — *Declared but only intended for the future email feature.* No
  message content is ever sent over the network. Parsing is 100% on-device.

You'll be prompted for SMS and notification permissions the first time you open
the app. You can review or re-grant them later in **Settings › Permissions**.

## Getting started

1. Install the app (APK is published per release).
2. Open it and grant the requested permissions.
3. It will immediately scan your existing inbox and import past transaction
   SMS. New transactions are picked up in real time as they arrive.
4. Visit the **Home** tab to see this month's spend.

## Home

The Home tab is your dashboard:

- A **hero card** showing total spend for the current month, the transaction
  count, and a daily-spend trend line.
- Quick chips for **today**, **this week**, and **daily average**.
- **Top categories** for the month (tap *See all* to jump to Insights).
- A **recent activity** list of your transactions, newest first, grouped by date.
- A **Sync** button (floating action button at the bottom right) to re-scan your
  inbox manually.

Each transaction row shows the category icon + label and the date. Money-out
debits appear in red; credits in green; duplicates are dimmed with a *Duplicate*
badge and excluded from totals.

## Activity

The **Activity** tab shows your full, searchable transaction history. Filter by
category if you want to focus on one type of spending.

## Insights

The **Insights** tab holds the monthly report:

- Total spend and transaction count.
- A **category breakdown** (donut chart) — tap to see the share by category.
- **Top merchants** for the month.
- A **month-over-month comparison** showing how this month compares to last.

## Alerts

The **Alerts** tab collects transaction messages the app couldn't understand
(yet). Tap **Dismiss** once you've checked one, or leave it for later review.
New unrecognized messages appear here with a badge count on the tab.

## Working with a transaction

Open any transaction row to see its detail. From there, or from the Home/Activity
row overflow (three dots), you can:

- **Change category** — pick a more accurate category than the automatic guess.
- **Mark as / remove duplicate** if the app flagged something incorrectly.
- **Reveal/hide code** on the original message — OTP codes are masked by
  default to protect sensitive data.
- **Show full message** / **Show less** — long messages are collapsed by
  default.
- **Copy the message** to the clipboard.

Category and duplicate changes update the totals immediately.

## Settings

In Settings you can:

- **End-of-day report time** — default is 11:00 PM. Move it earlier/later to
  control when the daily summary notification fires.
- **End-of-day notifications** — toggle the daily summary notification on/off.
- Review the status of **SMS access** and **notifications** permissions.

## How categorization works

Spend Tracker categorizes each transaction automatically:

1. **Your custom mappings** — if you've taught it that a specific merchant maps to
   a category, that wins. *(An in-app editor for this is planned; for now the
   built-in defaults apply.)*
2. **Built-in defaults** — a keyword-based categorizer recognizes common Indian
   merchants and assigns *Food, Transport, Shopping, Bills, Entertainment,
   Health*. For example: Swiggy/Zomato → Food, Uber/Ola/IRCTC → Transport,
   Amazon/Flipkart → Shopping, electricity/mobile recharges → Bills, Netflix
   Hotstar → Entertainment, Apollo/Pharmacy → Health.
3. **Other** — anything not recognized stays in *Other*.

If a transaction lands in the wrong category, open it and pick the correct one —
your correction is saved. Transactions imported before automatic categorization
was in place are categorized for you the next time you open the app; anything you
have already re-categorized yourself is left untouched.

## Privacy & where your data lives

Spend Tracker is built around these guarantees:

- **Nothing leaves your phone.** There is no account, no sign-in, no server
  syncing your data. Message bodies and parsed transactions are stored only in a
  local database on the device.
- **Parsing is on-device and rule-based.** No message content is sent to any LLM
  or cloud API.
- **Backups exclude your data.** The local database is explicitly excluded from
  Android's cloud backup *and* device transfer, so resetting or moving your phone
  won't upload your transactions — and won't restore them either. Keep that in mind if you change phones.
- **Sensitive data is masked.** OTP codes in message bodies are hidden by
  default and require a tap to reveal.
- **No analytics or ads.** There are no third-party analytics SDKs and no ad
  libraries.

## Tips

- If a transaction message is missing its full text in the app, pull down in
  Home and tap **Sync** — the app periodically re-fetches bodies that couldn't
  be stored on first read (for example, when the message was very long).
- Duplicate detection ignores the same sender + amount + direction + merchant
  within a 2-minute window. If you see a *Duplicate* badge you didn't expect,
  remove the duplicate mark from the transaction detail.
