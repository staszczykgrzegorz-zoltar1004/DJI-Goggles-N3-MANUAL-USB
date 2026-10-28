"""Check complete PL/EN resource coverage and every typed error/progress key."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
def strings(folder):
    return {n.attrib['name']:n.text or '' for n in ET.parse(ROOT/f'app/src/main/res/{folder}/strings.xml').getroot()}
en,pl=strings('values'),strings('values-pl')
assert en.keys()==pl.keys(), 'Different resource key sets'
for key in en:
    assert sorted(re.findall(r'%\d+\$[sd]',en[key]))==sorted(re.findall(r'%\d+\$[sd]',pl[key])),key
    assert en[key].strip('"') and pl[key].strip('"'),key
java=ROOT/'app/src/main/java/pl/stasz/n3manual'
source='\n'.join(p.read_text(encoding='utf8') for p in java.glob('*.java'))
for key in re.findall(r'"((?:error_|progress_)[a-z_]+)"',source):assert key in en,key
for key in re.findall(r'R\.string\.([a-z_]+)',source):assert key in en,key
assert not re.search(r'"[^"\n]*[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ][^"\n]*"',source), 'Polish literal remains in production Java'
print(f'PASS: {len(en)} English/Polish strings; placeholders and all production references match')
