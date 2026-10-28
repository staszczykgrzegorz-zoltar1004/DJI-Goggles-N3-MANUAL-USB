"""Sign an APK with a persistent private release key, outside all public bundles.

Windows credentials protect the key password with DPAPI. Key/password values
are passed to child tools via the environment, never printed or put in argv.
"""
import argparse
import base64
import ctypes as C
from ctypes import wintypes as W
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TOOLS = Path(os.environ.get('N3_ANDROID_TOOLS', str(Path.home() / 'Documents/apk pogoda/.tools')))
PRIVATE = Path.home() / '.codex/private-artifacts/N3Manual'


class Blob(C.Structure):
    _fields_ = [('size', W.DWORD), ('data', C.POINTER(C.c_ubyte))]


def dpapi(value, protect):
    buffer = (C.c_ubyte * len(value)).from_buffer_copy(value)
    source, target = Blob(len(value), buffer), Blob()
    if protect:
        ok = C.windll.crypt32.CryptProtectData(C.byref(source), 'N3 Manual signing', None, None, None, 1, C.byref(target))
    else:
        ok = C.windll.crypt32.CryptUnprotectData(C.byref(source), None, None, None, None, 1, C.byref(target))
    if not ok:
        raise C.WinError()
    try:
        return C.string_at(target.data, target.size)
    finally:
        C.windll.kernel32.LocalFree(target.data)


def key_environment():
    PRIVATE.mkdir(parents=True, exist_ok=True)
    # Limit only this newly created private artifact folder, never user/system roots.
    sid = subprocess.check_output(['powershell.exe', '-NoProfile', '-Command',
        '[System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value'], text=True).strip()
    subprocess.run(['icacls', str(PRIVATE), '/inheritance:r', '/grant:r',
                    '*' + sid + ':(OI)(CI)F', '*S-1-5-18:(OI)(CI)F'], check=True, capture_output=True)
    config = PRIVATE / 'signing.json'
    env = os.environ.copy()
    if config.exists():
        data = json.loads(config.read_text(encoding='utf8'))
        password = dpapi(base64.b64decode(data['protected_password']), False).decode('ascii')
    else:
        password = secrets.token_urlsafe(36)
        data = {'keystore': str(PRIVATE / 'n3-manual-release.p12'), 'alias': 'n3-manual-release',
                'protected_password': base64.b64encode(dpapi(password.encode('ascii'), True)).decode('ascii')}
        if Path(data['keystore']).exists():
            raise RuntimeError('Existing key without configuration: refuse replacement')
        env['N3_RELEASE_PASSWORD'] = password
        subprocess.run([str(TOOLS / 'jdk-17/bin/keytool.exe'), '-genkeypair', '-noprompt',
            '-storetype', 'PKCS12', '-keystore', data['keystore'], '-alias', data['alias'],
            '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
            '-dname', os.environ.get('N3_SIGNING_DNAME', 'CN=Local N3 Manual build'),
            '-storepass:env', 'N3_RELEASE_PASSWORD', '-keypass:env', 'N3_RELEASE_PASSWORD'],
            env=env, check=True, capture_output=True)
        config.write_text(json.dumps(data, indent=2), encoding='utf8')
        (PRIVATE / 'NIE_UDOSTEPNIAJ.txt').write_text(
            'PRYWATNY KLUCZ AKTUALIZACJI N3 MANUAL. Nie publikuj tego folderu.\n'
            'Haslo chronione DPAPI: zachowaj profil Windows. Przed zmiana komputera '
            'wyeksportuj przenosna kopie z tools/export_signing_key.py na bezpieczny nosnik.\n', encoding='utf8')
    env['N3_RELEASE_PASSWORD'] = password
    env['JAVA_HOME'] = str(TOOLS / 'jdk-17')
    env['PATH'] = str(TOOLS / 'jdk-17/bin') + os.pathsep + env.get('PATH', '')
    return data, env


def sign(source, destination):
    data, env = key_environment()
    destination.parent.mkdir(parents=True, exist_ok=True)
    aligned = destination.with_suffix('.aligned.tmp.apk')
    binaries = TOOLS / 'android-sdk/build-tools/35.0.0'
    subprocess.run([str(binaries / 'zipalign.exe'), '-f', '-p', '4', str(source), str(aligned)], check=True, env=env)
    signer = [str(TOOLS / 'jdk-17/bin/java.exe'), '-jar', str(binaries / 'lib/apksigner.jar')]
    subprocess.run(signer + ['sign', '--ks', data['keystore'],
        '--ks-type', 'PKCS12', '--ks-key-alias', data['alias'], '--ks-pass', 'env:N3_RELEASE_PASSWORD',
        '--key-pass', 'env:N3_RELEASE_PASSWORD', '--v1-signing-enabled', 'true',
        '--v2-signing-enabled', 'true', '--v3-signing-enabled', 'true',
        '--out', str(destination), str(aligned)], check=True, env=env)
    result = subprocess.check_output(signer + ['verify', '--verbose', '--print-certs', str(destination)], env=env, text=True)
    aligned.unlink()
    env.pop('N3_RELEASE_PASSWORD', None)
    return {'apk': destination.name, 'bytes': destination.stat().st_size,
            'sha256': hashlib.sha256(destination.read_bytes()).hexdigest(),
            'signature_verification': result}


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input', type=Path, default=ROOT / 'app/build/outputs/apk/release/app-release-unsigned.apk')
    p.add_argument('--output', type=Path, default=ROOT / 'release/public/1.0.0-beta1/N3Manual-1.0.0-beta1.apk')
    args = p.parse_args()
    result = sign(args.input.resolve(), args.output.resolve())
    (args.output.parent / (args.output.stem + '-signature.json')).write_text(json.dumps(result, indent=2), encoding='utf8')
    print(json.dumps({k: v for k, v in result.items() if k != 'signature_verification'}))
