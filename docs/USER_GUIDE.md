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

Spend Tracker asks for two permissions, and explains each in plain language
before the Android prompt:

- **SMS access** — *Optional, but needed for automatic tracking.* Reads
  transaction alerts from your inbox so it can parse and store them. Without it
  the app still works: add transactions by hand and view your reports.
- **Notifications** — *Needed for the daily summary.* On Android 13+ the
  end-of-day (EOD) report can't post without it.
- **Internet** — *Declared in the app but not used today.* It is reserved for a
  future email import. No message content is ever sent over the network; parsing
  is 100% on-device.

The **first time you open the app**, a disclosure screen explains what is
accessed, why, and that it stays on your phone. Automatic tracking is switched on
only when you tap **Allow SMS access**. Tap **Continue without SMS** to skip it and
use the app manually. Notification access is explained in a second, short prompt.
You can review or re-grant either permission later in **Settings › Permissions**
(which opens the system app settings).

## Getting started

1. Install the app (the release page publishes both an app bundle and a
   sideloadable APK).
2. Open it and read the first screen. Tap **Allow SMS access** to enable
   automatic tracking, or **Continue without SMS** to add transactions by hand.
3. With SMS access granted, it will scan your existing inbox and import past
   transaction SMS. New transactions are picked up in real time as they arrive.
4. Visit the **Home** tab to see this month's spend.

## Home

The Home tab is your dashboard:

- A **hero card** showing total spend for the current month, the transaction
  count, and a daily-spend trend line.
- A **"Spent this week" card** with the week's (Mon–Sun) total, a date range and
  transaction count, and a small bar chart of daily spend. **Swipe it** to page
  back through the last four weeks (this week, last week, then the two weeks
  before). It always tracks the real weeks, even when you browse a past month.
- Quick chips for **today**, **this week**, and **daily average**.
- **Top categories** for the month (tap *See all* to jump to Insights).
- A **recent activity** list of your transactions, newest first, grouped by date.
- A **Sync** button (floating action button at the bottom right) to re-scan your
  inbox manually.

Each transaction row shows the category icon + label and the date. Money-out
debits appear in red; credits in green. Duplicates are dimmed with a *Duplicate*
badge and excluded from totals, and internal *Self transfer* rows are greyed out
— they're neither income nor spend, so they carry no colour.

## Home-screen widget

Add the **Spend Tracker** widget to your home screen (long-press an empty spot →
*Widgets* → *Spend Tracker*) for a glanceable view of **this month's spending**
and transaction count, without opening the app. Tap the widget to jump straight
into the app.

The widget uses the same numbers as the Home hero — money out only, with
duplicates and internal self transfers excluded. It refreshes as new transactions
arrive and follows your system light/dark theme.

## Activity

The **Activity** tab shows your full transaction history, grouped by day with
the most recent first.

- **Search** by merchant name or amount.
- **Category chips** under the search box narrow the list to one category (tap
  *All* to clear).
- **Filters** opens a sheet where you can also narrow by:
  - **Direction** — money in (credits), money out (debits), or both.
  - **Date range** — pick a from/to range.
  - **Amount range** — a minimum and/or maximum.
  - **Hide duplicates** — exclude transactions the app flagged as duplicates.
- **Clear filters** resets everything (search included). The Filters chip shows a
  count of the active filters, and when nothing matches you'll see a
  "No matching transactions" message.
- **Add** (the floating button) records a transaction by hand — see *Adding a
  transaction manually* below.

## Adding a transaction manually

Not every purchase arrives by SMS — cash, split bills, or anything you paid
offline. Tap **Add** on the Activity screen to record one yourself:

- **Money out / Money in** — the direction (defaults to money out).
- **Amount** — the amount you spent (or received).
- **Merchant or payee** — who it was for, e.g. *Chai stall*.
- **Category** — pick one; your choice is saved exactly as chosen and is never
  re-guessed from the merchant.
- **Account / card** — optional, e.g. the last few digits.
- **Date** — defaults to today; tap it to pick another day.

Tap **Save** and the transaction appears in Activity and counts towards your
totals and reports like any other.

Manual entries are labelled **Manual** on their detail screen (there's no source
message to show, so the original-message section is hidden). They aren't run
through the SMS duplicate check, so adding one never merges with — or gets
flagged against — a texted transaction.

## Insights

The **Insights** tab holds the monthly report:

- Total spend and transaction count.
- A **category breakdown** (donut chart) — tap to see the share by category.
- **Top merchants** for the month.
- A **month-over-month comparison** showing how this month compares to last.

## Alerts

The **Alerts** tab collects transaction messages the app couldn't understand
(yet). Tap **Dismiss** on one you've checked, or use **Clear all** in the header
to remove every alert at once (you'll be asked to confirm). Dismissed and cleared
alerts stay gone — the app remembers them, so re-scanning your inbox won't bring
them back. Leaving an alert queued is fine — new unrecognized messages appear
here with a badge count on the tab.

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
- **Rename merchant** — correct the merchant shown on the transaction. The new
  name is also remembered as a merchant → category rule, so future transactions
  from that merchant categorize automatically.
- **Add note / Edit note** — attach a free-text note to the transaction (for
  example, "split with Ravi" or a warranty reference). Open it again to change
  it; save an empty note to remove it.

Rename and notes are available on the transaction's own screen (from its
overflow, three dots). Category and duplicate changes update the totals
immediately.

## Settings

In Settings you can:

- **End-of-day report time** — default is 11:00 PM. Move it earlier/later to
  control when the daily summary notification fires.
- **End-of-day notifications** — toggle the daily summary notification on/off.
- **Merchant categories** — teach the app which category a merchant belongs to
  (see *How categorization works*).
- **Export** — save or share a CSV of your transactions (see *Export*).
- Review the status of **SMS access** and **notifications** permissions — tap
  either row to open the system app settings and change it.
- Open the **Privacy policy** under **About**.
- See the installed **app version** under **About** at the bottom of the screen.

## Merchant categories

**Settings › Merchant categories** is where you teach the app how to categorize a
merchant. Tap **Add mapping**, enter the merchant name (e.g. *Swiggy*) and pick a
category, and confirm. Tap any existing row to change its category or rename the
merchant; use the trash icon to remove a mapping.

Your mappings are matched by merchant name (case and surrounding spaces are
ignored) and take priority over the built-in defaults. They apply to new
transactions as they arrive and to past ones the app re-scans; transactions you
have already re-categorized by hand are left untouched. Renaming a transaction's
merchant (from its detail screen) also adds a mapping for the new name, so the
rename improves categorization going forward.

## How categorization works

Spend Tracker categorizes each transaction automatically:

1. **Your custom mappings** — if you've taught the app that a specific merchant
   maps to a category (see *Merchant categories* above), that wins.
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

## Export

**Settings › Export** lets you take your data with you. Pick a period — **This
month**, **Last month**, or **All time** — and tap **Export CSV**. The app writes
a spreadsheet-friendly CSV and opens your phone's share sheet, where you choose
where it goes (a file manager, Drive, email, and so on).

- **It only ever runs when you tap it.** Nothing is exported in the background.
- **Nothing is uploaded by the app.** There is no server involved — the share
  sheet is the on-device Android one, and you pick the destination.
- The file contains only your parsed transactions (date, direction, amount,
  merchant, category, account, source) — **never the text of your messages**.

The file is written to the app's temporary storage, so it isn't part of any
backup.

## Privacy & where your data lives

Spend Tracker is built around these guarantees:

- **You choose before anything is read.** A first-run disclosure explains the SMS
  access, and the permission is requested only after you tap **Allow SMS access**.
  Skipping it leaves the app fully usable.
- **Nothing leaves your phone.** There is no account, no sign-in, no server
  syncing your data. Message bodies and parsed transactions are stored only in a
  local database on the device.
- **Parsing is on-device and rule-based.** No message content is sent to any LLM
  or cloud API.
- **Alerts stay local too.** Messages the app couldn't parse wait in the Alerts
  tab only on this device; they are never uploaded.
- **Backups exclude your data.** The local database is explicitly excluded from
  Android's cloud backup *and* device transfer, so resetting or moving your phone
  won't upload your transactions — and won't restore them either. Keep that in mind if you change phones.
- **Sensitive data is masked.** OTP codes in message bodies are hidden by
  default and require a tap to reveal.
- **No analytics or ads.** There are no third-party analytics SDKs and no ad
  libraries.
- **Export is under your control.** Exporting writes a CSV of your parsed
  transactions (never message bodies) and opens the system share sheet. The app
  sends nothing — it goes only where you choose.

The full policy is in the repository at
[`privacy-policy.md`](./privacy-policy.md) and is linked from **Settings › About ›
Privacy policy**.

## Tips

- If a transaction message is missing its full text in the app, tap the **Sync**
  button in Home — the app periodically re-fetches bodies that couldn't be stored
  on first read (for example, when the message was very long).
- Duplicate detection ignores the same sender + amount + direction + merchant
  within a 2-minute window. If you see a *Duplicate* badge you didn't expect,
  remove the duplicate mark from the transaction detail.
