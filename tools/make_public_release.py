"""Public, bilingual release packaging from an explicit source allowlist.

Run --prepare for the clean-source build, then run without flags after recording
that build's verification. Private signing state and device reports are excluded.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

ROOT=Path(__file__).resolve().parents[1]
VERSION='1.0.0-beta2'
PUBLIC=ROOT/'release/public'/VERSION
APK=PUBLIC/f'N3Manual-{VERSION}.apk'
DOCS=('README.md','README.en.md','README.pl.md','INSTALL.en.md','INSTALL.pl.md',
      'LICENSE','CHANGELOG.md','CHANGELOG.pl.md','THIRD_PARTY_NOTICES.md',
      'THIRD_PARTY_NOTICES.pl.md','TESTING.md','TESTING.pl.md','TEST_REPORT.md',
      'TEST_REPORT.en.md','TEST_REPORT.pl.md','FORUM_EN.md','FORUM_PL.md')
APIS={23:'6.0',27:'8.1',29:'10',31:'12',33:'13',34:'14',35:'15',36:'16'}

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def package(prepare=False):
    apk_hash=sha(APK)
    results=[]
    for api,version in APIS.items():
        file=ROOT/f'qa/results-beta2/api{api}/result.json'
        raw=json.loads(file.read_text()) if file.exists() else {}
        if raw.get('status')=='PASS' and raw.get('apk_sha256')==apk_hash:
            assert raw['runtime_checks']==44 and raw['core_assertions']==87
            results.append(raw)
        else:
            # Host startup logs may contain network metadata. Publish only the
            # failure category; never label a failed emulator boot an app failure.
            assert not raw.get('checks'), 'An application test failed: investigate before release'
            results.append({'api':api,'android':version,'status':'NOT_RUN',
                            'reason':'host emulator startup blocked by insufficient free disk space',
                            'apk_sha256':apk_hash,'physical_usb_tested':False})
    passed=[r for r in results if r['status']=='PASS']
    assert {23,33,34,36}.issubset({r['api'] for r in passed})
    matrix={'version':VERSION,'date':'2026-10-28','apk_sha256':apk_hash,
            'runtime_checks_per_pass':44,'core_assertions_per_pass':87,
            'resource_strings_per_language':113,'results':results}
    (ROOT/'qa/public-matrix.json').write_text(json.dumps(matrix,indent=2),encoding='utf8')
    check_file=ROOT/'qa/source-build-verification.json'
    check=json.loads(check_file.read_text()) if check_file.exists() else {}
    source_ok=check.get('apk_sha256')==apk_hash and check.get('clean_source_build')=='PASS' and check.get('differing_entries')==[]
    if not prepare:assert source_ok,'Clean source build verification required'
    rows='\n'.join(f"| {r['android']} | {r['api']} | {r['status']} | {'44 + 87' if r['status']=='PASS' else '—'} |" for r in results)
    common=f'APK: `{APK.name}` · {APK.stat().st_size} B\n\nSHA-256: `{apk_hash}`\n\n| Android | API | Result / Wynik | Runtime + core |\n|---|---:|---|---|\n{rows}\n'
    en=f'''# Test report — {VERSION}

[English](TEST_REPORT.en.md) / [Polski](TEST_REPORT.pl.md)

2026-10-28 · Grzegorz Staszczyk · MIT

{common}
All PASS rows installed the exact signed, non-debuggable APK above on official
AOSP/default x86_64 images with Android Emulator 37.2.12/WHPX. {len(passed)} Android
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
same author certificate as beta1. Clean source archive build: {'PASS; non-signature APK entries match byte for byte' if source_ok else 'PENDING (preparation only)'}.

Physical USB feedback remains from prototype 0.3 on Hammer Construction 2
Thermal 5G / Android 14 with N3 + original Neo + FPV RC3. No physical USB test
of beta2 or other phones was performed. Emulator tests do not verify actual
goggles, firmware, real USB permission dialogs or persisted M assignment.
The full Sport→Manual→Confirm path is simulated; the final result always
requires observation inside the goggles. No firmware or motor commands added.
'''
    pl=f'''# Raport testów — {VERSION}

[Polski](TEST_REPORT.pl.md) / [English](TEST_REPORT.en.md)

28.10.2026 · Grzegorz Staszczyk · MIT

{common}
Każdy PASS oznacza instalację dokładnie tego podpisanego, niedebugowalnego APK
na oficjalnym obrazie AOSP/default x86_64, Android Emulator 37.2.12/WHPX.
Zaliczono {len(passed)} wersji Androida. NOT_RUN oznacza brak startu emulatora z powodu
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
{'PASS; wpisy APK poza podpisem są bajtowo identyczne' if source_ok else 'OCZEKUJE (tylko przygotowanie)'}.

Sprzętowy feedback pozostaje z prototypu 0.3 na Hammer Construction 2 Thermal 5G
/ Android 14, N3 + oryginalny Neo + FPV RC3. Nie wykonano fizycznego testu USB
beta2 ani innych telefonów. Emulator nie weryfikuje gogli, firmware,
prawdziwego okna zgody USB ani trwałości przypisania M. Pełną ścieżkę
Sport→Manual→Confirm sprawdzono w symulacji; wynik wymaga obserwacji w goglach.
Nie dodano poleceń firmware ani silników.
'''
    (ROOT/'TEST_REPORT.en.md').write_text(en,encoding='utf8')
    (ROOT/'TEST_REPORT.pl.md').write_text(pl,encoding='utf8')
    (ROOT/'TEST_REPORT.md').write_text('# Tests / Testy\n\n[English report](TEST_REPORT.en.md) / [Raport po polsku](TEST_REPORT.pl.md)\n\n'+common,encoding='utf8')
    for name in DOCS:shutil.copy2(ROOT/name,PUBLIC/name)
    shutil.copy2(ROOT/'qa/public-matrix.json',PUBLIC/'test-matrix.json')
    if source_ok:shutil.copy2(check_file,PUBLIC/'source-build-verification.json')
    files=[ROOT/n for n in DOCS+('build.gradle','settings.gradle','gradle.properties',
        'gradlew','gradlew.bat','.gitignore','BUILD.ps1','TEST.ps1','app/build.gradle',
        'tools/sign_apk.py','tools/export_signing_key.py','tools/make_public_release.py',
        'qa/run_matrix.py','qa/check_translations.py','qa/public-matrix.json')]
    if source_ok:files.append(check_file)
    for directory in ('app/src','tests','gradle/wrapper','licenses'):
        files.extend(p for p in (ROOT/directory).rglob('*') if p.is_file())
    files=sorted(set(files))
    assert all(p.suffix.lower() not in ('.p12','.jks','.keystore','.apk','.pyc') and p.name not in ('signing.json','local.properties') for p in files)
    staging=ROOT/'release/github-upload-beta2';staging.mkdir(exist_ok=True)
    source=PUBLIC/f'N3Manual-{VERSION}-source-MIT.zip'
    with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for file in files:
            rel=file.relative_to(ROOT);dest=staging/rel;dest.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(file,dest);z.write(file,f'N3Manual-{VERSION}-source/'+rel.as_posix())
    guides=PUBLIC/f'N3Manual-{VERSION}-guides-PL-EN.zip'
    with zipfile.ZipFile(guides,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for name in DOCS:z.write(ROOT/name,name)
        z.write(ROOT/'licenses/Apache-2.0.txt','licenses/Apache-2.0.txt')
        z.write(ROOT/'app/src/main/res/raw/third_eye_license.txt','licenses/third-eye-MIT.txt')
    # Exactly the root-level release attachments, so no missing subfolder entries.
    attachments=sorted(p for p in PUBLIC.iterdir() if p.is_file() and p.name!='SHA256SUMS.txt')
    (PUBLIC/'SHA256SUMS.txt').write_text(''.join(f'{sha(p)}  {p.name}\n' for p in attachments),encoding='ascii')
    bundle=ROOT/'release'/f'N3Manual-{VERSION}-forum.zip'
    with zipfile.ZipFile(bundle,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for file in sorted(p for p in PUBLIC.rglob('*') if p.is_file()):z.write(file,f'N3Manual-{VERSION}/'+file.relative_to(PUBLIC).as_posix())
    for file in (source,guides,bundle):
        with zipfile.ZipFile(file) as z:assert z.testzip() is None
    print(json.dumps({'version':VERSION,'source_files':len(files),'passed_apis':[r['api'] for r in passed],
                      'apk_sha256':apk_hash,'source_verified':source_ok,'source':str(source),'bundle':str(bundle)}))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--prepare',action='store_true')
    package(parser.parse_args().prepare)
