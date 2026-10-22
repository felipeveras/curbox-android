# Optional app redirect test fork

## Current delivery — brain counter only, versionCode 11

Supersedes artifact metadata below. The original Material UI from version 9 remains unchanged outside the short-video counter settings and floating counter. No BrainPal assets or code were used. The five pink cartoon brain silhouettes are original static Kotlin Canvas paths with eyes: healthy, softening, melting with drips, almost a puddle, puddle with eyes. The number sits below the brain, with opaque white text on dark plum. There is no continuous animation, new permission, dependency, Room migration, commit, push or issue write. All pre-existing dirty work was preserved.

### Display and counting rules

- Fixed stage thresholds: **0–2 / 3–5 / 6–9 / 10–14 / 15+**. The brain silhouette loses visible height and filled mass through the first four stages and becomes a small puddle at 15. These are playful feedback, not a medical assessment. Threshold editing is deliberately omitted to keep settings simple.
- **Session is the default**, including legacy JSON. “Count per day” / “Contar por dia” switches the display source only; neither source is erased by switching. A session ends at **300,000 ms** outside detected short videos, or without detected activity, including screen off. Switching supported apps inside the window keeps the same session. Staying on the same clip with no further accessibility activity for five minutes is deliberately treated as inactivity; this rule is explained in EN and pt-BR settings.
- The session spans midnight if still active. Daily display uses the existing local-calendar `ReelStatsDao` date keys, switches to the new date at the next event or heartbeat (normally within one second while awake), and reloads an existing date when timezone/clock changes return to it rather than overwriting it with zero. No fixed 24-hour arithmetic is used. Backward wall-clock corrections rebase session timestamps without subtracting from the count. A killed process is conservatively considered inactive since its last persisted observation.
- Session count/timestamps, last comparator and bounded existing per-app deduplication history (up to 50 entries per app) survive service/process recreation in private service runtime preferences. The mode and normalized position remain in the existing multiprocess settings DataStore. Daily analytics remain in the original Room table. No second detector or separate source of video counts was added: only the existing accepted progression calls increment both the session and daily totals and call the original usage callback once.
- Existing detector semantics remain: loading/empty comparator is not a new video; first observation establishes a baseline unless the app's detector has an initial comparator; substantial-change and app-specific deduplication rules still apply. Screen-off stale events no longer count unseen clips. These are detected transitions, not a claim of perfect video identification in every third-party app version.
- Fixed compact artwork/type replace the huge configurable text overlay. Legacy color/opacity/type-size JSON values are retained for compatibility, but their now-inapplicable controls are hidden only on this counter's settings screen. Existing checkpoint visibility settings and position-picker flow remain. Native settings controls continue to inherit the original Material theme.

### Overlay interaction and accessibility

The baseline was a plain pass-through number. The brain overlay has no close or dismiss control; settings, detector state, checkpoints, and screen-off continue to control visibility. Its drag surface remains the full brain-and-count group, consumes the whole gesture, clamps it to screen bounds, and persists normalized center coordinates on release. Cancelled gestures do not save, and underlying video controls do not receive the drag. Hiding uses GONE rather than transparent touch interception. Position preview uses the same real brain layout and retains the existing confirmation flow. The counter has a localized count/stage/drag description. Number contrast is fixed rather than user-configurable transparent text. TalkBack, large-font layout and OEM touch routing still require a phone check.

### Diagnosis and limitations

- A confirmed race existed between a queued main-thread overlay creation and a later unsupported-app event: the latter skipped removal while the manager was still unattached, then the stale creation became visible. The render callback now checks current `activeShortVideo` state before attaching. A paused-main-looper Robolectric test proves the stale overlay was visible before the fix and absent after it.
- `ReelScriptRunner.detect()` returns `null` both when the active root is unavailable and when there is no confirmed reel result (including unsupported packages or detector errors). `ReelsCountTracker` currently treats `null` for a supported app as leaving the reel and clears its comparator. This ambiguity remains a hypothesis for missed counts/flicker; the 0–3–6–10–15 UI change does not alter detector classification. Device event traces are needed before changing that behavior.
- The existing substantial-text and bounded deduplication rules, session/day mode, five-minute inactivity expiry, daily Room writes, usage callback, and settings persistence remain unchanged. Similar captions can still be indistinguishable when a detector exposes only shared UI text. A native Robolectric render is not phone/OEM touch-routing verification.

### Real verification and evidence

Evidence directory: `<LOCAL>/builds/curbox-brain-counter-v11-evidence/`.

- App-switch race RED/GREEN: `race-red.log`, `race-green.log`. The first demonstrates a real visible-overlay failure; the fix passes the same paused-looper test. Stage thresholds RED: `stages-red.log`. Removed-control RED: `noclose-red.log`. Stage-15 eye placement RED/GREEN: `eyes-red.log`, `render-green.log`.
- Targeted final counter/detector tests: `targeted-core-final.log` passed; the later native render assertions passed in `render-green.log`. Native light five-stage bitmaps and contact sheet are `native-fixture-light-stage-*.png` and `contact-sheet-light.png`; corresponding dark fixtures are also included. These are Robolectric native renders, not phone screenshots.
- Full suite: `full-suite.log` reports **229 tests, one failure, zero errors**. The sole failure is `ScriptLanguageTest.matchesRegexSupportsCommonFlags`: expected `true\ntrue\n` but got `false\nfalse\n` (`ComparisonFailure`, line 224). The historical archived-HEAD baseline recorded in the pre-existing evidence failed the same test with a `ScriptError`, a different exception; the current result is reported separately and no script-language code changed.
- ARM64 fullDebug build: `build.log`; final APK metadata and checksum are summarized in `<LOCAL>/builds/curbox-brain-counter-v11-evidence/report.md`.

- Behavioral TDD RED: `red-timeout.log` (session did not expire), `red-overlay.log` (brain view absent), `red-close-green-screen-off.log` (close absent), `red-screen-off.log` (2 callbacks instead of 1), `red-restart-dedup.log` (3 callbacks instead of 2 after restart). `red-state-api.log` is explicitly an API compile RED, not a behavioral assertion. Subsequent component/build logs retain both successful and intermediate failed attempts.
- Final targeted verification: **65 tests, zero failures/errors/skips**, plus successful `assembleFullDebug`, `assemblePlaystoreDebug`, `assembleFdroidDebug`. Log `targeted-build-delivery-final.log`, XML `targeted-results/`.
- Full suite: **227 tests, 1 failure, zero errors/skips**, sole failure `ScriptLanguageTest.matchesRegexSupportsCommonFlags` (ComparisonFailure), log `full-suite-delivery.log`, XML `full-results/`. Existing archived HEAD baseline rerun also fails that test with ScriptError, not the same exception (`baseline-script.log`). Independently compared all 533 tracked archive files against HEAD: no differences or missing files. Script sources were not changed.
- Coverage includes exact timeout and stage boundaries, restored session, cross-midnight session, backward clock, supported-app switching, duplicate comparator/restart dedup history, screen-off stale events, real daily Room writes/local-date reload, display-mode switching, real DataStore mode/position persistence, settings-fragment recreation in EN/light and pt-BR/dark, overlay drag/cancel/close/reopen, compact number and descriptions. Existing redirect/blocking/name/search/pt-BR regression tests also pass.
- **Actual Android native Robolectric Graphics renders**, not a web approximation or device capture: `native-fixture-{light,dark}-stage-{0..4}.png`. All ten were inspected via `native-fixture-contact-sheet.png`; distinct silhouettes, visible eyes, readable numbers, separate close control, no observed clipping at normal font size. These use fixture counts 0/10/25/50/100 and plain light/dark fixture backgrounds, not real app videos.
- An initial full-run fixture failure exposed Robolectric's Room binder callback with a null ComponentName, then a stalled looper while awaiting DataStore. Diagnostic logs/thread dump are retained. Only the fixture disables that irrelevant Room service binding and pumps the paused main looper. Final full run is unfiltered and completes; production multiprocess Room behavior was not disabled. Existing KSP/Kotlin and deprecation warnings remain. `git diff --check` passes.

Reproduce with the Java/SDK exports documented below:

```sh
./gradlew testFullDebugUnitTest --tests '*Brain*Test' --tests '*ReelCounterOverlayConfigTest' \
  --tests '*AutoRedirect*' --tests '*AppRedirect*' --tests '*WarningRedirectTest' \
  --tests '*RedirectSettingsTest' --tests '*BlockedAppsSearchTest' \
  assembleFullDebug assemblePlaystoreDebug assembleFdroidDebug \
  -PcurboxTestArm64=true --no-daemon --max-workers=2
./gradlew testFullDebugUnitTest -PcurboxTestArm64=true --no-daemon --max-workers=2
```

### Verified APK and phone checklist

- `<LOCAL>/builds/curbox-brain-counter.apk` — **14,073,586 bytes**; SHA256 **`7abfe06be2d342dd717ad4f9fd44384cafbc9fa52cf2f922d46eb743ba25f533`**.
- fullDebug, **ARM64 only**, `neth.iecal.curbox.redirect.debug`, versionCode **10**, versionName `6-full-redirect-debug`, minSdk 26, targetSdk 35, no testOnly marker. Permissions match the version-9 original-UI APK exactly.
- `apksigner verify --print-certs` passes, unchanged debug certificate SHA256 **`ae500a99a0e477a808098fa1ddddbbeb6d96ff73e28bb031754c825c8fb77c98`**. Final metadata/signature/hash log: `apk-verification-final.log`. Earlier metadata logs in the evidence directory describe superseded intermediate builds, not this delivery.
- `adb devices` is empty. **No physical-phone or emulator testing is claimed.**

Pending device checklist:

- [ ] Update the v9 install without uninstalling. Confirm saved groups, redirect opt-in, destination names/search, 5-second warning and original Material screens remain intact.
- [ ] Enable the video counter. Scroll distinct Shorts/Reels; confirm the number changes once per accepted transition and never once per repeated event. Switch supported apps within five minutes: same session. Rotate/restart service: count retained.
- [ ] Leave videos for 4:59 and return; session retained. Leave for at least 5:00, or turn screen off for five minutes; return to a fresh session. Repeat with process restart. No video should count while the screen is off.
- [ ] Toggle session/day, reopen settings, restart the app; mode retained and daily analytics unchanged. Check midnight, timezone change and backward clock behavior on a test device.
- [ ] Drag the brain near edges, cancel a drag, then close it; no close or underlying Like/Follow action should fire during drag. Re-enter short videos and verify the saved position. Verify portrait/landscape, display cutout and gesture-navigation areas on the POCO.
- [ ] Check light/dark backgrounds, font scale 1.3/2.0, TalkBack count/stage/close descriptions, checkpoint visibility, and all five silhouettes. Captions/app updates can affect the existing detector and must be checked with real versions.

Files changed for this feature only: `ReelsCountTracker.kt`, `ReelsOverlayManager.kt`, `ReelCounterOverlayConfig.kt`, `ReelCounterFragment.kt`, `fragment_reel_counter.xml`, new `BrainCounterState.kt`, `BrainView.kt`, `overlay_brain_counter.xml`, `brain_count_background.xml`, EN/pt-BR resources, debug versionCode in `app/build.gradle.kts`, five new `Brain*Test.kt` classes, and this report. Pre-existing unrelated dirty files and `ReelCounterViewModel.kt` changes were not reset. The reusable Android build skill was also updated with the proven Robolectric fixture pitfall; no other profile was modified.

---


## Current delivery — original Material UI restored, versionCode 9

This section supersedes **all palette/design and APK claims below**. Historical reports are retained as evidence, not the current specification. The rejected graphite/mint design has been removed, not expanded into more screens.

### Narrow rollback

- Deleted `values/redirect_theme.xml` and removed both ContextThemeWrapper uses: group editor and warning now inflate with their original Activity/Fragment context. The picker receives that original context too. No app-wide theme files were changed.
- Removed hardcoded redirect text/control colors, cancel-button transparency/tint overrides, special warning font sizing, the editor surface override and oversized/left-aligned custom destination control. Original Material DayNight styling is inherited again; destination settings use a standard outlined Material button, matching existing editor controls. Warning cancel returns to the original filled Material treatment. Native title uses Material TitleLarge.
- Preserved functional UI: readable destination name/icon and fallback, name/package searches and secondary package rows, pt-BR, opt-in automation, visible 5→4→3 countdown, custom/default-message separation, open-now and cancel/Home actions. Manual mode retains the ordinary unlock flow. Automatic mode still cannot unlock/cooldown.
- Existing dirty translations and unrelated modifications were not reset. No commit, push, issue mutation, new permissions or infrastructure changes.

### YouTube flash: investigation and proven bounded fix

Traced `AppBlockerService.onAccessibilityEvent` → synchronous `doAppBlockerCheck` for WINDOW_STATE_CHANGED and WINDOW_CONTENT_CHANGED → schedule/usage checks → `showWarningScreen` → `pressHome` → delayed Activity launch. The conflated worker channel is downstream of app blocking and is not responsible for this path's delay. `pressHome` records uptime and invokes GLOBAL_ACTION_HOME; the 1,000 ms gate is a throttle, not a deduplication/foreground check. `lastPackage` is cleared on block; the visibility flag only ignores events belonging to Curbox itself, not background YouTube events. Warning launches use NEW_TASK | CLEAR_TASK, which replaces a previous warning and its countdown.

Ranked hypotheses and evidence:

1. **Repeated background content event after the gate:** if YouTube supplies a content event while Curbox is already foreground, the old code presses Home again and starts a replacement warning. A new real-service Robolectric regression reproduces exactly this: first block, advance 1,100 ms, set active root to Curbox, replay underlying app content. RED produced a second WarningActivity instead of null (`curbox-rollback-red-loop.log`, 2 tests/1 failure). This is a proven code-path defect, **not proof that the phone's particular recording was caused by this defect**.
2. **First reactive frame/Home transition:** accessibility receives an event after the app/window has opened. Initial target frames can therefore already exist before Home runs. The existing 100 ms post is **after** Home and adds a Home→warning gap; deleting it cannot make this pre-launch interception. No guessed timing changes were made.
3. **Activity/lifecycle restart:** CLEAR_TASK on each repeated enforcement can visibly replace a warning and cancel/rearm its instance. The background-content fix prevents that demonstrated duplicate path. State-change events are retained because root can lag during genuine transitions. A blanket `isWarningScreenVisible` early return would weaken blocking if a user really opens YouTube again, so it was not introduced.
4. **Phone/OEM delivery, animation or root timing:** cannot be established without device logs. `adb devices` is empty. No claim of zero flash, repaired POCO animation, or end-to-end YouTube/Kindle test is made.

Minimal correction: only WINDOW_CONTENT_CHANGED events with a **known, different active-root package** are ignored. Null root falls through to existing enforcement; real foreground content and all window-state events keep existing checks. The regression additionally changes the active root back to the blocked app and proves it is still blocked. Existing 100 ms delay, 1,000 ms throttle, hidden-warning handling, optional Shizuku force-stop and permissions remain unchanged. This prevents the demonstrated avoidable warning-replacement loop; it does not guarantee removal of the initial reactive flash or all OEM event behavior.

### Verification and retained evidence

- Behavioral RED for original-palette requirements: `<LOCAL>/curbox-rollback-red-material.log`, 22 tests/3 failures (dialog palette and action tint still mint).
- Final targeted run: **44 tests, 0 failures/errors/skips**, plus **assembleFullDebug BUILD SUCCESSFUL in 2m 12s**. Log `<LOCAL>/curbox-rollback-green-build.log`; XML `<LOCAL>/curbox-rollback-targeted-test-results/`.
- Full run: **209 tests, 1 failure, 0 errors/skips**; only `ScriptLanguageTest.matchesRegexSupportsCommonFlags` (ComparisonFailure). Log `<LOCAL>/curbox-rollback-full-tests.log`; XML `<LOCAL>/curbox-rollback-full-test-results/`. Rerun in pristine existing git-archive HEAD baseline also fails (ScriptError, not identical exception): `<LOCAL>/curbox-rollback-baseline.log`. Script sources were untouched.
- `git diff --check` passes. Existing KSP/Kotlin mismatch, deprecated API and missing default `ascii_chillguy` resource warnings remain.
- Actual native Robolectric rendering, no infrastructure install: inspected `<LOCAL>/builds/curbox-original-ui-evidence/curbox-original-native-{auto,auto-dark,manual-light,manual-dark,custom-large}.png`. Original purple/light and lavender/dark Material controls are restored. Countdown/support text readable; all actions visible; custom large text wraps. Manual disabled Continue intentionally retains Material disabled treatment. `Message 1` in manual fixtures is upstream custom/default behavior, not a regression in automatic headings.
- Tests assert inherited original Activity palette for warning/editor/picker in light/dark, >=4.5 contrast on supporting copy and enabled redirect button, recreation, both manual/auto native renders, timer/lifecycle/cancel/no-cooldown/safety/name/icon/search/persistence. Renders are native Android with fixture app metadata, **not phone screenshots**.

```sh
export JAVA_HOME=<LOCAL>/builds/streak-toolchain/java
export ANDROID_HOME=<LOCAL>/builds/streak-toolchain/android
export ANDROID_SDK_ROOT="$ANDROID_HOME" PATH="$JAVA_HOME/bin:$PATH"
./gradlew testFullDebugUnitTest --tests '*AutoRedirect*' --tests '*AppRedirect*' \
  --tests '*WarningRedirectTest' --tests '*RedirectSettingsTest' \
  --tests '*BlockedAppsSearchTest' assembleFullDebug \
  -PcurboxTestArm64=true --no-daemon --max-workers=2
```

### Verified APK

- `<LOCAL>/builds/curbox-redirect-original-ui.apk` — **13,806,053 bytes**.
- SHA256 **`77bd83c3818e3adf8c4dc7a1608047ff58795cab52002b04da2e2e634c612850`**, identical to final fullDebug output.
- Package **`neth.iecal.curbox.redirect.debug`**, versionCode **9**, versionName `6-full-redirect-debug`; ARM64 only, minSdk 26, targetSdk 35, no testOnly marker.
- `apksigner verify --print-certs` passes; unchanged debug certificate SHA256 **`ae500a99a0e477a808098fa1ddddbbeb6d96ff73e28bb031754c825c8fb77c98`**. Not production signing.
- Verification log `<LOCAL>/curbox-rollback-apk-verification.log`.

Changed in this rollback only: `app/build.gradle.kts`, `AppBlocker.kt`, `WarningActivity.kt`, `CreateAppGroupFragment.kt`, `dialog_warning_overlay.xml`, `fragment_create_app_group.xml`, removed `redirect_theme.xml`, tests `AppRedirectServiceTest`, `AutoRedirectTest`, `RedirectSettingsTest`, and this report. All other pre-existing dirty work remains.

Remaining physical-phone check: update existing install, verify light/dark editor/picker/warning, explicitly save 5-second opt-in, try wait/open-now/cancel/background, and retry opening YouTube while warning is showing. Capture accessibility event type/package + active root + Home timestamp + warning lifecycle if flickering repeats. Do not interpret this JVM regression as proof that the initial phone flash is eliminated. Broader premium redesign remains out of scope.

---

## Video-feedback correction — current APK, versionCode 8

This section supersedes the versionCode 7 artifact below. Scope is the redirect warning correction, not the pending Opal/premium issue #1. All pre-existing dirty work remains; no reset, commit, push or issue changes were made.

### Proven causes and limits of the video

Inspected `<LOCAL>/.hermes/cache/scratch/curbox-video-detail.jpg`, the supplied contact sheet of the 3.76-second clip. It shows YouTube briefly before Home, then a dark warning with `Message 2`, almost invisible unlock-delay copy and `Continuar`, plus readable Kindle and Home actions. It does NOT establish a five-second timer failure: the warning appears only near the end and the stored opt-in on the phone is unknown.

1. **Runtime palette mismatch, reproduced:** the scoped overlay sets a dark dialog surface, but inherited Material TextAppearances/control ColorStateLists still use base DayNight text colors. Before correction, the actual `proceed_seconds.currentTextColor` was `#49454F` in light mode and `#CAC4D0` in dark mode, not the scoped `#B7C2BD`. Both regression cases went RED; the light ink explains near-invisible copy on the dark surface. Checking only theme tokens or the enabled redirect button had missed this. Fixed the concrete warning copy and enabled/disabled unlock-button text lists within redirect screens, and checked recreation. No broad app theme replacement.
2. **Mixed modes, reproduced:** automatic redirect still created the legacy unlock timer/UI, including visible Continue. The RED test saw VISIBLE (0) rather than GONE (8). Automatic redirect now bypasses unlock setup, hides/guards Continue, and cannot emit cooldown even through a programmatic click. Manual alternatives and ordinary warnings retain their existing unlock rules.
3. **Placeholder title, traced:** `AppBlockerWarningScreenConfig.message` defaults to `[Message 1, Message 2]`, and WarningActivity randomly selected one as the heading. Automatic mode now has its own `Vamos para o Kindle` heading. Only the exact untouched default list is suppressed; custom messages remain as readable supporting content. Manual-mode messages are unchanged.
4. **Opt-in:** `redirectAfter5Seconds` still defaults false; old data is intentionally manual. Persistence/defaults, editor save and service forwarding tests pass. There is no access to the phone's saved data, so its actual setting cannot be diagnosed from this video. In the group editor select Kindle, explicitly enable **Abrir automaticamente após 5 segundos**, and save (subject to the existing settings-delay policy). There is no silent migration enabling automation.

### Corrected interaction

Automatic screen: **Vamos para o Kindle**, **Abrindo em 5…**, then 4/3/2/1, **Abrir Kindle agora**, **Cancelar e voltar ao início**. No competing Continue or unlock challenge. Custom copy is retained separately. Manual mode shows no redirect countdown and retains its existing unlock configuration. The initial manual unlock-delay copy is formatted before the first timer callback, avoiding a raw `%1$d` in the first rendered frame.

Existing Handler deadline and lifecycle/safety guards were preserved: five-second boundary, one consumed attempt, immediate action, cancel/Home, pause/resume, screen-off, save/recreation cancellation, self/original/blocked/missing targets, launch exceptions, no cooldown. No accessibility enforcement change was made.

### Why YouTube can appear before Home

`AppBlocker.doAppBlockerCheck` reacts to an accessibility event from an app/window that has already opened. `showWarningScreen` then calls `service.pressHome()` (`performGlobalAction(GLOBAL_ACTION_HOME)`), optionally force-stops through existing Shizuku support, and posts WarningActivity **100 ms later**, with an existing **1,000 ms** anti-repeat gate. This mechanism matches the app → Home → warning sequence; it is not pre-launch interception. Android/OEM event delivery and Home/Activity scheduling cannot guarantee zero visible target frames. The short clip alone cannot identify exact latency contributors or establish a launcher failure. No speculative removal of guards, new overlay enforcement, or launcher replacement was attempted. End-to-end timing needs the actual phone/service logs.

### RED → GREEN and native rendering evidence

Preserved logs (absolute paths):

- `<LOCAL>/curbox-fix-red-flow.log`: 14 tests, 1 failure, automatic mode still exposed Continue.
- `<LOCAL>/curbox-fix-red-colors.log`: 16 tests, 3 failures, light/dark runtime support colors and mixed modes.
- `<LOCAL>/curbox-fix-red-placeholder-render.log`: placeholder-suppression assertion RED; also an initial native-render fixture error from noncanonical qualifier ordering, corrected before final verification. This fixture error is not claimed as a product bug.
- `<LOCAL>/curbox-fix-red-delay-format.log`: 20 tests, 2 failures, unformatted first-frame delay copy.
- `<LOCAL>/curbox-fix-final-green-build.log`: final targeted **39 tests, 0 failures/errors/skips** plus **assembleFullDebug BUILD SUCCESSFUL**, 1m 10s (54 tasks, 4 executed, 50 up-to-date). Earlier non-up-to-date compile/build also succeeded in `<LOCAL>/curbox-fix-green-build.log`.
- Targeted XML retained at `<LOCAL>/curbox-fix-targeted-test-results/`.
- `<LOCAL>/curbox-fix-full-tests.log`: final broad regression **204 tests, 1 failure**, no errors/skips. Sole failure is the known `ScriptLanguageTest.matchesRegexSupportsCommonFlags` (ComparisonFailure). `<LOCAL>/curbox-fix-baseline.log` reruns that same test in the existing pristine `git archive HEAD` baseline: also fails, with ScriptError rather than the identical exception. No script source was changed. Full XML retained at `<LOCAL>/curbox-fix-full-test-results/`.
- `git diff --check` passed. Existing KSP/Kotlin mismatch/deprecation warnings remain.

Final targeted/build command, with the documented existing Java/Android environment:

```sh
./gradlew testFullDebugUnitTest --tests '*AutoRedirect*' --tests '*AppRedirect*' \
  --tests '*WarningRedirectTest' --tests '*RedirectSettingsTest' \
  --tests '*BlockedAppsSearchTest' assembleFullDebug \
  -PcurboxTestArm64=true --no-daemon --max-workers=2
```

**Actual native Android rendering was possible without installing infrastructure.** Existing Robolectric 4.14.1 with `@GraphicsMode(NATIVE)` builds WarningActivity and its Material dialog, measures/layouts/draws the actual decor into a Bitmap. Inspected the resulting PNGs, not HTML mockups:

- `<LOCAL>/builds/curbox-fix-evidence/curbox-fixed-native-auto.png`: pt-BR automatic dialog, light-system configuration, visible 5-second copy, no Continue/default placeholders.
- `<LOCAL>/builds/curbox-fix-evidence/curbox-fixed-native-manual-dark.png`: dark manual dialog, readable 15-second unlock-delay copy and disabled Continue retained, no redirect countdown.
- `<LOCAL>/builds/curbox-fix-evidence/curbox-fixed-native-custom-large.png`: automatic dialog with custom message and 1.3 font scale; heading/message wrap without clipping, actions remain visible.

These are **real Android native Robolectric renders with fixture app metadata**, not physical-phone/Kindle screenshots. Additional assertions check runtime light/dark colors, enabled state resolution, recreation, contrast >=4.5 for supporting copy, unchanged ordinary warnings in both system themes, explicit 5→4→3 copy, and no unlock on hidden Continue. The real Handler timer boundary remains covered; the legacy CountDownTimer shadow is not presented as proof of real elapsed unlock ticking.

### Verified delivery

- `<LOCAL>/builds/curbox-redirect-5s-fix.apk`
- **13,877,398 bytes**, SHA256 **`ca68d9d3e0c59d77a3f0ad73839b4ce20edad8ea364604b70cc2638b645ada3b`**.
- fullDebug, ARM64 only (`arm64-v8a`), package **`neth.iecal.curbox.redirect.debug`**, **versionCode 8**, versionName `6-full-redirect-debug`, minSdk 26, targetSdk 35, no testOnly marker.
- `apksigner verify --print-certs` passed on the delivery copy. Same debug signer as previous APK: certificate SHA256 **`ae500a99a0e477a808098fa1ddddbbeb6d96ff73e28bb031754c825c8fb77c98`**. Not production signing.
- Metadata/signature log `<LOCAL>/curbox-fix-apk-verification.log`; delivery bytes were hash-compared to the final build output.

Changed for this correction only: `WarningActivity.kt`, `dialog_warning_overlay.xml`, English/pt-BR strings, debug versionCode in `app/build.gradle.kts`, `AutoRedirectTest.kt`, and this report. Existing dirty service/model/editor/picker/translation work was not reset.

**Remaining phone verification:** adb has no attached devices; installed SDK has no emulator/system images and `/dev/kvm` is absent. No infrastructure was installed. Native rendering and JVM tests do not certify POCO accessibility timing, TalkBack, OEM launcher behavior or real Kindle navigation. Install this update, explicitly enable/save the five-second setting, verify wait/open-now/cancel, then background during the countdown and confirm the original app stays blocked. Premium issue #1 remains outside this correction.

---

## Current delivery: opt-in 5 seconds and scoped calm-focus UI

This section supersedes the manual-only behavior and APK hashes in the historical report below. Existing dirty work, translation changes, signing configuration and package were preserved. No commit, push, PR, issue mutation, model delegation, dependency installation or database migration was performed.

### Implemented behavior

- `AppGroup.redirectAfter5Seconds` defaults to `false`. Old JSON and previously configured alternatives remain manual until explicitly enabled. The editor has a clearly labeled switch, disabled until an alternative is selected. Turning the alternative Off also clears automation. The value follows the existing group DataStore save/settings-delay path and JSON representation used by backup/sync.
- The normal app-blocker service forwards the selected group's flag alongside its target and conservative blocked-package set. Hidden warning screens still do not redirect.
- A separate main-looper Handler countdown starts on resume and attempts the alternative at 5,000 ms. It does not depend on the unlock timer, including a 15 second delay or disabled unlocking. Only a resumed, interactive screen can finish it. Pause, screen-off detection, state save, destruction, cancel, or a manual launch cancels it; returning does not restart it. Recreation preserves cancellation and the consumed launch attempt.
- One attempt per warning flow, manual or automatic, including after failure/recreation. The existing action guards remain in use: blank, own package, original app and destinations in the service's active blocked groups cannot launch. Missing launcher intents and runtime launch failures leave the block in place. The service's snapshot guard is conservative, not a claim of atomic revalidation against settings changed during the five seconds; focus and other blockers retain their existing independent enforcement.
- The action says **Abrir Kindle agora** when Kindle's installed label is available, with its icon. Missing labels use **App selecionado**, not an unhelpful package name; a missing icon has a generic app fallback. Failed attempts disable the action and show an inline error plus the existing Toast. **Cancelar e voltar ao início** goes Home without granting cooldown. Redirect paths do not broadcast unlock or alter schedules/usage limits.
- The alternatives picker searches names and packages, trims queries, ignores case, and shows the name, secondary package, icon, selection and empty state. The existing blocked-app selector already searched names; it now also searches packages and displays the package as secondary text. Its search hint is localized rather than hardcoded English.

### Native design scope and sources

This is a Configure surface with an actionable warning, not a four-screen redesign. Kotlin/XML, ViewBinding, Material controls and the existing Inter font are retained. A scoped `ThemeOverlay.Curbox.Redirect` applies to the group editor, alternative picker and redirect warning only. The wider app dynamic theme is not replaced. Controls use 48 dp or larger targets, restrained hierarchy, semantic colors, natural text wrapping and pt-BR copy. No web mockup, copied assets, 3D, invented metrics or fabricated Android preview was produced.

Six scoped palette roles: canvas `#111416`, graphite surface `#1C2225`, mint action `#9DE1CE`, primary text `#EAF0ED`, supporting text `#B7C2BD`, amber attention `#F2C078`. Computed contrast ratios: primary/canvas **16.02:1**, supporting/graphite **8.79:1**, button ink/mint **12.41:1**, amber/graphite **9.65:1**. These are token calculations, not proof of complete device accessibility. Anti-slop source audit: no gradients, glass, fake metrics, equal feature grids, decorative rails or custom brand assets. Device typography/wrapping/keyboard/TalkBack review is still pending.

Read project `agents.md`, `Readme.md`, affected layouts/themes, supplied baseline screenshot `img_b15c5c06b02e.jpg`, `claude-design`, `android-flutter-build`, `test-driven-development`, and the requested public sources via curl:

- https://raw.githubusercontent.com/anthropics/skills/main/skills/frontend-design/SKILL.md
- https://raw.githubusercontent.com/uizze/uizze/main/skills/anti-ui-slop/SKILL.md
- https://raw.githubusercontent.com/uizze/uizze/main/skills/anti-ui-slop/reference/polish.md
- https://raw.githubusercontent.com/uizze/uizze/main/skills/anti-ui-slop/reference/craft.md

The actual pending premium work was found through past-session search and read back, read-only: **https://github.com/felipeveras/curbox-android/issues/1**, OPEN, “Planejar redesign premium arredondado, métricas de foco, streaks e heatmap”. Nothing was created or closed. Its Início/Proteção/Foco/Progresso redesign and goal/streak/heatmap rules are **not complete in this APK**. Existing `AppUsageEntity` and `FocusStatsEntity` provide real source records; `FocusGoalProgress.durationWithinDay` already clips/merges overlapping intervals. Existing `FocusStatsFragment` currently sums session durations, buckets by session start and uses fixed 86,400,000 ms days, so it must not be reused unchanged as evidence for goal-based streaks, overlap-safe metrics or DST-safe comparisons. Safe continuation: prototype the four screens with explicit states, then test domain rules and coverage before integrating real metrics. No persistent streak semantics or Room changes were rushed into this delivery.

### Tests, RED evidence and build

Actual behavioral RED logs were retained:

- `<LOCAL>/curbox-5s-red-timer.log`: 5 tests, 2 failures for missing automatic launch/readable name. Earlier fixture compilation errors were corrected before retaining this behavioral RED.
- `<LOCAL>/curbox-5s-red-persistence.log`: explicit opt-in failed JSON persistence; the still-under-investigation timer also failed in that run.
- `<LOCAL>/curbox-5s-red-editor.log`: 10 tests, 2 failures for readable editor destination and missing search.
- `<LOCAL>/curbox-5s-red-polish-search.log`: 9 tests, 2 failures for pt-BR immediate-action copy and missing blocked-package search.
- `<LOCAL>/curbox-5s-red-recreation.log`: 13 tests, 2 failures for save-state cancellation and rearming failed attempts on recreation.
- `<LOCAL>/curbox-5s-red-cancel-copy.log`: 13 tests, 1 failure for unclear cancel/Home wording.

The intermediate files named `green-timer`, `green-editor` and `green-polish-search` contain debugging attempts, including failures, and are **not** the final green evidence. Robolectric's CountDownTimer shadow did not advance callbacks with paused-looper time in the initial implementation; the redirect now uses an actual Handler/uptime deadline. PowerManager interactive state is set explicitly in timer fixtures. Material enabled-state tint is checked rather than the disabled fallback `defaultColor`.

Final command (same Java/SDK exports as historical workflow):

```sh
./gradlew testFullDebugUnitTest --tests '*AutoRedirect*' --tests '*AppRedirect*' \
  --tests '*WarningRedirectTest' --tests '*RedirectSettingsTest' \
  --tests '*BlockedAppsSearchTest' assembleFullDebug assemblePlaystoreDebug assembleFdroidDebug \
  -PcurboxTestArm64=true --no-daemon --max-workers=2
```

**BUILD SUCCESSFUL in 2m 55s; 138 actionable tasks, 47 executed, 91 up to date.** Log `<LOCAL>/curbox-5s-verified-build.log`. Parsed test XML: **30 tests, 0 failures, 0 errors, 0 skipped**. Retained targeted XML `<LOCAL>/curbox-5s-targeted-test-results/`. Coverage includes the exact five-second boundary, one attempt, manual launch, cancel/Home, pause/resume, screen off, save-state/recreation, independent unlock delay, no cooldown, missing/self/original/blocked destinations, action launch exceptions, labels/icons/pt-BR, searches/empty results, service forwarding and persistence/default compatibility.

Broad regression: `./gradlew testFullDebugUnitTest -PcurboxTestArm64=true --no-daemon --max-workers=2` returned **195 tests, 1 failure, 0 errors, 0 skipped** in 44s. Only `ScriptLanguageTest.matchesRegexSupportsCommonFlags` failed. Log `<LOCAL>/curbox-5s-full-tests.log`. It was rerun in the existing pristine `git archive HEAD` baseline at `<LOCAL>/.hermes/cache/scratch/curbox-upstream-baseline-90444`; log `<LOCAL>/curbox-5s-upstream-baseline.log`, failed in 25s. Current suite reports ComparisonFailure, pristine baseline reports ScriptError; do not claim identical exceptions. No scripting code was modified. Existing KSP/Kotlin version and deprecation warnings remain. `git diff --check` passed.

### Final artifact

- Delivery: `<LOCAL>/builds/curbox-redirect-5s-premium.apk`
- Build output: `<LOCAL>/curbox-redirect/app/build/outputs/apk/full/debug/app-full-debug.apk`
- **16,530,296 bytes**, SHA256 **`29d8bb6dacd2480ac8dbc9e59947df7f8b389898e5f210d67a90cabde7ea44b9`**
- Package **`neth.iecal.curbox.redirect.debug`**, versionCode **7**, versionName `6-full-redirect-debug`, label `Curbox Redirect Test`.
- ARM64 only (`arm64-v8a`), minSdk 26, target/compile SDK 35, debug build, no `testOnly` marker in badging.
- `apksigner verify --print-certs` passed on the copied delivery file. Same Android debug signer as the prior delivery, certificate SHA256 **`ae500a99a0e477a808098fa1ddddbbeb6d96ff73e28bb031754c825c8fb77c98`**. This is not production signing.
- Metadata/hash/signature verification log: `<LOCAL>/curbox-5s-apk-verification.log`.
- Other real compiled outputs: `app/build/outputs/apk/playstore/debug/app-playstore-debug.apk`, `app/build/outputs/apk/fdroid/debug/app-fdroid-universal-release-unsigned.apk` (the upstream F-Droid filename is misleading; this task built the debug variant).

### Changed files in this evolution

Production: `blockers/AppBlocker.kt`, `data/models/AppBlocker.kt`, `ui/activity/WarningActivity.kt`, `ui/activity/SelectAppsActivity.kt`, group `CreateAppGroupFragment.kt`; new `ui/activity/RedirectAppPicker.kt`, `utils/RedirectApps.kt`; layouts `dialog_warning_overlay.xml`, `fragment_create_app_group.xml`, `activity_select_apps.xml`, `select_apps_item.xml`; new `dialog_redirect_picker.xml`, `item_redirect_choice.xml`, `values/redirect_theme.xml`; English and pt-BR string resources. Tests: new `AutoRedirectTest`, `AutoRedirectPersistenceTest`, `BlockedAppsSearchTest`; extended `RedirectSettingsTest`, `WarningRedirectTest`, `AppRedirectServiceTest`. The pre-existing dirty files outside this list were not reset or rewritten.

### Remaining verification checklist

- [x] Opt-in compatibility, timer/cancel/lifecycle/recreation, no unlock, safety, labels, search, persistence automated tests.
- [x] Full/playstore/fdroid debug APK compilation and full regression with disclosed baseline failure.
- [x] Copied fullDebug ARM64 APK verified for package, version, signer, ABI, SHA256.
- [ ] Real device UI, TalkBack, large fonts, keyboard, OEM accessibility/launch behavior and Kindle navigation. `adb devices` is empty; no installed emulator/system images or `/dev/kvm`. No emulator was installed. No device-tested claim or synthetic preview.
- [ ] On Felipe's phone, update the existing personal APK; verify old groups remain manual, select Kindle by search, enable 5 seconds and save, trigger a block, wait or open now, confirm original app still blocks. Then cancel/Home, background at 2 seconds, rotate, uninstall target and test it in a blocked group. Check layout with enlarged fonts and the keyboard.
- [ ] Broader premium issue #1: four screens, goal policy, data coverage, historic goals, streak/heatmap and equivalent-period comparisons remain a separate continuation.

---

## Historical manual-only delivery (superseded artifact details)

Base: `curbox-app/curbox-android`, branch `kt-rewrite`, commit `90444ce3fa0c0d86781368166b80f9337bf5830a`. GPL license retained. No commit, push, remote fork, or PR was created.

## Behavior

The app group editor has an optional **Open instead** picker. **Off** is the default. Pick an installed launcher app such as Kindle or Audible and save the group. Its normal block screen then has a separate **Open <package>** button. This button does not unlock the blocked app, emit a cooldown broadcast, or change schedules and usage limits. If several groups block an app, the group selected by the existing blocking logic supplies the alternative.

This implementation deliberately uses a user initiated button, not automatic launches from accessibility events. Repeated accessibility events therefore cannot produce a redirect loop. The action is attempted once per warning screen instance. Own package, original blocked package, blank targets, and packages in any active app blocking group are rejected. This last rule is conservative: even a package currently outside its active schedule is unavailable as an alternative if it belongs to an active group. Launcher resolution happens at click time. Missing, disabled, or launch denied apps leave the warning open, disable repeated attempts, and show an error. Other blockers, including focus mode, retain their enforcement.

If the warning dialog is hidden, no redirect button is shown and ordinary blocking still applies. The editor explains this requirement. Turning off the alternative does not weaken the restriction. Group saves still use the existing DataStore and settings delay path. The nullable `AppGroup.redirectPackage` field is included in the existing Gson settings, backup, and sync representation. Old JSON without the field remains disabled. No Room schema or migration changed.

## Tests and evidence

JDK: Temurin 17.0.20.1. Gradle wrapper: 8.13. Android SDK compile/target: 35, minSdk: 26.

Tests were written and run before each main behavior slice. Retained logs:

- `<LOCAL>/curbox-red-persistence.log`: 1 test failed, expected Kindle package but field did not survive JSON. `<LOCAL>/curbox-green-persistence.log`: passed after defaulted field.
- `<LOCAL>/curbox-red-action-api.log`: test compilation failed because the new action API did not exist. `<LOCAL>/curbox-green-action.log`: passed after minimum implementation. This API bootstrap is a compile RED, not a behavioral assertion RED.
- `<LOCAL>/curbox-red-safety.log`: self and blocked destinations incorrectly launched. `<LOCAL>/curbox-green-safety.log`: passed after guards.
- `<LOCAL>/curbox-red-failure.log`: blank and launch failure behavior failed. `<LOCAL>/curbox-green-failure.log`: passed after blank/error handling.
- `<LOCAL>/curbox-red-warning.log`: real Robolectric warning screen failed with `A separate redirect button must exist`. `<LOCAL>/curbox-green-warning.log`: passed after button wiring.
- `<LOCAL>/curbox-red-service.log`: real blocking event opened the normal screen but forwarded null instead of Kindle. `<LOCAL>/curbox-green-service.log`: passed after service wiring.
- `<LOCAL>/curbox-red-settings.log`: real group editor failed with `Group editor must expose optional destination`. `<LOCAL>/curbox-green-settings.log`: passed after picker wiring.
- `<LOCAL>/curbox-red-label.log`: destination label assertion failed before labeling the button. Final targeted tests verify it passes.

Final targeted tests and all flavor builds:

```sh
export JAVA_HOME=<LOCAL>/builds/streak-toolchain/java
export ANDROID_HOME=<LOCAL>/builds/streak-toolchain/android
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew testFullDebugUnitTest --tests '*AppRedirect*' --tests '*WarningRedirectTest' --tests '*RedirectSettingsTest' assembleFullDebug assemblePlaystoreDebug assembleFdroidDebug -PcurboxTestArm64=true --no-daemon --max-workers=2
```

Result: **BUILD SUCCESSFUL in 1m 20s**, 138 actionable tasks, 23 executed, 115 up to date. Log: `<LOCAL>/curbox-verified-build.log`. The five feature test classes contain **12 tests, zero failures**: persistence/defaults, one attempt, own/blocked targets, blank and launch failures, real service intent forwarding, real block screen launch/no cooldown/missing app/disabled default, real group picker and DataStore save.

Full suite command:

```sh
./gradlew testFullDebugUnitTest -PcurboxTestArm64=true --no-daemon --max-workers=2
```

Result: **177 tests, 1 failed** (`ScriptLanguageTest.matchesRegexSupportsCommonFlags`); all feature tests pass. Log: `<LOCAL>/curbox-final-full-tests.log`. This existing test also fails in a pristine `git archive HEAD` checkout at `<LOCAL>/.hermes/cache/scratch/curbox-upstream-baseline-90444`; log `<LOCAL>/curbox-upstream-baseline.log`. The pristine checkout fails with ScriptError while the final suite reports ComparisonFailure; this is not a claim that the exact exception is identical. No scripting code was changed. The earlier isolated JVM-only reproduction failed to compile due to missing unrelated sources and is not baseline proof.

The upstream F-Droid unit test task also fails before feature changes because common `SupabaseRestTest` references sync-only source absent in that flavor. Log `<LOCAL>/curbox-baseline.log`. F-Droid APK compilation succeeds. Upstream KSP 1.9.25 versus Kotlin 1.9.24 warnings and existing deprecations remain; they did not prevent APK builds.

## APK and signing

Primary APK:

`<LOCAL>/curbox-redirect/app/build/outputs/apk/full/debug/app-full-debug.apk`

- 13,584,681 bytes
- SHA256 `bca54d1c7e733f1b2bdad8b891075606ba4ae207d43d4d6f9ea16e4e844cadcd`
- Package `neth.iecal.curbox.redirect.debug`, label `Curbox Redirect Test`
- Version `6-full-redirect-debug`, versionCode 6
- Native ABI `arm64-v8a` only; no `testOnly` flag in final `aapt dump badging`
- Debug signed, NOT production signed. Certificate DN `C=US, O=Android, CN=Android Debug`.
- Certificate SHA256 `ae500a99a0e477a808098fa1ddddbbeb6d96ff73e28bb031754c825c8fb77c98`

`apksigner verify --print-certs` passed. The separate debug application ID avoids replacing upstream release or normal upstream debug installations. Release package/signing/support are unchanged. The ARM64 restriction is opt in with `-PcurboxTestArm64=true` and only applies to debug builds.

Also compiled and signature verified:

- `<LOCAL>/curbox-redirect/app/build/outputs/apk/playstore/debug/app-playstore-debug.apk`, 13,832,312 bytes, SHA256 `ed05236651b95a1f8a0f5156de335617ead7654083272d07f46dc9bc13120faa`
- `<LOCAL>/curbox-redirect/app/build/outputs/apk/fdroid/debug/app-fdroid-universal-release-unsigned.apk`, 10,448,404 bytes, SHA256 `e9f124d11dc4ec49a1352c5fdeb5259534d885f3e7bc1ef5427e3e6d06da09dd`. Despite its upstream filename this is a debug signed ARM64 APK, not an unsigned release.

An earlier IDE injected ABI build generated testOnly intermediates. Do not distribute those older hashes. Final outputs above use the dedicated debug ABI property and are normal installable APKs.

## Device checklist and limits

`adb devices` returned no devices. No emulator, physical device, accessibility permission grant, or real Kindle/Audible navigation was tested. Robolectric tests are not device tests.

On a phone: install the primary APK, grant its accessibility permission, create an app blocking group, pick Kindle/Audible, save, trigger the block, click the alternative button, and return to the blocked app to confirm blocking still works. Also test Off, an uninstalled destination, a destination added to another active blocking group, and the warning screen hidden setting. Android OEM background behavior and launch policies still need real device validation.

Orca continuation was optional and unavailable according to the parent run (`runtime_open_timeout`, application not running). This implementation did not use or claim the requested Orca model topology.
