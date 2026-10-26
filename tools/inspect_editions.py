#!/usr/bin/env python3
"""Inspect compiled edition APKs without installing them or touching a device."""
import argparse, hashlib, json, re, struct, subprocess, zipfile
from pathlib import Path

def dex_types(data):
    if not data.startswith(b"dex\n"):
        raise ValueError("Unsupported DEX container")
    strings_size, strings_off = struct.unpack_from("<II", data, 0x38)
    types_size, types_off = struct.unpack_from("<II", data, 0x40)
    strings = []
    for i in range(strings_size):
        at = struct.unpack_from("<I", data, strings_off + 4*i)[0]
        while data[at] & 0x80:
            at += 1
        at += 1
        strings.append(data[at:data.index(0, at)].decode("utf-8", errors="replace"))
    return {strings[struct.unpack_from("<I", data, types_off + 4*i)[0]] for i in range(types_size)}

def inspect(apk, edition, aapt):
    badging = subprocess.run([str(aapt), "dump", "badging", str(apk)], check=True,
                             capture_output=True, text=True, encoding="utf-8").stdout
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if not package:
        raise ValueError("APK package metadata unavailable")
    expected = "org.open.quietgate" + (".foss" if edition == "foss" else "")
    if package[1] != expected:
        raise ValueError("Wrong application ID")
    permissions = sorted(re.findall(r"uses-permission: name='([^']+)'", badging))
    has_internet = "android.permission.INTERNET" in permissions
    if has_internet != (edition == "google"):
        raise ValueError("Incorrect edition Internet permission")
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        if archive.read("assets/EDITION.txt").decode().strip() != edition:
            raise ValueError("Edition marker mismatch")
        types = set()
        for name in names:
            if re.fullmatch(r"classes\d*\.dex", name):
                types.update(dex_types(archive.read(name)))
        google_types = sorted(t for t in types if t.startswith("Lcom/google/"))
        integration = sorted(t for t in types if t.startswith("Lorg/open/quietgate/Google"))
        google_notice = "assets/GOOGLE-AUTH-THIRD-PARTY-LICENSES.txt" in names
        google_help = "assets/help/GOOGLE.txt" in names
        if edition == "foss" and (google_types or integration or google_notice or google_help):
            raise ValueError("Google integration found in FOSS artifact")
        if edition == "google" and not (google_types and integration and google_notice and google_help):
            raise ValueError("Google edition lost its integration")
        for asset in ["LICENSE", "SHIZUKU-LICENSE.txt", "APACHE-2.0.txt", "THIRD_PARTY_NOTICES.txt", "CHANGELOG.md"]:
            if not archive.read("assets/" + asset).strip():
                raise ValueError("Missing license/changelog document")
    # Verify the packaged foreground service declaration rather than relying only on lint.
    manifest = subprocess.run([str(aapt), "dump", "xmltree", str(apk), "AndroidManifest.xml"],
                              check=True, capture_output=True, text=True, encoding="utf-8").stdout
    if "org.open.quietgate.GuardNotificationService" not in manifest or "foregroundServiceType" not in manifest:
        raise ValueError("Foreground service type absent")
    return {"edition": edition, "package": package[1], "versionCode": int(package[2]),
            "versionName": package[3], "bytes": apk.stat().st_size,
            "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(), "permissions": permissions,
            "googleTypeReferences": len(google_types), "googleIntegrationTypeReferences": len(integration),
            "googleNoticesIncluded": google_notice, "googleHelpIncluded": google_help,
            "debuggable": "application-debuggable" in badging, "result": "PASS",
            "scope": "Compiled APK inspection only; no device/UI/live-call verification"}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--aapt", required=True, type=Path)
    parser.add_argument("--foss", required=True, type=Path)
    parser.add_argument("--google", required=True, type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    report = [inspect(args.foss, "foss", args.aapt), inspect(args.google, "google", args.aapt)]
    if report[0]["versionCode"] != report[1]["versionCode"] or report[0]["versionName"] != report[1]["versionName"]:
        raise ValueError("Edition versions disagree")
    text = json.dumps(report, indent=2) + "\n"
    if args.output:
        args.output.write_text(text, encoding="utf-8")
    print(text)

if __name__ == "__main__":
    main()
