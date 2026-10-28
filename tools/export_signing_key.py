"""Explicit private key export. Never include its output in a public ZIP."""
import argparse
from pathlib import Path
import shutil
from sign_apk import key_environment

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--private-destination', required=True, type=Path)
    a = p.parse_args()
    if a.private_destination.exists():
        raise SystemExit('Destination already exists; refusing overwrite')
    data, env = key_environment()
    a.private_destination.mkdir(parents=True)
    shutil.copy2(data['keystore'], a.private_destination / 'n3-manual-release.p12')
    (a.private_destination / 'PRIVATE-PASSWORD.txt').write_text(env['N3_RELEASE_PASSWORD'], encoding='ascii')
    print('Private signing recovery exported. DO NOT PUBLISH OR SHARE THIS FOLDER.')
