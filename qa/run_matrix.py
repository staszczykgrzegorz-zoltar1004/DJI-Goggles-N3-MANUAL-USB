"""Real Android Emulator matrix for the exact signed, non-debuggable public APK.

Own disposable AVDs only, no physical phones, accounts, system driver changes or
startup entries. All emulator processes started here are stopped in finally.
"""
import argparse
import concurrent.futures as cf
import hashlib
import json
import os
from pathlib import Path
import queue
import re
import subprocess as sp
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parent
SDK = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'AppData/Local/Android/Sdk')))
ADB = SDK / 'platform-tools/adb.exe'
APK = PROJECT / 'release/public/1.0.0-beta1/N3Manual-1.0.0-beta1.apk'
TEST_APK = ROOT / 'signed-tests.apk'
PORTS = queue.Queue()
for port in (5580, 5582): PORTS.put(port)


def command(args, timeout=30, check=True, binary=False):
    result = sp.run([str(a) for a in args], capture_output=True,
                    text=not binary, encoding=None if binary else 'utf8',
                    errors=None if binary else 'replace', timeout=timeout,
                    creationflags=sp.CREATE_NO_WINDOW)
    if check and result.returncode:
        raise RuntimeError(f'Command failed: {args[:4]}: {str(result.stdout)[-1800:]} {str(result.stderr)[-1000:]}')
    return result.stdout if binary else result.stdout + result.stderr


def avd(api):
    image = SDK / f'system-images/android-{api}/default/x86_64'
    until = time.monotonic() + 1800
    while not (image / 'package.xml').exists():
        if time.monotonic() > until: raise TimeoutError('Verified SDK image not ready')
        time.sleep(2)
    # API36's initial userdata was interrupted by the host disk becoming full.
    # Keep that old image intact; use a separate clean disposable image.
    suffix = '_clean' if api == 36 else ''
    folder = ROOT / 'avds' / f'N3Manual_API{api}{suffix}.avd'
    folder.mkdir(parents=True, exist_ok=True)
    (folder.parent / f'N3Manual_API{api}.ini').write_text(
        f'avd.ini.encoding=UTF-8\npath={folder}\ntarget=android-{api}\n', encoding='utf8')
    (folder / 'config.ini').write_text(
        f'AvdId=N3Manual_API{api}\navd.ini.displayname=N3 Manual QA API {api}\n'
        f'abi.type=x86_64\ntag.id=default\nimage.sysdir.1={image}\\\n'
        'hw.cpu.arch=x86_64\nhw.cpu.ncore=2\nhw.ramSize=2048\n'
        'hw.lcd.width=720\nhw.lcd.height=1280\nhw.lcd.density=320\n'
        'hw.gpu.enabled=yes\nhw.gpu.mode=swiftshader\nhw.keyboard=yes\n'
        'disk.dataPartition.size=2048M\nshowDeviceFrame=no\n', encoding='utf8')
    return folder


def run_api(api):
    folder = ROOT / 'results' / f'api{api}'
    folder.mkdir(parents=True, exist_ok=True)
    report = {'api': api, 'apk_sha256': hashlib.sha256(APK.read_bytes()).hexdigest(),
              'physical_usb_tested': False, 'checks': []}
    process = None; serial = None; port = None
    try:
        avd(api)
        port = PORTS.get(); serial = f'emulator-{port}'
        env = os.environ.copy(); env['ANDROID_AVD_HOME'] = str(ROOT / 'avds');env['ANDROID_HOME'] = str(SDK)
        log = (folder / 'emulator.log').open('wb')
        process = sp.Popen([str(SDK / 'emulator/emulator.exe'), '-avd', f'N3Manual_API{api}',
            '-port', str(port), '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot',
            '-feature', '-QuickbootFileBacked',
            '-gpu', 'swiftshader', '-accel', 'on', '-memory', '2048', '-cores', '2'],
            env=env, stdout=log, stderr=sp.STDOUT, creationflags=sp.CREATE_NO_WINDOW)
        print(f'BOOT API {api} {serial}', flush=True)
        started = time.monotonic(); deadline = started + 420
        while time.monotonic() < deadline:
            result = command([ADB, '-s', serial, 'shell', 'getprop', 'sys.boot_completed'], check=False, timeout=10)
            if result.strip() == '1': break
            if process.poll() is not None:
                raise RuntimeError('Emulator exited: ' + (folder / 'emulator.log').read_text(errors='replace')[-2500:])
            time.sleep(2)
        else: raise TimeoutError('Android boot timed out')
        report['boot_seconds'] = round(time.monotonic() - started, 2)
        def adb(*args, timeout=40, check=True, binary=False):
            return command([ADB, '-s', serial, *args], timeout, check, binary)
        adb('shell','input','keyevent','224');adb('shell','wm','dismiss-keyguard',check=False)
        report['abi'] = adb('shell','getprop','ro.product.cpu.abi').strip()
        report['android'] = adb('shell','getprop','ro.build.version.release').strip()
        for package in (APK,TEST_APK):
            output = adb('install','-r',str(package),timeout=90)
            if 'Success' not in output: raise RuntimeError('Install: ' + output)
        report['checks'].append('install exact signed release and matching test APK')
        instrumentation = adb('shell','am','instrument','-w','-r',
            'pl.stasz.n3manual.test/pl.stasz.n3manual.SmokeRunner', timeout=120)
        (folder / 'instrumentation.txt').write_text(instrumentation, encoding='utf8')
        if 'N3_QA_PASS' not in instrumentation: raise RuntimeError(instrumentation[-4500:])
        match = re.search(r'N3_QA_PASS api=\d+ checks=(\d+) core=(\d+)', instrumentation)
        report['runtime_checks'] = int(match[1]);report['core_assertions'] = int(match[2])
        report['checks'].append('framework lifecycle, permission denial, stale Confirm, recreation and protocol')
        def ui():
            adb('shell','uiautomator','dump','/sdcard/n3-qa.xml')
            raw = adb('exec-out','cat','/sdcard/n3-qa.xml',binary=True)
            nodes = list(ET.fromstring(raw).iter('node'))
            if not any(n.get('package')=='pl.stasz.n3manual' for n in nodes):
                raise RuntimeError('Own application not visible')
            return nodes, raw
        def tap(nodes,label):
            matches=[n for n in nodes if n.get('text','').casefold()==label.casefold() and n.get('enabled')=='true']
            if len(matches)!=1:raise RuntimeError('Missing visible control: '+label)
            a,b,c,d=map(int,re.findall(r'\d+',matches[0].get('bounds')))
            adb('shell','input','tap',str((a+c)//2),str((b+d)//2))
        adb('shell','am','force-stop','pl.stasz.n3manual')
        adb('shell','am','start','-W','-n','pl.stasz.n3manual/.MainActivity')
        nodes,raw=ui();(folder/'portrait.xml').write_bytes(raw)
        (folder/'portrait.png').write_bytes(adb('exec-out','screencap','-p',binary=True))
        tap(nodes,'Połącz i sprawdź USB');nodes,raw=ui()
        if not any(n.get('text')=='Diagnostyka USB' for n in nodes):raise RuntimeError('Missing diagnostic dialog')
        (folder/'diagnostics.xml').write_bytes(raw)
        tap(nodes,'Zamknij');report['checks'].append('USB absent gives diagnostics, not silent help')
        adb('shell','settings','put','system','font_scale','1.3')
        adb('shell','am','force-stop','pl.stasz.n3manual');adb('shell','am','start','-W','-n','pl.stasz.n3manual/.MainActivity')
        nodes,raw=ui();(folder/'large-font.xml').write_bytes(raw)
        (folder/'large-font.png').write_bytes(adb('exec-out','screencap','-p',binary=True))
        adb('shell','settings','put','system','font_scale','1.0')
        adb('shell','settings','put','system','accelerometer_rotation','0')
        adb('shell','settings','put','system','user_rotation','1');time.sleep(2)
        nodes,raw=ui();(folder/'landscape.xml').write_bytes(raw)
        (folder/'landscape.png').write_bytes(adb('exec-out','screencap','-p',binary=True))
        report['checks'].append('portrait, 130% font, landscape remain usable and scrollable')
        report['status']='PASS'
        print(f'PASS API {api}: {report["runtime_checks"]} runtime + {report["core_assertions"]} core',flush=True)
    except Exception as error:
        report['status']='FAIL';report['error']=str(error)
        print(f'FAIL API {api}: {str(error)[:900]}',flush=True)
    finally:
        if serial:
            try:command([ADB,'-s',serial,'emu','kill'],timeout=15,check=False)
            except Exception:pass
        if process:
            try:process.wait(timeout=15)
            except sp.TimeoutExpired:process.terminate();process.wait(timeout=10)
        if port is not None:PORTS.put(port)
        (folder/'result.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--apis',nargs='+',type=int,default=[23,35,36,27,29,31,33,34])
    args=p.parse_args()
    if any(a not in (23,27,29,31,33,34,35,36) for a in args.apis):raise SystemExit('Unexpected API')
    with cf.ThreadPoolExecutor(max_workers=1) as pool:results=list(pool.map(run_api,args.apis))
    (ROOT/'matrix.json').write_text(json.dumps(results,indent=2),encoding='utf8')
    print(json.dumps({'passed':sum(r['status']=='PASS' for r in results),'total':len(results)}),flush=True)
