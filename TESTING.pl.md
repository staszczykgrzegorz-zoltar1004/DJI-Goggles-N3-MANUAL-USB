# Odtwarzanie testów

[Polski](TESTING.pl.md) / [English](TESTING.md)

Publiczny APK jest kompilacją release. Runner Instrumentation jest osobnym
APK wyłącznie do kontroli jakości, podpisanym tym samym kluczem. Nie publikuj
testowego APK ani klucza.

## Rdzeń bez Androida

Wymagany JDK 17. Z katalogu projektu uruchom `TEST.ps1 -JavaHome <JDK 17>`.
Na innych systemach utwórz `test-build` i uruchom:

```text
javac -encoding UTF-8 -d test-build app/src/main/java/pl/stasz/n3manual/UiFailure.java app/src/main/java/pl/stasz/n3manual/Duml.java app/src/main/java/pl/stasz/n3manual/LogicTunnel.java app/src/main/java/pl/stasz/n3manual/ManualWizard.java tests/CoreTests.java
java -cp test-build pl.stasz.n3manual.CoreTests
```

Oczekiwane87 asercji i13 wektorów bajtowych. Testy obejmują CRC/ramki,
ścisłe dopasowanie ACK, porcjowanie strumienia, port30000, pomijanie obrazu,
ograniczone odpowiedzi identyfikacji, symulowany kreator, już wybrane Manual
i odmowę pracy przy innym ekranie/zaznaczeniu. Symulacja nie potwierdza zapisu
w fizycznym dronie.

## Testy Androida

Zbuduj `:app:assembleRelease :app:lintRelease :app:assembleDebugAndroidTest`.
Wyrównaj i podpisz release oraz test APK tym samym własnym kluczem, zainstaluj
oba na jednorazowym emulatorze i uruchom:

```text
adb -s emulator-5580 shell am instrument -w -r pl.stasz.n3manual.test/pl.stasz.n3manual.SmokeRunner
```

Oczekiwane `N3_QA_PASS api=... checks=44 core=87`. Runner wymaga
versionName1.0.0-beta2. Sprawdza flagi release, brak INTERNET, początkowy stan
przycisków, anulowanie, nieaktualny Confirm, symulowaną odmowę zgody,
odtworzenie Activity i testy rdzenia. Sprawdza też zasobyPL/EN, pełną pomoc,
błędy i przełączenia PL→EN→System wraz z odtworzeniem, zapisem wyboru,
językiem widoku i kasowaniem Confirm. Nie symuluje kontrolera fizycznego USB.

`qa/run_matrix.py` obsługuje Windows i oficjalne obrazy AOSP/default x86_64.
Ustaw ANDROID_HOME. Umieść podpisany release w
`release/public/1.0.0-beta2/N3Manual-1.0.0-beta2.apk`, a test APK
w `qa/signed-tests-beta2.apk`. Następnie:

```text
python qa/run_matrix.py --apis 23 27 29 31 33 34 35 36
```

Skrypt używa własnych N3Manual_API*, działa kolejno i zatrzymuje emulatory.
Wyniki zapisuje w `qa/results-beta2/api*/`. Nie zmienia BIOS, sterowników ani
autostartu. Dane obrazów i AVD wymagają wielu GB miejsca. Opcja środowiskowa
N3_QA_RAM_MB pozwala dobrać pamięć emulatora. Zrzuty pion/poziom/czcionka130%
wymagają oceny czytelności i przewijania. Dodatkowe testy języków obejmują
System z PL/EN/innym językiem systemu oraz wybór przez widoczny dialog.

## Próby sprzętowe

Zapisz model telefonu, Android/API, firmware i język N3, kabel/tryb USB i hash
APK. Na stole, ze zdjętymi śmigłami i wyłączonymi silnikami, sprawdź zgodę USB,
ponowne podłączenie, zwolnienie połączenia przez Fly, strzałki i kreator,
osobno już wybrane M oraz Sport→Manual→widoczne Confirm. Sprawdź anulowanie,
wyjście z aplikacji i odłączenie USB, a potem znacznik M po odłączeniu i restarcie.
Nie testuj w locie. ACK nie jest dowodem trwałego przypisania. Nie usuwaj
kontroli rozpoznawania sprzętu i układu menu.
