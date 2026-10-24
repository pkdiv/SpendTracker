# Spend Tracker — Google Play Store Listing Copy

Text-only listing copy for Google Play. It matches the behavior documented in
[`USER_GUIDE.md`](./USER_GUIDE.md) and the constraints in
[`../AGENTS.md`](../AGENTS.md). Image assets (icon, feature graphic,
screenshots) still have to be produced outside the repo.

## App name (max 30 chars)

**Spend Tracker**

## Short description (max 80 chars)

**Track spending from bank & UPI SMS — on-device, private, no account.**

## Full description (max 4000 chars)

Spend Tracker turns your bank and UPI transaction SMS into a clear picture of
your spending — entirely on your phone.

**Automatic, on-device tracking**
Spend Tracker reads transaction alerts from your SMS inbox and extracts the
amount, merchant, account/card and date. Parsing is rule-based and runs
on-device: message content is never uploaded to a server, analytics service, or
AI model.

**Private by design**
- No account, no sign-in, no cloud sync.
- No internet permission.
- No ads, no analytics SDKs, no data sharing.
- The local database is excluded from Android cloud backup and device transfer.
- Only transaction-shaped messages are imported; personal SMS and OTP messages
  are ignored.

**Built for Indian spending**
- Rules for HDFC, ICICI and SBI messages, plus generic UPI alerts.
- Amounts use Indian digit grouping (₹1,23,456.00).
- Categories for food, transport, shopping, bills, entertainment and health,
  with editable merchant → category rules.

**Your day and month at a glance**
- Home dashboard with month-to-date spend, a daily trend, a swipeable week card
  and top categories.
- End-of-day notification with the day's total, at a time you choose.
- End-of-month insight with a category breakdown, top merchants and a comparison
  with the previous month.
- Home-screen widget for month-to-date spend.

**Built for real life**
- Add cash or offline spending by hand.
- Add notes and rename merchants; a rename also teaches the app for next time.
- Search and filter by merchant, amount, category, direction, date and
  duplicates.
- Review messages the app couldn't parse in Alerts and dismiss them.
- Export a CSV of your transactions; nothing is uploaded — you choose where it
  goes.

**SMS access is optional**
Automatic tracking needs SMS access, and the app asks only after explaining what
it reads and why. If you skip it, you can still add transactions manually and
view your reports.

Spend Tracker is a spending tracker, not a bank or payment app: it does not move
money, lend, or connect to your accounts.

## Categorization and tags

- **Category:** Finance
- **Tags:** finance, expense tracker, spending tracker, SMS, privacy, India

## Contact and links (required in Play Console)

- **Privacy policy:** <https://pkdiv.com/SpendTracker/privacy-policy> (must be live)
- **Support contact:** <developer@pkdiv.com>
- **Website (optional):** <https://pkdiv.com/SpendTracker>

## Default language and locales

- **Default language:** English (India)
- The app ships only the default `values/strings.xml`; no translated locales are
  claimed.

## First production release notes

> First release: on-device SMS spending tracker with HDFC, ICICI, SBI and generic
> UPI parsing, end-of-day/end-of-month insights, a home-screen widget, manual
> entry, notes, merchant category rules and CSV export. No account, no cloud, no
> ads, no internet permission.

## Assets still to produce (Play Console)

- App icon: 512×512 PNG, no transparency, under 1 MB.
- Feature graphic: 1024×500 PNG/JPEG.
- Phone screenshots: 2–8, showing Home, Activity, Insights, transaction detail,
  Alerts and the first-run SMS disclosure. Use real or clearly fictional data.
- Optional: 7"/10" tablet screenshots, promo video.
