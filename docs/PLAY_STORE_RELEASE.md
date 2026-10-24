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
- [ ] **P1 — Remove the unused `INTERNET` permission** until email ingestion
      ships (it contradicts the minimal-permissions claim).
- [ ] **P1 — Stop importing every SMS.** `SmsReader.backfill()` reads the whole
      inbox and `TransactionRepository.insertUnrecognized` stores every
      non-transaction body verbatim in `unrecognized_messages`. The Alerts screen
      (`UnrecognizedScreen`) shows those bodies unmasked.

---

## 1. Decide scope and positioning

- [ ] Confirm v1 is **SMS-only**. `GmailClient` is a stub returning `emptyList`;
      `docs/ROADMAP.md` currently marks email ingestion **P0 "must-have"**.
      Either implement it or demote it to post-v1 and say so in the roadmap.
- [ ] Confirm the supported rule set is HDFC, ICICI, SBI and generic UPI
      (`parsing/rules/`). Do not imply broader bank coverage in the store listing.
- [ ] Do **not** claim "UPI notifications" unless `NotificationListenerService` is
      added — the current path reads SMS only. Update `README.md` and
      `docs/USER_GUIDE.md` accordingly.
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
      151 debug / 121 release tests, 0 failures; `app-release.aab` built with
      `versionCode=1100`, `versionName=0.11.0`.
- [ ] Add `./gradlew lint` (or `lintRelease`) to CI and resolve fatal issues.
      Fatal issues resolved 2026-10-24 (`./gradlew lint` is green); adding the
      step to CI remains.
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

- [ ] **Only import transaction-shaped SMS.** Add a sender/content pre-filter in
      `SmsReader`/`SmsProcessor` so unrelated personal messages are never copied
      into `unrecognized_messages`.
- [ ] If non-transaction messages must be retained for the Alerts queue, store the
      minimum needed (sender, timestamp, a hash/reference) rather than the full
      body, and cap length (`Privacy.capBody` already exists for email).
- [ ] Mask sensitive tokens in the Alerts list. `UnrecognizedScreen` currently
      renders `message.body` raw; apply `Privacy.maskSensitive` there as
      `OriginalMessageCard` already does.
- [ ] Make the scan **incremental**: track the highest processed SMS `_ID` and
      resume from there instead of re-reading the whole inbox on every launch and
      resume.
- [ ] Remove the automatic full rescan from `MainActivity.onResume`; keep the
      manual Sync action and run the initial import once.
- [ ] Replace the fire-and-forget coroutine in `SmsReceiver` with
      `goAsync()` or enqueue the work in WorkManager so an insert cannot be lost
      when the process is killed.
- [ ] Confirm `NoRawBodyLoggingTest` still passes and that no new logging of
      message content was introduced.
- [ ] Confirm the database remains excluded from cloud backup and device transfer
      (`backup_rules.xml`, `data_extraction_rules.xml`).
- [ ] Confirm export contains parsed fields only, never raw message content
      (`TransactionCsv`).

---

## 5. Functional correctness before launch

- [ ] **EOD time changes take effect.** `SettingsViewModel.setEodTime` only writes
      DataStore; `ReportScheduler.scheduleEod` runs only in
      `SpendTrackerApp.onCreate`. Reschedule `EodReportWorker` when the time or
      notification toggle changes.
- [ ] **Missed-run reconciliation.** `lastEodDate` / `lastEomMonth` are written by
      the workers but never read. Implement the reconcile-on-start behavior
      promised in `AGENTS.md` §8 (and fix the doc if the behavior is dropped).
- [ ] **EOM reliability.** `EomReportWorker` only posts when WorkManager happens to
      run on the 1st (`dayOfMonth != 1` returns success). Make a delayed run still
      report the intended prior month, and avoid duplicate posts.
- [ ] Verify Doze, reboot, force-stop and battery-saver behavior for the daily and
      monthly notifications.
- [ ] Fix notification formatting to use the shared INR formatter
      (`Formatters.formatInr`); both workers currently use
      `"₹%.2f".format(Locale.US)`, which bypasses Indian digit grouping.
- [ ] Replace `R.drawable.ic_launcher_foreground` as the notification small icon
      with a proper monochrome notification icon.
- [ ] Add the missing **SBI rule unit test** — `SbiBankRule` has no test, which
      violates `AGENTS.md` §7/§12 and the project's definition of done.
- [ ] Review parsing coverage against real sample messages; each rule ships with
      anonymized samples and a test.
- [ ] Verify manual entry, notes, rename, merchant categories, export, widget,
      search and filters on a release build (not just debug).
- [ ] Verify Room migrations from the last published schema survive an in-place
      upgrade with user data intact.

---

## 6. Testing and QA

- [ ] Unit tests green for both variants:
      `./gradlew test --no-daemon` (147 debug / 121 release tests as of 2026-10-20).
- [ ] Add/keep tests for any behavior changed by this checklist (filtering,
      incremental scan, rescheduling, disclosure consent).
- [ ] Run through `docs/USER_GUIDE.md` end to end on a **clean install** and
      correct every statement that does not match the app (for example the guide
      currently promises "plain-language reasons" at the permission prompt and
      pull-to-refresh in Home).
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

- [ ] App name (max 30 chars): `Spend Tracker`.
- [ ] Short description (max 80 chars) and full description (max 4000 chars) that
      match actual behavior: SMS-based, on-device, supported banks, no account,
      no cloud. Mention that SMS access is required for automatic tracking and
      that manual entry works without it.
- [ ] App icon: 512×512 PNG, no transparency, under 1 MB.
- [ ] Feature graphic: 1024×500 PNG/JPEG.
- [ ] Phone screenshots: 2–8, showing Home, Activity, Insights, transaction
      detail, Alerts and the permission disclosure. Use real data or clearly
      fictional data; no misleading UI.
- [ ] Optional: 7" and 10" tablet screenshots if tablet support is claimed.
- [ ] Optional: promo video (YouTube).
- [ ] Category (Finance or Productivity) and tags.
- [ ] Contact email, website/landing page (optional), privacy policy URL.
- [ ] Default language and supported locales.
- [ ] Release notes for the first production release.

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
