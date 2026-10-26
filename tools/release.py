#!/usr/bin/env python3
"""Create a versioned local release without building or publishing it."""
import argparse, hashlib, json, re, shutil, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ["RELEASE-README.md", "README.md", "PRIVACY.md", "PERMISSIONS.md", "FOSS.md", "AI-DISCLOSURE.md", "CHANGELOG.md", "CHANGELOG-README.md", "SECURITY-REVIEW.md", "DEVICE-VERIFICATION.md", "LICENSE", "RELEASING.md", "GOOGLE-CONTACTS-SETUP.md", "EDITIONS.md", "F-DROID.md", "ARTIFACT-INSPECTION.json"]
EXCLUDED = {".git", ".gradle", "build", ".idea", "__pycache__", "releases"}
def allowed(path):
    rel = path.relative_to(ROOT)
    return not any(p in EXCLUDED for p in rel.parts) and path.name != "local.properties" and path.suffix.lower() not in {".apk", ".apks", ".jks", ".keystore", ".log", ".pyc", ".zip"}
def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--edition", required=True, choices=["google", "foss"])
    parser.add_argument("--output", type=Path, default=ROOT.parent / "QuietGate-releases")
    args = parser.parse_args()
    config = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
    version = re.search(r"versionName ['\"]([^'\"]+)", config).group(1)
    code = int(re.search(r"versionCode (\d+)", config).group(1))
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", version):
        parser.error("Invalid version name")
    for name in DOCS:
        doc = ROOT / name
        if not doc.is_file() or not doc.read_text(encoding="utf-8").strip():
            parser.error("Required release documentation missing or empty: " + name)
    edition_doc = ROOT / "docs/editions" / args.edition / "RELEASE-README.md"
    if not edition_doc.is_file() or not edition_doc.read_text(encoding="utf-8").strip():
        parser.error("Edition-specific release disclosure is missing")
    disclosure = edition_doc.read_text(encoding="utf-8")
    if version not in (ROOT / "CHANGELOG-README.md").read_text(encoding="utf-8").splitlines()[0]:
        parser.error("CHANGELOG-README.md must identify the current version")
    if (ROOT / "CHANGELOG.md").read_bytes() != (ROOT / "app/src/main/assets/CHANGELOG.md").read_bytes():
        parser.error("Bundled changelog must match CHANGELOG.md before building")
    if version not in disclosure.splitlines()[0]:
        parser.error("RELEASE-README.md heading must identify the current version")
    for heading in ["## Included files", "## Permissions and setup", "## Privacy and local data", "## FOSS and licenses", "## Languages and AI assistance", "## Changes in this release", "## Verification and limitations"]:
        if heading not in disclosure:
            parser.error("Missing release disclosure section: " + heading)
    apk = args.apk.resolve()
    if not apk.is_file() or not zipfile.is_zipfile(apk):
        parser.error("Provide an existing APK archive")
    with zipfile.ZipFile(apk) as archive:
        try:
            marker = archive.read("assets/EDITION.txt").decode("utf-8").strip()
            bundled_changelog = archive.read("assets/CHANGELOG.md")
        except KeyError:
            parser.error("APK lacks its edition marker or changelog")
        if marker != args.edition:
            parser.error("APK edition does not match --edition")
        if bundled_changelog != (ROOT / "CHANGELOG.md").read_bytes():
            parser.error("Rebuild the APK: its bundled changelog is stale")
        if args.edition == "foss":
            for name in archive.namelist():
                if name.endswith(".dex"):
                    dex = archive.read(name)
                    if b"Lcom/google/" in dex or b"Lorg/open/quietgate/Google" in dex:
                        parser.error("Google classes or descriptors found in the FOSS APK")
            if "assets/GOOGLE-AUTH-THIRD-PARTY-LICENSES.txt" in archive.namelist():
                parser.error("Google-only license asset found in the FOSS APK")
    output = args.output.resolve()
    if output == ROOT or ROOT in output.parents:
        parser.error("Release archives must be outside the source tree")
    target = output / version / args.edition
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
    stem = "QuietGate-" + args.edition + "-" + version
    shutil.copyfile(apk, target / (stem + ".apk"))
    for name in DOCS:
        shutil.copyfile(edition_doc if name == "RELEASE-README.md" else ROOT / name, target / name)
    with zipfile.ZipFile(target / (stem + "-documentation.zip"), "w", zipfile.ZIP_DEFLATED) as z:
        for name in DOCS:
            z.write(target / name, stem + "-documentation/" + name)
    metadata = {"app":"QuietGate", "edition":args.edition, "versionName":version, "versionCode":code, "package":"org.open.quietgate" + (".foss" if args.edition == "foss" else ""), "primaryLanguage":"Java", "runtimeAI":False, "buildType":"development/debug", "verification":"See DEVICE-VERIFICATION.md; no device tests performed for 0.12-alpha"}
    (target / "release.json").write_text(json.dumps(metadata, indent=2)+"\n", encoding="utf-8")
    with (target / "SHA256SUMS.txt").open("w", encoding="utf-8", newline="\n") as out:
        for p in sorted(target.rglob("*")):
            if p.is_file() and p.name != "SHA256SUMS.txt":
                out.write(hashlib.sha256(p.read_bytes()).hexdigest()+"  "+p.relative_to(target).as_posix()+"\n")
    print(target)
if __name__ == "__main__":
    main()
