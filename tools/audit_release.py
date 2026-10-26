#!/usr/bin/env python3
"""Inspect distributable bytes without printing matched private values."""
import argparse
import getpass
import io
import json
import os
from pathlib import Path
import re
import zipfile

PATTERNS = {
    'local-user-path': re.compile(rb'(?:[A-Za-z]:[\\/](?:Users|Documents and Settings)[\\/][^\s<>"\x00]+|/(?:Users|home)/[^\s<>"\x00]+)', re.I),
    'private-key': re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    'credential-pattern': re.compile(rb'(?:ghp_|github_pat_|AIza)[A-Za-z0-9_-]{20,}'),
}
PRIVATE_NAMES = {'.git', '.gradle', '.env', 'local.properties'}
PRIVATE_SUFFIXES = {'.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.bak', '.log'}

def private_tokens():
    values = os.environ.get('QUIETGATE_PRIVATE_IDENTIFIERS', '').splitlines()
    username = getpass.getuser()
    if username.lower() not in {'root', 'runner', 'user', 'developer', 'admin', 'administrator'}:
        values.append(username)
    values.append(os.environ.get('COMPUTERNAME', ''))
    return [value for value in set(values) if len(value) >= 4]

def inspect_bytes(data, label, depth=0):
    findings = []
    for name, pattern in PATTERNS.items():
        if pattern.search(data):
            findings.append({'file': label, 'rule': name})
    for token in private_tokens():
        if any(value.lower() in data.lower() for value in (token.encode('utf-8'), token.encode('utf-16le'))):
            findings.append({'file': label, 'rule': 'private-identifier'})
            break
    if data[:4] in (b'PK\x03\x04', b'PK\x05\x06'):
        if depth >= 5:
            return findings + [{'file': label, 'rule': 'archive-nesting-limit'}]
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            findings += inspect_bytes(archive.comment, label + '!comment', depth + 1)
            for info in archive.infolist():
                path = Path(info.filename)
                if PRIVATE_NAMES.intersection(path.parts) or path.suffix.lower() in PRIVATE_SUFFIXES:
                    findings.append({'file': label + '!' + info.filename, 'rule': 'private-file'})
                findings += inspect_bytes(info.filename.encode() + info.extra + info.comment, label + '!metadata', depth + 1)
                if not info.is_dir():
                    findings += inspect_bytes(archive.read(info), label + '!' + info.filename, depth + 1)
    return findings

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('files', nargs='+', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    findings = []
    for path in args.files:
        findings += inspect_bytes(path.read_bytes(), path.name)
    result = {'filesChecked': len(args.files), 'result': 'FAIL' if findings else 'PASS', 'findings': findings,
              'scope': 'Targeted identifier, credential-pattern, and recursive archive inspection; not a guarantee of anonymity.'}
    text = json.dumps(result, indent=2) + '\n'
    if args.output:
        args.output.write_text(text, encoding='utf-8')
    print(text)
    raise SystemExit(bool(findings))

if __name__ == '__main__':
    main()
