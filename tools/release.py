#!/usr/bin/env python3
"""Create a versioned local release without building or publishing it."""
import argparse, hashlib, json, re, shutil, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ["README.md", "PRIVACY.md", "PERMISSIONS.md", "FOSS.md", "AI-DISCLOSURE.md", "CHANGELOG.md", "DEVICE-VERIFICATION.md", "LICENSE", "RELEASING.md"]
EXCLUDED = {".git", ".gradle", "build", ".idea", "__pycache__", "releases"}
def allowed(path):
    rel = path.relative_to(ROOT)
    return not any(p in EXCLUDED for p in rel.parts) and path.name != "local.properties" and path.suffix.lower() not in {".apk", ".apks", ".jks", ".keystore", ".log", ".pyc", ".zip"}
def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--output", type=Path, default=ROOT.parent / "QuietGate-releases")
    args = parser.parse_args()
    config = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
    version = re.search(r"versionName ['\"]([^'\"]+)", config).group(1)
    code = int(re.search(r"versionCode (\d+)", config).group(1))
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", version):
        parser.error("Invalid version name")
    apk = args.apk.resolve()
    if not apk.is_file() or not zipfile.is_zipfile(apk):
        parser.error("Provide an existing APK archive")
    output = args.output.resolve()
    if output == ROOT or ROOT in output.parents:
        parser.error("Release archives must be outside the source tree")
    target = output / version
    target.mkdir(parents=True, exist_ok=False)
    sources = [p for p in sorted(ROOT.rglob("*")) if p.is_file() and not p.is_symlink() and allowed(p)]
    for p in sources:
        dest = target / "source" / p.relative_to(ROOT)
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(p, dest)
    with zipfile.ZipFile(target / "source.zip", "w", zipfile.ZIP_DEFLATED) as z:
        for p in sources:
            info = zipfile.ZipInfo("QuietGate/" + p.relative_to(ROOT).as_posix(), (2000,1,1,0,0,0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0
            z.writestr(info, p.read_bytes())
    shutil.copyfile(apk, target / ("QuietGate-" + version + ".apk"))
    for name in DOCS:
        shutil.copyfile(ROOT / name, target / name)
    metadata = {"app":"QuietGate", "versionName":version, "versionCode":code, "package":"org.open.quietgate", "primaryLanguage":"Java", "runtimeAI":False, "buildType":"development/debug", "verification":"See DEVICE-VERIFICATION.md; no new tests performed by packaging"}
    (target / "release.json").write_text(json.dumps(metadata, indent=2)+"\n", encoding="utf-8")
    with (target / "SHA256SUMS.txt").open("w", encoding="utf-8", newline="\n") as out:
        for p in sorted(target.rglob("*")):
            if p.is_file() and p.name != "SHA256SUMS.txt":
                out.write(hashlib.sha256(p.read_bytes()).hexdigest()+"  "+p.relative_to(target).as_posix()+"\n")
    print(target)
if __name__ == "__main__":
    main()
