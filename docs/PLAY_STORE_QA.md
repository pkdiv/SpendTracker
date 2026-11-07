# Spend Tracker — Device QA scripts

Runnable pre-launch checks for the device passes in
[`PLAY_STORE_RELEASE.md`](./PLAY_STORE_RELEASE.md) §5–§6. Run each on a physical
device with a **release** build and record pass/fail plus evidence. Items still
requiring a device or Play Console are the only reason those checklist rows stay
unchecked.

## Setup

- Build with JDK 21: `JAVA_HOME=<jdk21> ./gradlew test bundleRelease --no-daemon`.
- Install the tagged AAB through an **internal testing** track, or the release APK
  with `adb install app/build/outputs/apk/release/app-release.apk`.
- Seed test SMS on an emulator with `adb emu sms send <number> "<text>"`, or a
  spare SIM. Never use real account numbers.

## 1. Notification reliability — Doze / reboot / force-stop / battery saver (§5)

Goal: the EOD (daily) and EOM (monthly) notifications fire exactly once per
period under power management.

1. Settings → set the EOD time ~2 minutes ahead; lock the screen.
2. Doze: `adb shell dumpsys deviceidle force-idle`; confirm the EOD notification
   posts near the set time.
3. Reboot; confirm the reports are rescheduled and an EOD that came due around
   boot still posts.
4. `adb shell am force-stop dev.pkdiv.spendtracker`; reopen after the EOD time
   has passed; confirm `ReportScheduler.catchUpMissedReports()` posts exactly one
   catch-up.
5. Turn on battery saver and repeat step 2.

Expected: one EOD per day and one EOM per month, no duplicates (`lastEodDate` /
`lastEomMonth`), missed runs caught up on next start, tapping the notification
opens the app.

## 2. Release-build feature smoke test (§5)

Exercise every feature on the release build and compare against
[`USER_GUIDE.md`](./USER_GUIDE.md):

- Manual entry (Add FAB) → saved with source "Manual"; detail omits the
  source-message rows.
- Notes (add / edit / clear) and Rename merchant (a rename also teaches future
  categorization).
- Settings → Merchant categories add / edit / delete.
- Receipt attach / remove on the detail screen (SAF picker, thumbnail, and the
  URI grant survives a restart).
- Export CSV (This month / Last month / All time) → share sheet; file contains
  parsed fields only.
- Home-screen widget: month-to-date spend; refreshes after a parsed SMS and a
  manual entry.
- Search & filters: merchant/amount text, category, direction, date range,
  amount range, hide duplicates.
- Insights: totals, daily-spend chart, category donut, top merchants,
  month-over-month comparison, unusually-large, recurring spend.

## 3. Clean-install USER_GUIDE walkthrough (§6)

Uninstall → reinstall → follow [`USER_GUIDE.md`](./USER_GUIDE.md) end to end.
Correct any statement that does not match the app.

## 4. Device matrix (§6)

API 26, 29/30, 33, 35 and 36, plus at least one OEM skin (Samsung / Xiaomi). Per
device check: SMS permission prompt and disclosure, `POST_NOTIFICATIONS` (33+),
edge-to-edge insets (35+), widget, and notification delivery.

## 5. Large inbox (§6)

Import 10k+ SMS: the first scan completes, subsequent resumes are incremental
(no full rescan), and Activity / Alerts stay responsive.

## 6. SMS edge cases (§6)

Dual-SIM and multiple senders; long / multipart SMS (concatenated); and a message
deleted from the inbox after import.

## 7. Permission lifecycle (§6)

Revoke SMS mid-use (app stays usable), deny `POST_NOTIFICATIONS`, restart the
app, and re-grant from Settings › Permissions.

## 8. Accessibility (§6)

TalkBack (rows merged, charts expose `contentDescription`), 200% font scale (no
clipping), ≥48dp touch targets, and contrast.

## 9. Theme and widget (§6)

Light / dark themes and widget resize / dark mode.

## 10. Play Console checks (§6)

- Run the **pre-launch report** on real devices; triage every crash, ANR and
  accessibility warning.
- Install the AAB from the **internal testing** track and confirm the signed
  build installs before promoting.
