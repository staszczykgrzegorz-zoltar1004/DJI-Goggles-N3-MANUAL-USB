"""Package a public APK and a strict allowlist of MIT source, never private keys."""
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = '1.0.0-beta1'
PUBLIC = ROOT / 'release/public' / VERSION
APK = PUBLIC / f'N3Manual-{VERSION}.apk'
APIS = (23, 27, 29, 31, 33, 34, 35, 36)
DOCS = ('README.md', 'README.en.md', 'LICENSE', 'CHANGELOG.md',
        'THIRD_PARTY_NOTICES.md', 'TESTING.md', 'TEST_REPORT.md', 'FORUM_PL.md')


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    apk_hash = digest(APK)
    source_check = json.loads((ROOT / 'qa/source-build-verification.json').read_text())
    assert source_check['apk_sha256'] == apk_hash and source_check['clean_source_build'] == 'PASS'
    assert source_check['differing_entries'] == []
    results = [json.loads((ROOT / f'qa/results/api{api}/result.json').read_text()) for api in APIS]
    assert all(r['status'] == 'PASS' and r['apk_sha256'] == apk_hash
               and r['runtime_checks'] == 20 and r['core_assertions'] == 87 for r in results)
    matrix = {'date': '2026-10-28', 'version': VERSION, 'apk_sha256': apk_hash,
              'emulator': '37.2.12 / 16428233 / WHPX', 'results': results}
    (ROOT / 'qa/public-matrix.json').write_text(json.dumps(matrix, indent=2), encoding='utf8')
    rows = '\n'.join(f"| {r['android']} | {r['api']} | x86_64 | PASS | 20 | 87 |" for r in results)
    report = f'''# Raport testów / Test report — N3 Manual {VERSION}

2026-10-28 · Autor: Grzegorz Staszczyk · MIT

Dokładny publiczny APK: `{APK.name}` ({APK.stat().st_size} B).
SHA-256: `{apk_hash}`.
Wszystkie poniższe testy instalowały ten sam podpisany, niedebugowalny APK.

## Macierz emulatorów / Emulator matrix

Android Emulator37.2.12 (build16428233), oficjalne obrazy AOSP/default x86_64,
Windows/WHPX. Obrazy pobrano z repozytorium Google i sprawdzono rozmiar,
opublikowaną sumę SHA-1 oraz CRC archiwum. Ekran720×1280, obrót1280×720,
czcionka100% i130%. Zapisane wyniki są w `qa/public-matrix.json` w źródłach.

| Android | API | ABI emulatora | Wynik | Kontrole runtime | Asercje rdzenia |
|---|---:|---|---|---:|---:|
{rows}

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
'''
    (ROOT / 'TEST_REPORT.md').write_text(report, encoding='utf8')
    for name in DOCS:
        shutil.copy2(ROOT / name, PUBLIC / name)
    shutil.copy2(ROOT / 'qa/public-matrix.json', PUBLIC / 'test-matrix.json')
    shutil.copy2(ROOT / 'qa/source-build-verification.json', PUBLIC / 'source-build-verification.json')
    (PUBLIC / 'licenses').mkdir(exist_ok=True)
    shutil.copy2(ROOT / 'licenses/Apache-2.0.txt', PUBLIC / 'licenses/Apache-2.0.txt')
    shutil.copy2(ROOT / 'app/src/main/res/raw/third_eye_license.txt', PUBLIC / 'licenses/third-eye-MIT.txt')
    # Explicit file and directory allowlists exclude build outputs, archives,
    # phone diagnostics, local.properties, signing state and machine caches.
    files = [ROOT / n for n in DOCS + ('build.gradle', 'settings.gradle',
             'gradle.properties', 'gradlew', 'gradlew.bat', '.gitignore',
             'BUILD.ps1', 'TEST.ps1', 'tools/sign_apk.py',
             'tools/export_signing_key.py', 'tools/make_public_release.py',
             'qa/run_matrix.py', 'qa/public-matrix.json', 'qa/source-build-verification.json')]
    for directory in ('app/src', 'tests', 'gradle/wrapper', 'licenses'):
        files.extend(p for p in (ROOT / directory).rglob('*') if p.is_file())
    files.append(ROOT / 'app/build.gradle')
    forbidden = ('.p12', '.jks', '.keystore', '.apk', '.pyc')
    assert all(p.suffix.lower() not in forbidden and p.name not in
               ('signing.json', 'local.properties') for p in files)
    source = PUBLIC / f'N3Manual-{VERSION}-source-MIT.zip'
    prefix = f'N3Manual-{VERSION}-source'
    with zipfile.ZipFile(source, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for file in sorted(set(files)):
            z.write(file, prefix + '/' + file.relative_to(ROOT).as_posix())
    with zipfile.ZipFile(source) as z:
        assert z.testzip() is None
    paths = sorted(p for p in PUBLIC.rglob('*') if p.is_file() and p.name != 'SHA256SUMS.txt')
    sums = ''.join(f'{digest(p)}  {p.relative_to(PUBLIC).as_posix()}\n' for p in paths)
    (PUBLIC / 'SHA256SUMS.txt').write_text(sums, encoding='ascii')
    bundle = ROOT / 'release' / f'N3Manual-{VERSION}-forum.zip'
    with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for file in sorted(p for p in PUBLIC.rglob('*') if p.is_file()):
            z.write(file, 'N3Manual-' + VERSION + '/' + file.relative_to(PUBLIC).as_posix())
    with zipfile.ZipFile(bundle) as z:
        assert z.testzip() is None
    print(json.dumps({'bundle': str(bundle), 'bundle_sha256': digest(bundle),
                      'source': str(source), 'source_files': len(set(files)),
                      'apk_sha256': apk_hash, 'emulator_pass': len(results)}))


if __name__ == '__main__':
    main()
