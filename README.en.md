# N3 Manual 1.0.0-beta1

Author: **Grzegorz Staszczyk** · **MIT license** · [Polska instrukcja](README.md)

An independent Android USB menu remote for DJI Goggles N3 with the original
DJI Neo and DJI FPV Remote Controller 3. It helps restore the Custom Mode
assignment to Manual, including when the physical 5D button is damaged.
It is not affiliated with DJI. It does not freeze settings, patch firmware,
disable Normal/Sport, or arm the aircraft. The app interface is Polish.

## Install

Open `N3Manual-1.0.0-beta1.apk` on an Android 6.0/API23 or later phone.
Allow your file manager/browser to install this APK when Android asks; this
permission can be revoked afterwards. No root, account, PC or USB debugging
is needed. SHA-256 checksums accompany the release.

Private prototypes 0.1–0.3 used a different signing key. Uninstall the old
N3 Manual app once before installing this public beta; goggles settings are
unaffected. Future public releases using the same key can be installed as updates.

One universal Java/DEX APK, no CPU-specific native libraries. A compatible
physical USB implementation is still necessary on the phone.

## Restore Manual

1. Work on the bench with propellers removed and motors off. Power on N3,
   Neo and FPV RC3; keep their radio link active. Connect USB to **goggles only**.
2. Close DJI Fly. Connect phone to N3 with a data cable. USB-C–USB-C Android
   Accessory mode worked on the tested Hammer phone. Select N3 Manual as the
   USB app and allow the system USB permission.
3. Tap **Połącz i sprawdź USB** (connect/test). Wait for
   **N3 połączone · menu odpowiada** (N3 connected; menu responding).
   This verifies communication, not the Manual assignment.
4. Tick the bench preparation checkbox and tap **1. Przejdź do Manual**
   (go to Manual). Do not operate another menu controller during the wizard.
5. Look inside the goggles. Only if the selected right-hand button says
   **Confirm**, tap **2. Widzę Confirm — zatwierdź** (I see Confirm; accept).
   Stop on an unexpected screen. If Manual is already selected, no new
   confirmation is necessary.
6. Verify the Manual Mode checkmark / Custom Mode assignment in the goggles.
   Disconnect the phone before flying. Normal controller mode selection and
   preparation for manual flight still apply.

**Przerwij** means cancel. Leaving the app or disconnecting USB also cancels
at a command boundary. **Ręczny pilot menu** provides Left/Up/Right,
Back/Down/OK. Use this application only on the ground.

## Troubleshooting

**Pokaż stan USB** shows diagnostics. If neither a USB device nor an Android
Accessory is available, Android has not exposed the connection to this app.
If DJI Fly opens automatically, clear its default USB opening in Android app
settings and reconnect. USB-host/OTG mode is also implemented; an OTG adapter
may help on some phones, but is unnecessary when the C–C accessory link works.

**Diagnostyka i raport** lets you save a report. Include phone model, Android
version, N3 firmware, aircraft/controller model and exact error when reporting
an issue. Review the report before posting publicly. An unknown menu or device
causes the app to stop; do not bypass the recognition checks.

## Compatibility and privacy

Hardware feedback: Hammer Construction 2 Thermal 5G / Android14 with
N3 + Neo + FPV RC3, private version0.3. The owner confirmed USB menu operation
and reported the application working well. This public beta retains that
transport; a separate hardware check after updating is still needed.
See [TEST_REPORT.md](TEST_REPORT.md) for tests of this exact public APK.
Emulators do not validate physical USB. Other phones, goggles and aircraft
have not been verified on hardware. Other menu languages/firmware may need
wizard changes.

No Internet permission, accounts, ads, analytics, camera/microphone access,
background service or startup automation. Diagnostic events are held in memory
until process exit unless you explicitly export a file. The report includes
phone model, Android version and technical events, not camera recordings,
phone serial numbers or account data. Your chosen export destination may be
a cloud document provider. USB access requires Android's system permission.

## Source and build

MIT, Copyright (c)2026 Grzegorz Staszczyk. See [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). License texts are also in
the application's About dialog.

Install JDK17, Android SDK Platform35 and Build Tools35.0.0. Set `JAVA_HOME`
and `ANDROID_HOME`, or open the source project in Android Studio. The Gradle8.7
wrapper is checksum-pinned; AGP8.6.1. Initial builds download build dependencies.

```text
Windows:      gradlew.bat :app:assembleRelease :app:lintRelease
Linux/macOS:  sh ./gradlew :app:assembleRelease :app:lintRelease
```

The output is `app/build/outputs/apk/release/app-release-unsigned.apk`.
Sign with your own `apksigner` key. The author's private key is not distributed;
a differently signed build cannot update the original APK. Use your own app ID
and signing identity when distributing a fork. The optional Windows signing
helper uses DPAPI and an external private folder; standard Android signing
tools work independently of it.

Core tests: `TEST.ps1 -JavaHome <JDK17>` or the platform-neutral Java commands
in [TESTING.md](TESTING.md). Android runtime test sources are included.
