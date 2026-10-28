# Raport testów — 1.0.0-beta2

[Polski](TEST_REPORT.pl.md) / [English](TEST_REPORT.en.md)

28.10.2026 · Grzegorz Staszczyk · MIT

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

Każdy PASS oznacza instalację dokładnie tego podpisanego, niedebugowalnego APK
na oficjalnym obrazie AOSP/default x86_64, Android Emulator 37.2.12/WHPX.
Zaliczono 5 wersji Androida. NOT_RUN oznacza brak startu emulatora z powodu
niewystarczającego miejsca na jego dysku; nie wykonano tam testu aplikacji beta2.
Wcześniejsza beta1 niezależnie zaliczyła8API — to wynik historyczny, nie wynik beta2.

Każdy PASS zawiera44 kontrole: flagi i wersję release, brak INTERNET, blokadę
poleceń bez USB, anulowanie i kasowanie nieaktualnego Confirm, symulowaną
odmowę zgody, odtworzenie Activity, zasobyPL/EN, pełną pomoc i błędy,
przełączenia PL→EN→System, zapis wyboru i wybór dla pierwszego językaPL/EN/DE
(symulowana konfiguracja systemowych Resources w Instrumentation).
Ten sam test wykonuje 87 asercji produkcyjnego CRC, ramek, tunelu i kreatora.
Zewnętrzne testy UI obejmują diagnostykę bez USB, pion, poziom i czcionkę 130%.
Na API 34 dodatkowo użyto widocznego wyboru języka, ponownie uruchomiono aplikację
po polsku, przywróconoEnglish/System i zapisano zrzuty PL oraz wyboru języka.

Wszystkie 113 kluczy tekstów i parametrów formatowania są zgodne w zasobach
angielskich i polskich. Jeden uniwersalny APK zawiera oba języki. Sprawdzono
systemowy wybór Android 13+ i własny zapis Android 6–12. Instalator i systemowe
zgody USB należą do Androida i korzystają z jego języka. Język aplikacji
nie zmienia języka menu gogli.

Build release/lint: 0 błędów, 1 informacyjne ostrzeżenie min-API dla localeConfig
(używane od Android  13, ignorowane wcześniej). Podpisy v1/v2/v3 sprawdzone,
ten sam certyfikat autora co beta1. Czysta kompilacja archiwum źródeł:
PASS; wpisy APK poza podpisem są bajtowo identyczne.

Sprzętowy feedback pozostaje z prototypu 0.3 na Hammer Construction 2 Thermal 5G
/ Android 14, N3 + oryginalny Neo + FPV RC3. Nie wykonano fizycznego testu USB
beta2 ani innych telefonów. Emulator nie weryfikuje gogli, firmware,
prawdziwego okna zgody USB ani trwałości przypisania M. Pełną ścieżkę
Sport→Manual→Confirm sprawdzono w symulacji; wynik wymaga obserwacji w goglach.
Nie dodano poleceń firmware ani silników.
