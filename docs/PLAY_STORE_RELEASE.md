# Spend Tracker — Google Play Release Checklist

Everything required to publish Spend Tracker to Google Play, in the order it has
to happen. Check items off as they land; do not submit until every **P0** item is
green.

Verified against the codebase on **2026-10-24**. Play policy deadlines quoted
here are current as of that date — re-check the linked policy pages before
submitting.

---

## 0. Release gate (current status)

**Verdict: not submittable yet.** The in-repo P0 blockers are fixed; the
remaining P0s are Play Console / hosting actions that cannot be done from the
repository.

- [x] **P0 — Target API 36.** `app/build.gradle.kts` now sets
      `compileSdk = 36` and `targetSdk = 36` (SDK platform 36 installed). Still
      required: run the Android 16 behavior-change pass on an API 36 device
      (§2).
- [x] **P0 — Ship an AAB.** CI now runs `bundleRelease` and attaches
      `app/build/outputs/bundle/release/*.aab` to the GitHub Release (the APK is
      kept alongside it for sideloading).
- [x] **P0 — Privacy policy written and linked in-app.** The policy lives at
      [`privacy-policy.md`](./privacy-policy.md) and is linked from
      **Settings › About › Privacy policy** to
      `https://pkdiv.com/SpendTracker/privacy-policy` (the `privacy_policy_url`
      string). **Remaining manual step:** deploy the rendered policy at that
      path and confirm the URL loads.
- [x] **P0 — Prominent disclosure + consent before the SMS prompt.** A first-run
      `DisclosureScreen` (`ui/disclosure/`) is rendered before any runtime
      permission dialog. SMS is requested only from its affirmative action;
      "Continue without SMS" leaves the app usable. The notification rationale
      is shown once before `POST_NOTIFICATIONS`. Consent is persisted in
      `SettingsStore`.
- [ ] **P0 — Permissions Declaration Form + demo video** for SMS access, with the
      "SMS-based money management" use case. Play Console only — see §3.3. The
      in-app disclosure and demo steps are now in place for the recording.
- [x] **P0 — Unique `versionCode` per upload.** `versionCode`/`versionName` are
      derived from the `vX.Y.Z` tag (or `-PversionName=X.Y.Z`):
      `versionCode = X*10000 + Y*100 + Z`, `versionName = X.Y.Z`. The build fails
      when the version can't be parsed. The release build no longer falls back to
      the debug keystore.
- [x] **P1 — Remove the unused `INTERNET` permission.** Removed from
      `AndroidManifest.xml` (verified absent from the merged manifest). The app
      declares SMS + notifications directly; WorkManager merges in its standard
      `WAKE_LOCK` / `RECEIVE_BOOT_COMPLETED` / `FOREGROUND_SERVICE` /
      `ACCESS_NETWORK_STATE` permissions for scheduling only — no network access.
      Re-add `INTERNET` only when email ingestion ships.
- [x] **P1 — Stop importing every SMS.** `SmsFilter` runs before parsing and only
      lets transaction-shaped messages through (amount + transaction signal, OTP
      messages rejected), so personal SMS never reach `unrecognized_messages`.
      Bodies are capped at 64 KB (`Privacy.capBody`), the Alerts list applies
      `Privacy.maskSensitive`, the inbox scan is incremental (`last_sms_id`
      high-water mark, no resume rescan), and `SmsReceiver` finishes its insert
      via `goAsync()`.

---

## 1. Decide scope and positioning

- [x] Confirm v1 is **SMS-only**. Email ingestion is demoted to P3/post-v1 in
      `docs/ROADMAP.md`; `GmailClient` remains a stub.
- [x] Confirm the supported rule set is HDFC, ICICI, SBI and generic UPI
      (`parsing/rules/`). The store listing must not imply broader coverage.
- [x] Do **not** claim "UPI notifications" — `README.md` now says "SMS
      transaction alerts (including UPI messages sent by your bank)", which is
      the actual path. No `NotificationListenerService` is added.
- [x] Define the public version scheme. Tags are the source of truth
      (`vX.Y.Z`), with `versionCode = X*10000 + Y*100 + Z` and
      `versionName = X.Y.Z` injected at build time by `app/build.gradle.kts`.
      CI validates the tag (or the `workflow_dispatch` version input) and passes
      it as `-PversionName`.
- [ ] Confirm `applicationId = dev.pkdiv.spendtracker` is final; it is immutable
      on Play after the first upload.

---

## 2. Build and signing

- [x] Bump `compileSdk` and `targetSdk` to **36** in `app/build.gradle.kts`;
      keep `minSdk = 26`. AGP 8.13.2 builds against the installed
      `platforms;android-36`.
- [ ] Address Android 16 behavior changes (edge-to-edge enforcement, predictive
      back, notification/permission changes) and test on an API 36 device or
      emulator. Edge-to-edge is already enabled in `MainActivity`; predictive
      back uses Navigation Compose defaults.
- [x] Replace `assembleRelease` with `bundleRelease` in CI
      (`.github/workflows/build-and-publish.yml`) and attach
      `app/build/outputs/bundle/release/*.aab` (the APK is still attached too,
      for sideloading).
- [x] Derive `versionCode`/`versionName` from the pushed tag (`v*`) or an
      explicit `-PversionName`; the build fails if the version cannot be parsed.
- [x] Keep `KEYSTORE_PATH`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` in CI
      secrets only; never commit the keystore.
- [x] Remove the **debug-keystore fallback** for release builds in
      `app/build.gradle.kts`. Without `KEYSTORE_PATH` a release build is now
      unsigned rather than debug-signed.
- [ ] Enroll in **Play App Signing** on first upload; store the upload key and
      credentials securely. The upload key is not the app signing key.
- [x] Run `./gradlew test bundleRelease --no-daemon` locally with JDK 21 and
      confirm `BUILD SUCCESSFUL` plus zero failures in
      `app/build/test-results/testDebugUnitTest/*.xml`. Verified 2026-10-24:
      167 debug / 136 release tests, 0 failures; `app-release.aab` built with
      `versionCode=1100`, `versionName=0.11.0`.
- [x] Add `./gradlew lint` (or `lintRelease`) to CI and resolve fatal issues.
      CI now runs `lint` alongside `test`/`bundleRelease`, and `./gradlew lint`
      is green (2026-10-24).
- [ ] Optional: enable R8 (`isMinifyEnabled = true`) with keep rules; currently
      release builds are unminified.
- [x] Verify the release AAB contains no debug-only components, no test sources,
      and `android:allowBackup` plus the backup exclusion rules are as intended
      (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`).
      Verified on the release APK: `allowBackup=true` with both
      `fullBackupContent` and `dataExtractionRules` wired, and no `debuggable`
      flag. (The AAB's only `*test*` entries are AndroidX library resources.)

---

## 3. Privacy, permissions and policy compliance

### 3.1 Privacy policy

- [x] Write the policy — [`privacy-policy.md`](./privacy-policy.md) — and
      publish it at the public URL `https://pkdiv.com/SpendTracker/privacy-policy`
      (Play requires a URL; the app links to the same URL). Publishing the
      rendered page at that path is a manual deploy step.
- [x] Cover, at minimum:
  - what is accessed (SMS messages, notification permission, device storage);
  - that parsing and storage are **on-device**, and no message content is
    transmitted off the device;
  - that the local database is excluded from Android cloud backup and device
    transfer;
  - that CSV export is user-initiated and goes only where the user sends it;
  - retention (data lives until the user clears app data or uninstalls);
  - how to delete data (uninstall / clear app data; no account exists);
  - third parties (none) and analytics/ads (none);
  - contact details (GitHub issues) and effective date.
- [ ] Link the policy in the Play listing **and** inside the app
      (Settings › About). In-app link: **done**. Play listing: manual.
- [x] Make the policy match the actual behavior — it explicitly discloses the
      on-device Alerts queue and the stored source-message text.

### 3.2 Prominent disclosure and consent

- [x] Add a first-run disclosure screen **before** the runtime permission dialog.
      `ui/disclosure/DisclosureScreen` states what data is accessed, why, and
      that it stays on-device, and requires the affirmative **Allow SMS access**
      action. **Continue without SMS** keeps the app usable.
- [x] Request `READ_SMS`/`RECEIVE_SMS` only after that consent, and only when the
      user is about to use SMS-based tracking (`MainActivity.requestSmsPermissions`
      is reachable only from the disclosure's consent action).
- [x] Ensure the app remains usable if SMS is denied (manual entry already works);
      never hard-block the app behind the permission.
- [x] Show a similar, contextual explanation before requesting
      `POST_NOTIFICATIONS` on Android 13+ (`NotificationRationaleDialog`,
      persisted as shown so it is not re-asked every launch).
- [x] Keep the disclosure text in `strings.xml` (`permission_*_rationale`,
      `disclosure_*`, `notification_rationale_*`).

### 3.3 Permissions Declaration Form

- [ ] In Play Console: **App content › Sensitive app permissions › SMS/Call Log**
      and declare SMS access.
- [ ] Select the closest permitted use case: **SMS-based money management**
      ("apps that track and manage budget"). SMS-based financial transactions is
      the alternative if reviewers prefer it.
- [ ] Explain precisely: transaction alerts are read on-device, parsed locally,
      never uploaded, and non-financial SMS are not used (fix §5.1 first).
- [ ] Record and upload the required **demo video** showing the core flow:
      permission disclosure → SMS import → parsed transaction → report.
- [ ] Submit and wait for approval before expecting the release to go live.
- [ ] Re-submit the declaration if how the app uses SMS changes.

### 3.4 Other Play declarations

- [ ] Complete **Data safety** honestly. If nothing is transmitted off-device,
      the app generally reports no data collected/shared; still declare SMS
      access accurately and account for user-initiated CSV export.
- [ ] Complete the **content rating** questionnaire (expected: everyone / low
      maturity; app has no ads, user-generated content, or purchases).
- [ ] Complete **target audience** and **ads** declarations (no ads).
- [ ] Confirm the **Financial services** policy does not apply (the app displays
      spending; it does not lend, broker, or move money).
- [ ] Confirm no other special declarations apply (news, government, health,
      gambling, crypto).
- [ ] Provide **app access** instructions only if a login were required — not
      applicable here.

---

## 4. Privacy and data-handling fixes (required before submission)

- [x] **Only import transaction-shaped SMS.** `SmsFilter.isTransactionShaped()`
      runs in `SmsProcessor` before parsing and requires an amount plus a
      transaction signal, rejecting OTP/verification messages. Unrelated personal
      messages return `ParseResult.Ignored` and are never stored.
- [x] Cap retained bodies at 64 KB (`Privacy.capBody`) for both transactions and
      Alerts; the Alerts queue still keeps sender/body/timestamp to remain
      reviewable, and the pre-filter means only financial messages reach it.
- [x] Mask sensitive tokens in the Alerts list — `UnrecognizedScreen` now renders
      `Privacy.maskSensitive(message.body)`, matching `OriginalMessageCard`.
- [x] Make the scan **incremental**: `SmsReader` persists the highest processed
      SMS `_ID` (`SettingsStore.last_sms_id`) and queries only `_ID > lastSmsId`
      in ascending order, advancing a forward-only high-water mark.
- [x] Removed the automatic full rescan from `MainActivity`; the initial import
      runs once and the manual Sync action uses the same incremental scan.
- [x] `SmsReceiver` now uses `goAsync()` and finishes the insert in a coroutine,
      so the process can't drop it mid-processing.
- [x] `NoRawBodyLoggingTest` passes; no new logging of message content.
- [x] Database remains excluded from cloud backup and device transfer (release
      manifest verified: `allowBackup=true` with both rules wired).
- [x] Export contains parsed fields only (`TransactionCsvTest` asserts raw message
      content is never included).

---

## 5. Functional correctness before launch

- [x] **EOD time changes take effect.** `SettingsViewModel.setEodTime` and the
      notification toggle both call `ReportScheduling.scheduleEod()` after writing
      the setting (covered by `SettingsViewModelTest`).
- [x] **Missed-run reconciliation.** `ReportScheduler.catchUpMissedReports()` runs
      on app start and enqueues one-shot EOD/EOM catch-up work; the workers read
      `lastEodDate` / `lastEomMonth` and skip duplicates.
- [x] **EOM reliability.** The `dayOfMonth != 1` early return is gone;
      `EomReportWorker` reports the prior month once (guarded by `lastEomMonth`),
      so a delayed run still reports the intended month.
- [ ] Verify Doze, reboot, force-stop and battery-saver behavior for the daily and
      monthly notifications (on a physical device).
- [x] Notification formatting uses `Formatters.formatInr` (Indian digit grouping)
      in both workers.
- [x] Notification small icon is the monochrome `res/drawable/ic_notification.xml`
      (no longer the launcher foreground).
- [x] Added `SbiBankRuleTest` (debit, credit, OTP) — it also caught and fixed a
      case-sensitivity bug in the SBI account regex (`A/c`).
- [ ] Review parsing coverage against real sample messages; each rule ships with
      anonymized samples and a test.
- [ ] Verify manual entry, notes, rename, merchant categories, export, widget,
      search and filters on a release build (not just debug).
- [x] Room migration survival tests pass (`Migration5To6Test` … `Migration8To9Test`);
      an in-place upgrade from the last published build remains part of device QA.

---

## 6. Testing and QA

- [x] Unit tests green for both variants: `./gradlew test --no-daemon`
      (167 debug / 136 release, 0 failures; re-verified 2026-10-24).
- [x] Added tests for the changed behavior: `SmsFilterTest`,
      `SmsScanWindowTest`, `SbiBankRuleTest`, `ReportSchedulerTest`, Alerts OTP
      masking (`UnrecognizedContentTest`), disclosure consent
      (`DisclosureScreenTest`), and EOD rescheduling (`SettingsViewModelTest`).
- [ ] Run through `docs/USER_GUIDE.md` end to end on a **clean install** and
      correct every statement that does not match the app.
- [ ] Test on a physical device matrix: API 26, 29/30, 33, 35, 36; at least one
      OEM skin (Samsung/Xiaomi) for SMS permission and notification behavior.
- [ ] Large-inbox performance: import 10k+ messages and confirm the first scan,
      subsequent resumes and Alerts tab remain responsive.
- [ ] Dual-SIM and multi-sender SMS; long/multipart SMS; message deleted from the
      inbox after import.
- [ ] Permission revoked mid-use; notification permission denied; app restarted.
- [ ] Accessibility: TalkBack, 200% font scale, 48dp touch targets, contrast,
      charts' `contentDescription`.
- [ ] Light/dark theme and widget resize/dark mode.
- [ ] Run Play Console **pre-launch report** on real devices; triage every crash,
      ANR and accessibility warning.
- [ ] Verify the release AAB installs from the internal testing track before
      promoting.

---

## 7. Store listing assets and copy

Text copy is drafted in [`STORE_LISTING.md`](./STORE_LISTING.md).

- [x] App name (max 30 chars): `Spend Tracker`.
- [x] Short description (max 80 chars) and full description (max 4000 chars) that
      match actual behavior: SMS-based, on-device, supported banks, no account,
      no cloud, SMS optional for manual use. Drafted in `STORE_LISTING.md`.
- [ ] App icon: 512×512 PNG, no transparency, under 1 MB.
- [ ] Feature graphic: 1024×500 PNG/JPEG.
- [ ] Phone screenshots: 2–8, showing Home, Activity, Insights, transaction
      detail, Alerts and the permission disclosure. Use real data or clearly
      fictional data; no misleading UI.
- [ ] Optional: 7" and 10" tablet screenshots if tablet support is claimed.
- [ ] Optional: promo video (YouTube).
- [x] Category (Finance) and tags — proposed in `STORE_LISTING.md`.
- [ ] Contact email, website/landing page (optional), privacy policy URL. The
      privacy URL and optional website are set; the **support email still needs a
      monitored address**.
- [x] Default language (English/India) and supported locales (default strings only).
- [x] Release notes for the first production release — drafted in `STORE_LISTING.md`.

---

## 8. Play Console setup and release tracks

- [ ] Create/verify the Play Console developer account and complete identity
      verification (personal accounts need ID verification; one-time fee applies).
- [ ] Create the app: name, default language, app/game, free/paid, declarations.
- [ ] Enroll in Play App Signing and upload the first AAB to **Internal testing**.
- [ ] Add internal testers, install from the track, and verify the signed build.
- [ ] **Closed testing requirement (personal accounts created after 13 Nov 2023):**
      at least **12 testers opted in continuously for 14 days** on physical
      devices before production access is granted. Track real usage — Play checks
      engagement, not just opt-ins.
- [ ] Promote to **Closed testing**, gather feedback, fix blockers, repeat until
      stable.
- [ ] Apply for **Production access** and complete the questionnaire honestly
      (describe the closed test, testers, feedback and fixes).
- [ ] Set up **staged rollout** (e.g. 10% → 50% → 100%) with explicit halt
      criteria: crash rate, ANR rate, permission-denial reports, parse failures.
- [ ] Select target countries and device categories.
- [ ] Confirm pre-launch report and policy status are clean before rollout.

---

## 9. Launch day

- [ ] Tag the exact commit (`vX.Y.Z`), let CI build the signed AAB from that tag,
      and verify the artifact's `versionCode`/`versionName`.
- [ ] Upload the AAB to production (or promote the tested artifact; do not rebuild
      a different artifact for production).
- [ ] Re-check the store listing, data safety, content rating, privacy policy URL
      and SMS declaration are all approved.
- [ ] Confirm the closed-test build and the production build use the same signing
      lineage.
- [ ] Start a staged rollout and watch Play Console vitals and reviews.
- [ ] Halt the rollout if crash/ANR rates exceed the agreed thresholds; fix and
      re-release with a higher versionCode.
- [ ] Attach the release APK (for sideloading) to the GitHub Release if desired —
      Play itself takes the AAB.

---

## 10. Post-launch

- [ ] Monitor Android vitals, ratings and reviews; respond to crash/permission
      reports.
- [ ] Watch the SMS permission declaration status — a policy change or usage
      change requires re-declaration.
- [ ] Keep `targetSdk` current for the annual Play deadline (API 36 now; expect
      API 37 next cycle).
- [ ] Keep `README.md`, `docs/USER_GUIDE.md`, `docs/DEVELOPMENT.md`, `AGENTS.md`
      §2 and `docs/ROADMAP.md` in sync with what actually shipped.
- [ ] Maintain a versionCode ladder; never reuse or decrease a versionCode.
- [ ] Keep the keystore, upload key and Play Console access backed up and
      access-controlled.

---

## Appendix — references

- [Use of SMS or call log permission groups](https://support.google.com/googleplay/android-developer/answer/10208820)
  — SMS-based money management is a permitted use case, subject to review.
- [Declare permissions for your app](https://support.google.com/googleplay/android-developer/answer/9214102)
- [Target API level requirements](https://developer.android.com/google/play/requirements/target-sdk)
  — API 36 from 31 Aug 2026, extension to 1 Nov 2026.
- [App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
  — 12 testers for 14 continuous days.
- [Data safety](https://developer.android.com/privacy-and-security/declare-data-use)
- [Google Play Developer Policy Center](https://play.google/developer-content-policy/)
