# Spend Tracker — Privacy Policy

**Effective date:** 24 October 2026
**Applies to:** the Spend Tracker Android app (`dev.pkdiv.spendtracker`)

Spend Tracker is a local-first app. It reads transaction messages on your device,
parses them on your device, and stores the results on your device. The app has no
server, no user account, and no cloud sync.

## 1. What the app accesses

| Access | Why | Where it goes |
| --- | --- | --- |
| **SMS messages** (`READ_SMS`, `RECEIVE_SMS`) | To find bank, card and UPI transaction alerts and extract the amount, direction, merchant, account/card last-4 and date. | Read and parsed **on-device only**. Never uploaded. |
| **Notifications** (`POST_NOTIFICATIONS`) | To show your end-of-day and end-of-month spend summaries. | Generated **on-device only**. |
| **Internet** (`INTERNET`) | Declared in the app manifest but **not used by any current feature**. It is reserved for a future, opt-in email import. No data is transmitted today. | Nothing is sent. |
| **Device storage** | To keep your transactions, settings and merchant→category rules in the app's private storage. | Stays on the device. |

SMS access and notification permission are requested only after an in-app
explanation, and the app remains usable without them (you can add transactions by
hand and view your reports).

## 2. How messages are processed

Parsing is **rule-based and on-device**. No message content is sent to any LLM,
cloud API, analytics service, or third party.

## 3. What is stored on your device

- **Parsed transactions** — amount, money-in/money-out direction, merchant,
  account/card last-4, category, date, an optional note, and a reference back to
  the source message. To keep every transaction traceable to its source, the app
  also stores the sender and text of the message it parsed. On a transaction's
  detail screen, OTP-style codes are masked by default (tap to reveal).
- **Alerts queue** — if a message cannot be parsed as a transaction, the app may
  keep it in an in-app Alerts tab so you can review or dismiss it. This queue is
  stored on the device only and is never transmitted.
- **Merchant→category rules and settings** — the rules you teach the app and your
  preferences (report time, notification toggle).
- **Exported CSV files** — only when you explicitly export. They are written to
  the app's temporary cache and handed to the Android share sheet, and they
  contain parsed transaction fields only — never message text.

All of the above stays in the app's private storage on your device.

## 4. What never leaves your device

Spend Tracker does **not** transmit, sell or share:

- your message content, sender, or parsed transactions off your device;
- your name, email address, phone number, contacts, or location;
- device identifiers or advertising IDs;
- usage analytics, crash reports, or diagnostic telemetry.

There are no third-party SDKs, no analytics libraries and no ads in the app.

## 5. Backups and device transfer

The app's local database is explicitly **excluded** from Android's cloud backup
and from device-to-device transfer. As a result, your spending history does not
leave the device through backup, and it will not be restored automatically if you
move to a new phone.

## 6. Data sharing and export

The app never uploads your data. If you choose **Settings → Export**, a CSV of
parsed transactions is created on the device and the Android share sheet lets you
send it to an app or storage location you pick. You control that destination.

## 7. Retention and deletion

Data stays on your device until you remove it:

- Dismiss or **Clear all** in the Alerts tab to remove queued messages from view
  (they are kept as dismissed so a re-scan cannot resurface them).
- Clear the app's storage in Android settings (**Settings → Apps → Spend Tracker →
  Storage → Clear storage**) to delete everything the app has stored.
- Uninstalling the app deletes its private storage, including the database and
  settings.

There is no account to delete and no server-side copy to request.

## 8. Security

- The app makes no network connections for message processing.
- Data is stored in app-private storage that other apps cannot read.
- OTP and verification codes are masked in the UI by default.
- Raw message content is never written to logs.

## 9. Children

The app is a personal spending tracker and is not directed at children under 13.

## 10. Changes to this policy

If the app's data handling changes — for example, if opt-in email import ships
and begins using the `INTERNET` permission — this policy will be updated before
that feature is released, and the effective date above will change.

## 11. Contact

Questions about this policy or the app's data handling:
[open an issue at github.com/pkdiv/SpendTracker](https://github.com/pkdiv/SpendTracker/issues).

## Publishing this policy

Google Play requires a public URL. The policy is published at:

**<https://pkdiv.com/SpendTracker/privacy-policy>**

That URL is configured in `res/values/strings.xml` (`privacy_policy_url`) and is
the one to use in the Play listing. Deploy the rendered version of this document
at that path. If the URL ever changes, update `privacy_policy_url`, the Play
listing link, and this section together.

---

*This policy describes the app as shipped in this repository.*
