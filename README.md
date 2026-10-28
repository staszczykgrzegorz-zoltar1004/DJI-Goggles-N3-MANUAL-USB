# N3 Manual 1.0.0-beta1

Autor: **Grzegorz Staszczyk** · licencja **MIT** · [English guide](README.en.md)

Natywna aplikacja Android do przywracania przypisania Manual w menu DJI Goggles
N3, używanych z DJI Neo i DJI FPV Remote Controller 3. Powstała jako obejście
uszkodzonego przycisku 5D. Telefon działa jako pilot menu przez USB.

To niezależne narzędzie, bez powiązania z DJI. Przywraca wybór w menu; nie
zamraża ustawień, nie modyfikuje firmware i nie wyłącza trybów Normal/Sport.

## Instalacja z APK

1. Pobierz `N3Manual-1.0.0-beta1.apk` z tego samego wydania co źródła i sumy
   `SHA256SUMS.txt`. Jest to podpisany instalator Androida, bez dodatkowego ZIP.
2. Otwórz plik w telefonie. Jeśli Android poprosi, zezwól używanej aplikacji
   Pliki/przeglądarce na instalację z tego źródła. Po instalacji możesz cofnąć
   to zezwolenie. Debugowanie USB i komputer nie są potrzebne.
3. Jeśli masz prywatny prototyp 0.1–0.3: ma inny podpis, więc najpierw odinstaluj
   starą aplikację N3 Manual. Nie zmienia to ustawień gogli. Kolejne publiczne
   wydania podpisane tym samym kluczem będzie można instalować jako aktualizacje.

Minimum: Android 6.0/API 23 oraz obsługa odpowiedniego trybu USB przez telefon.
APK zawiera kod Java/DEX, bez bibliotek zależnych od procesora. Jest wspólny dla
ARM/ARM64/x86/x86_64; zgodność fizycznego USB zależy od telefonu i gogli.
Interfejs aplikacji jest po polsku; instrukcja angielska objaśnia etykiety.

## Przywrócenie Manual

1. Zestaw na stole, śmigła zdjęte, silniki wyłączone. Włącz N3, Neo i FPV RC3,
   połącz je radiowo. USB podłączaj **tylko do gogli**, nie do Neo.
2. Zamknij DJI Fly. Połącz telefon z N3 przewodem z transmisją danych; na
   sprawdzonym Hammerze działał przewód USB-C–USB-C w trybie Android Accessory.
   W oknie aplikacji USB wybierz N3 Manual i zaakceptuj dostęp.
3. Dotknij **„Połącz i sprawdź USB”**. Poczekaj na
   **„N3 połączone · menu odpowiada”**. To potwierdzenie komunikacji, nie M.
4. Zaznacz pole przygotowania zestawu i dotknij **„1. Przejdź do Manual”**.
   Podczas działania nie poruszaj menu innym kontrolerem.
5. Spójrz w gogle. Tylko jeśli zaznaczony prawy przycisk ma napis **Confirm**,
   dotknij **„2. Widzę Confirm — zatwierdź”**. Przy innym ekranie przerwij.
   Jeśli Manual było już wybrane, nowe Confirm nie jest potrzebne.
6. Sprawdź w goglach znacznik przy Manual Mode / przypisanie Custom Mode.
   Przed lotem odłącz telefon. Ta operacja nie zastępuje zwykłego wyboru M
   przełącznikiem kontrolera ani przygotowania pilota do lotu Manual.

„Przerwij”, opuszczenie aplikacji albo odłączenie USB anuluje operację na
granicy polecenia. Strzałki i OK/Wstecz pozwalają sterować menu ręcznie.
Nie używaj aplikacji podczas lotu.

## Gdy nie łączy

- Jeśli zawsze uruchamia się DJI Fly, usuń jego domyślne otwieranie połączenia
  USB w ustawieniach aplikacji i podłącz ponownie przewód. Nazwy ustawień mogą
  się różnić między telefonami. Aplikacja nie potrzebuje roota ani debugowania.
- „Pokaż stan USB” wyświetla urządzenia host USB i akcesoria Android. Brak obu
  oznacza, że Android nie udostępnił aplikacji połączenia. Sam obraz w DJI Fly
  nie dowodzi dostępu do menu w N3 Manual.
- Aplikacja obsługuje też host USB/OTG. Adapter OTG po stronie telefonu może
  pomóc przy niedostępnym kanale Accessory, lecz nie jest konieczny na każdym
  telefonie. Nie wymuszaj innego trybu, gdy połączenie C–C już działa.
- Przy błędzie menu zapisz raport przez „Diagnostyka i raport”. Do zgłoszenia
  dodaj model telefonu, Androida, firmware N3, model drona/pilota oraz dokładny
  błąd. Przed publicznym udostępnieniem przejrzyj raport.
- Nieznany układ menu lub odmowa rozpoznania urządzenia oznacza zatrzymanie.
  Nie usuwaj zabezpieczeń rozpoznania i nie podłączaj Neo bezpośrednio do USB.

## Zakres zgodności i testów

Własny zestaw N3 + Neo + FPV RC3 oraz Hammer Construction 2 Thermal 5G,
Android 14: właściciel potwierdził działanie połączenia i kreatora w prototypie
0.3, następnie zgłosił poprawne działanie aplikacji. Publiczna beta zachowuje
ten transport i protokół; jej fizyczny test USB na tym zestawie wymaga osobnego
potwierdzenia po aktualizacji.

Wyniki testów dokładnie tego publicznego APK są w [TEST_REPORT.md](TEST_REPORT.md).
Emulator sprawdza instalację, logikę i interfejs Androida, a nie fizyczne
połączenie z goglami. Innych marek telefonów, gogli ani dronów nie sprawdzono
sprzętowo. Inny firmware lub język menu może wymagać dostosowania kreatora.

## Prywatność i licencja

Brak internetu, reklam, konta, analityki, usług w tle i automatycznego działania
przy starcie. Aplikacja nie prosi o dostęp do kamery, mikrofonu ani całej pamięci.
Potrzebuje systemowej zgody na wybrane USB. Raport zapisujesz sam przez
systemowy wybór pliku. Zawiera model i Androida telefonu oraz zdarzenia
diagnostyczne; nie zawiera nagrań, numeru seryjnego telefonu ani danych konta.
Raport pozostaje w pamięci do zamknięcia procesu, chyba że go wyeksportujesz.
Wybrana przez Ciebie lokalizacja eksportu może być usługą chmurową.

[LICENSE](LICENSE) — MIT, Copyright (c) 2026 Grzegorz Staszczyk.
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) — zachowane licencje zależności
i odniesień. Licencje są też dostępne w „O aplikacji i prywatności”.

## Budowanie źródeł

Wymagania: JDK 17, Android SDK Platform 35 / Build Tools 35.0.0 i dostęp do
repozytoriów Google/Maven przy pierwszym budowaniu. Gradle 8.7 jest przypięty
wrapperem z sumą SHA-256; Android Gradle Plugin 8.6.1. Ustaw `JAVA_HOME` i
`ANDROID_HOME` albo otwórz projekt w Android Studio i wskaż SDK lokalnie.

```powershell
.\gradlew.bat :app:assembleRelease :app:lintRelease
.\TEST.ps1 -JavaHome $env:JAVA_HOME
```

Na Linux/macOS użyj `sh ./gradlew :app:assembleRelease :app:lintRelease`.
Wynik jest w `app/build/outputs/apk/release/app-release-unsigned.apk`.
Do instalacji podpisz własnym kluczem przez `apksigner`. Klucz autora nie jest
częścią źródeł; własny podpis nie pozwoli aktualizować oryginalnego APK.
Przy publikacji forka zmień identyfikator aplikacji i nazwę podpisu.

Opcjonalny `tools/sign_apk.py` jest pomocnikiem Windows/Python 3.11+ dla
lokalnego zestawu narzędzi `N3_ANDROID_TOOLS`. Chroni hasło przez Windows DPAPI,
trzyma prywatny klucz poza projektem i nie umieszcza go w archiwach publicznych.
Nie jest wymagany do budowy ani instalacji zwykłym `apksigner`.

87 asercji rdzenia uruchamia `TEST.ps1`; testy Android Instrumentation są
w `app/src/androidTest` i `tests`. Metoda odtworzenia macierzy: [TESTING.md](TESTING.md).

Dokumentacja platformy: [USB Accessory](https://developer.android.com/develop/connectivity/usb/accessory),
[USB Host](https://developer.android.com/develop/connectivity/usb/host).
