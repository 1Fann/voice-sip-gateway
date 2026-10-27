"""Stage exact upstream native binaries and the published Java bindings."""
from pathlib import Path
import hashlib, json, shutil, zipfile
from download import retrieve
root=Path(__file__).resolve().parents[1]
m=json.loads((root/'third_party/upstream-native.json').read_text())
apk=retrieve(m['apk_url'],m['apk_sha256'],root/'.work/upstream-v0.3.5.apk')
output=root/'sip-pjsua2/src/generated'
with zipfile.ZipFile(apk) as z:
    for name,sha in m['libraries'].items():
        binary=z.read('lib/arm64-v8a/'+name)
        assert hashlib.sha256(binary).hexdigest()==sha
        p=output/'jniLibs/arm64-v8a'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(binary)
shutil.copytree(root/'third_party/pjsua2-java',output/'java',dirs_exist_ok=True)
print('Verified upstream ARM64 libraries and staged Java bindings.')
