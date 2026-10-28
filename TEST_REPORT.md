# Raport testów / Test report — N3 Manual 1.0.0-beta1

2026-10-28 · Autor: Grzegorz Staszczyk · MIT

Dokładny publiczny APK: `N3Manual-1.0.0-beta1.apk` (41887 B).
SHA-256: `9c315ac97bfeff2187ece986488f8c17a5a6d5a6a30b6084fd1bab9f299396cd`.
Wszystkie poniższe testy instalowały ten sam podpisany, niedebugowalny APK.

## Macierz emulatorów / Emulator matrix

Android Emulator37.2.12 (build16428233), oficjalne obrazy AOSP/default x86_64,
Windows/WHPX. Obrazy pobrano z repozytorium Google i sprawdzono rozmiar,
opublikowaną sumę SHA-1 oraz CRC archiwum. Ekran720×1280, obrót1280×720,
czcionka100% i130%. Zapisane wyniki są w `qa/public-matrix.json` w źródłach.

| Android | API | ABI emulatora | Wynik | Kontrole runtime | Asercje rdzenia |
|---|---:|---|---|---:|---:|
| 6.0 | 23 | x86_64 | PASS | 20 | 87 |
| 8.1.0 | 27 | x86_64 | PASS | 20 | 87 |
| 10 | 29 | x86_64 | PASS | 20 | 87 |
| 12 | 31 | x86_64 | PASS | 20 | 87 |
| 13 | 33 | x86_64 | PASS | 20 | 87 |
| 14 | 34 | x86_64 | PASS | 20 | 87 |
| 15 | 35 | x86_64 | PASS | 20 | 87 |
| 16 | 36 | x86_64 | PASS | 20 | 87 |

Każde uruchomienie: instalacja release + oddzielny test APK o tym samym podpisie;
brak flagi debuggable i uprawnienia INTERNET; poprawny stan przycisków bez USB;
anulowanie po opuszczeniu aplikacji; unieważnienie oczekującego Confirm;
ponowne sprawdzenie gotowości po powrocie; symulowany callback odmowy zgody;
odtworzenie Activity; 87 asercji produkcyjnego protokołu i kreatora na Androidzie.
Dodatkowo: otwarcie diagnostyki przy braku USB, zrzuty pion/poziom i czcionka130%.
Interfejs jest przewijany; przyciski niżej wymagają przewinięcia.

Łącznie160 kontroli runtime i696 asercji rdzenia w8 środowiskach, poza
odrębnym testem87 asercji JVM. Build release i lint:0 błędów,22 ostrzeżenia
SetTextI18n (polskie teksty w kodzie; interfejs nie jest wielojęzyczny).
Podpis APK zweryfikowany dla schematów v1/v2/v3. Brak bibliotek `.so`.

Źródła rozpakowano z publicznego ZIP i zbudowano od zera w nowym katalogu,
używając JDK17/Gradle8.7/SDK35 oraz istniejącej pamięci zależności build.
Wszystkie9 wpisów APK poza META-INF (kod, manifest i zasoby) są bajtowo
identyczne z testowanym podpisanym APK. Nie porównywano samego podpisu.

## Sprzęt / Physical hardware

Właściciel potwierdził połączenie menu oraz przejście do już wybranego Manual
w prototypie0.3 na Hammer Construction2 Thermal5G / Android14 (API34),
DJI GogglesN3 + oryginalny Neo + FPV Remote Controller3, przewód C–C/AOA.
Później zgłosił, że aplikacja działa poprawnie. Publiczna beta zachowuje
ten sam transport/protokół, lecz fizycznego testu tej nowej kompilacji nie
wykonano. Nie odnotowano osobnego, kontrolowanego testu nowego
Sport→Manual→Confirm na Androidzie; pełny przepływ sprawdzono w symulacji,
a wcześniejsze przywrócenie M przez menu z Windows potwierdzono sprzętowo.

Emulatory nie sprawdzają fizycznego USB, prawdziwego okna zgody dla N3,
firmware gogli ani trwałości przypisania M. Nie potwierdzono fizycznie innych
marek telefonów, innych dronów/gogli, wszystkich języków menu lub wersji
firmware. Android7/9/11 nie były osobnymi punktami tej macierzy.

## Granice wydania / Release scope

Beta do dalszych prób społeczności na ziemi. To pilot menu i kreator wyboru M,
bez roota, firmware, bezpośredniego zapisu FC, automatycznego startu i sterowania
silnikami. Zgodność menu jest sprawdzana, a ostateczny wynik wymaga obserwacji
w goglach. SHA-256 plików wydania: `SHA256SUMS.txt`.

The emulator results validate Android app compatibility, not physical USB or
goggles behavior. Only the earlier prototype has owner-confirmed hardware
feedback on the Hammer phone. Other handsets and this release's physical
Sport-to-Manual confirmation still require bench validation.
