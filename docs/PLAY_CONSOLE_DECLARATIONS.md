# Spend Tracker — Play Console Declarations (prepared answers)

Copy-paste answers for the non-SMS Play Console declarations. Transcribe them
into **Play Console › App content**. The **SMS/Call Log Permissions Declaration
Form + demo video** is a separate P0 tracked in
[`PLAY_STORE_RELEASE.md`](./PLAY_STORE_RELEASE.md) §3.3 and is not covered here.

These answers are prepared from the code as of the last review and must be
re-checked against final behavior before submission.

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
