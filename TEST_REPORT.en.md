# Test report — 1.0.0-beta2

[English](TEST_REPORT.en.md) / [Polski](TEST_REPORT.pl.md)

2026-10-28 · Grzegorz Staszczyk · MIT

APK: `N3Manual-1.0.0-beta2.apk` · 62423 B

SHA-256: `e6f88f8a3027f355f0e6c96dd307f9fbfa6cd2ddba4830d382e8c447705ddd6b`

| Android | API | Result / Wynik | Runtime + core |
|---|---:|---|---|
| 6.0 | 23 | PASS | 44 + 87 |
| 8.1 | 27 | NOT_RUN | — |
| 10 | 29 | PASS | 44 + 87 |
| 12 | 31 | NOT_RUN | — |
| 13 | 33 | PASS | 44 + 87 |
| 14 | 34 | PASS | 44 + 87 |
| 15 | 35 | NOT_RUN | — |
| 16 | 36 | PASS | 44 + 87 |

All PASS rows installed the exact signed, non-debuggable APK above on official
AOSP/default x86_64 images with Android Emulator 37.2.12/WHPX. 5 Android
versions passed. NOT_RUN rows could not start the emulator because its volume
had insufficient free disk space; no app test was run there for beta2.
The earlier beta1 independently passed all eight APIs; that is historical
evidence and is not represented as a beta2 result.

Each PASS includes 44 runtime checks: release flags/version, no INTERNET,
disabled commands without USB, cancellation and stale Confirm invalidation,
simulated permission denial, activity recreation, English/Polish resources,
full help and errors, manual PL→EN→System switching, persistence and primary-system
PL/EN/DE fallback (simulated system Resources configuration in instrumentation).
The same run executes 87 production CRC/frame/tunnel/wizard assertions.
External UI checks cover absent-USB diagnostics, portrait, landscape and 130%
font. API 34 additionally operates the visible picker, restarts in Polish,
returns to English/System and saves Polish/picker screenshots.

All 113 string keys and format placeholders match across the English default
and Polish resources. One universal APK contains both languages. Native
Android 13+ language preferences and the Android 6–12 local preference are tested.
Installer and USB permission windows belong to Android and use its language.
Changing app language does not change goggles menu language.

Release build/lint: 0 errors, 1 informational min-API warning for localeConfig
(used on Android 13+, ignored by older versions). Signing v1/v2/v3 verified,
same author certificate as beta1. Clean source archive build: PASS; non-signature APK entries match byte for byte.

Physical USB feedback remains from prototype 0.3 on Hammer Construction 2
Thermal 5G / Android 14 with N3 + original Neo + FPV RC3. No physical USB test
of beta2 or other phones was performed. Emulator tests do not verify actual
goggles, firmware, real USB permission dialogs or persisted M assignment.
The full Sport→Manual→Confirm path is simulated; the final result always
requires observation inside the goggles. No firmware or motor commands added.
