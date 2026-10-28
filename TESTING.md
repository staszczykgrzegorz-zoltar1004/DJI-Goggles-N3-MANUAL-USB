# Reproducing tests

The public APK is a release build. The instrumentation runner is a separate
QA-only application and is never included in the public APK.

## Core logic, without Android hardware

JDK 17, from the source root (create `test-build` first):

```text
javac -encoding UTF-8 -d test-build app/src/main/java/pl/stasz/n3manual/UiFailure.java app/src/main/java/pl/stasz/n3manual/Duml.java app/src/main/java/pl/stasz/n3manual/LogicTunnel.java app/src/main/java/pl/stasz/n3manual/ManualWizard.java tests/CoreTests.java
java -cp test-build pl.stasz.n3manual.CoreTests
```

Windows helper: `./TEST.ps1 -JavaHome <JDK 17>`. Expect 87 assertions plus
13 byte-vector outputs. Test cases cover CRC/framing, strict ACK correlation,
fragmented/concatenated traffic, N3 control port30000, video exclusion,
restricted identity replies, complete simulated wizard, already-selected
Manual and rejection of unexpected focus/layout. Simulated menu tests do not
prove changes on a physical aircraft.

## Android runtime / UI

1. Build `:app:assembleRelease :app:lintRelease :app:assembleDebugAndroidTest`.
2. Align/sign the unsigned release and the generated test APK with the **same
   test key**. Instrumentation of a non-debuggable release requires matching
   signatures. Do not distribute your private key or the test APK.
3. Install both on a disposable Android emulator and run:

```text
adb -s emulator-5580 install -r path/to/signed-release.apk
adb -s emulator-5580 install -r path/to/signed-tests-beta2.apk
adb -s emulator-5580 shell am instrument -w -r pl.stasz.n3manual.test/pl.stasz.n3manual.SmokeRunner
```

Expected result: `N3_QA_PASS api=... checks=44 core=87`.
The runner requires versionName1.0.0-beta2; update it deliberately for a new release.
It checks the actual release flags/version/permissions, launch state,
activity lifecycle cancellation, stale confirmation removal, a simulated USB
permission-denial callback, recreation and production core logic on Android.
It does not emulate the USB controller, real permission dialog, or goggles.

`qa/run_matrix.py` automates Windows headless-emulator startup, installation,
instrumentation, no-device diagnostics and screenshots in portrait, landscape
and 130% font size. Set `ANDROID_HOME` to your SDK, install official AOSP/default
x86_64 system images for the requested APIs and put signed artifacts at the
paths declared at the top of that script. With the distributed directory layout,
the release APK belongs at `release/public/1.0.0-beta2/N3Manual-1.0.0-beta2.apk`
and the QA APK at `qa/signed-tests-beta2.apk`.

```text
python qa/run_matrix.py --apis 23 27 29 31 33 34 35 36
```

The harness uses only its own `N3Manual_API*` AVDs, stops processes in `finally`,
does not alter BIOS/Windows virtualization/drivers, and keeps results in
`qa/results-beta2/api*/`. Images and AVDs can require many gigabytes of free space.
The script runs sequentially. Emulator screenshots require human inspection
for text clipping and scrollability; runtime assertions do not prove usability.

## Hardware follow-up matrix

Before declaring support for another phone/firmware, record phone model,
Android/API, N3 firmware/menu language, cable/USB mode and exact APK hash.
On the bench with propellers removed and motors off, check:

- system USB permission accepted and refused, reconnection, DJI Fly released;
- root/menu probe and manual arrows;
- already-selected Manual, and separately Sport→Manual→visible Confirm;
- cancel, app backgrounding and USB detach during navigation;
- observed Manual checkmark after disconnect and normal restart.

Do not test in flight. A menu ACK is not evidence of a saved mode assignment.
Do not remove model/layout checks to make an unrecognized device pass.

## Language coverage

The instrumentation checks both English and Polish resources, full help and errors; then switches PL→EN→System through the same preference API used by the picker. It verifies activity recreation, persistence, resolved UI language and invalidation of pending Confirm after each switch. External UI checks cover both languages, reopening, device-language fallback, help/about/diagnostics and the visible picker. System installer dialogs belong to Android and are outside app localization.
