# Spend Tracker — Play Console Declarations (prepared answers)

Copy-paste content for the Play Console **App content** declarations: the SMS
Permissions Declaration Form (the P0) plus Data safety, content rating, target
audience, ads and the remaining confirmations. Transcribe them into Play Console
and record the demo video from the script below.

These answers are prepared from the code as of the last review and must be
re-checked against final behavior before submission.

## SMS Permissions Declaration Form (P0)

**Path:** Play Console › App content › Sensitive app permissions › SMS/Call Log →
declare SMS access.

**Use case to select:** **SMS-based money management** ("apps that track and
manage budget"). The alternative, "SMS-based financial transactions", is
acceptable if reviewers prefer it.

### Declaration text (paste / paraphrase)

> Spend Tracker reads incoming SMS on the user's device to detect bank and UPI
> transaction alerts (debits, credits and UPI payments) and turn them into a
> personal spending log. All parsing and storage happen entirely on the device:
> message content is never transmitted off the device (the app has no INTERNET
> permission), and no third party, analytics service or ad network receives it.
> Only transaction-shaped messages are processed — the app requires an amount
> plus a transaction signal and rejects OTP/verification messages — so
> non-financial personal SMS are never imported or stored. Users can review and
> dismiss any message the app could not parse, and can delete all data by
> clearing app data or uninstalling. SMS access is optional: the app stays fully
> usable (manual entry, history, reports) if the user declines.

Key points reviewers check:

- Read on-device, parsed locally, never uploaded.
- Non-financial SMS are not used (an amount plus a transaction signal is
  required; OTP/verification messages are rejected).
- SMS access is optional and the app works without it.
- A data-deletion path exists (clear app data / uninstall).

### Required demo video

Play requires a video of the core flow: **permission disclosure → SMS import →
parsed transaction → report**. Keep it ~30–60s, on a device with test SMS.

Shot list:

1. First launch on a fresh install → the **disclosure screen** (what is read,
   why, and that it stays on-device); show **Allow SMS access** and **Continue
   without SMS**.
2. Tap **Allow SMS access** → the Android runtime prompt → grant.
3. The app imports SMS → **Home/Activity** lists parsed transactions (amount,
   merchant, category, date).
4. Open a **transaction detail** → the parsed fields plus the original-message
   reference (masked).
5. **Insights** → the end-of-day / end-of-month report (totals, category
   breakdown, daily chart).
6. Optional: **Alerts**, showing a dismissed message and OTP masking.

Capture notes:

- Use clearly fictional or anonymized data; never show real account numbers.
- Narrate or caption the on-device / never-uploaded claim.
- Upload unlisted (e.g. YouTube) and paste the link into the declaration.

## Data safety

The app reads transaction-shaped SMS, parses it **on-device**, and never
transmits anything over a network. There is no account, no analytics SDK, no
ad SDK, and the `INTERNET` permission is absent from the manifest.

| Play data type | Collected | Shared | Notes |
| --- | --- | --- | --- |
| Messages (SMS/MMS) | Yes | No | Read on-device to parse transaction alerts. Never uploaded. |
| Financial info | Yes | No | Parsed amount/merchant/category stored locally in the Room DB. |
| Photos / videos | No | No | Receipts store only a content URI; the image is never copied into the DB. |
| Personal info | No | No | No name, email, account, or identifiers. |
| Location | No | No | Not accessed. |
| Device or other identifiers | No | No | No advertising ID or device identifiers. |

- **Data shared:** None (no third parties).
- **Data encrypted in transit:** Not applicable — the app makes no network calls.
- **Data deletion:** Uninstall or clear app data (no account exists).

## Content rating

- **Rating:** Everyone (low maturity).
- Violence, sexuality, language, drugs, gambling, alcohol, tobacco: **None**.
- No ads, no user-generated content, no purchases, no location-based content.

## Target audience and content

- **Target age:** 18 and older (a personal finance tool; no child-directed content).
- No restricted content; the app does not collect or process children's data.

## Ads

- **Contains ads:** No.

## Financial services

- **Not applicable.** Spend Tracker displays a user's spending; it does not
  lend, broker, transfer, or move money, and is not a bank, wallet, or payment
  service.

## Other special declarations

- **None apply** — not news, government, health/medical, gambling, crypto,
  dating, or child-directed.

## App access

- **Not applicable.** No login is required; every feature is usable without an
  account. SMS access is optional (manual entry remains available without it).
