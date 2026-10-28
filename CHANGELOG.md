# Changelog

## 1.0.0-beta1 — 2026-10-28

First public beta, author Grzegorz Staszczyk, MIT.

- Signed, non-debuggable universal APK; Android 6.0 minimum, target API 35.
- Preserves the working 0.3 N3 mobile USB transport and read-driven menu wizard.
- Cancels and clears pending confirmation when leaving the application; ignores
  stale results after a USB connection changes.
- Rechecks permission before allowing commands; USB is optional at installation.
- Updated connection help, author/license display and privacy information.
- No cloud backup of application data; diagnostic export remains user initiated.
- Public source, Gradle wrapper and automated core/runtime compatibility tests.

## 0.3 — private prototype

Accepted N3 incoming LogicLink control port 30000 as well as 22345. Real phone
connection and navigation to already selected Manual confirmed by the owner.

## 0.2 / 0.1 — private prototypes

Android Accessory support, USB-host support, menu wizard and diagnostic tools.
