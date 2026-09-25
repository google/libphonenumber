#!/usr/bin/env bash
# Compile phonenumber.kotoba with kotoba 0.7.2 and assert accept/reject fixtures.
set -euo pipefail

root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
cd "$root"

if ! command -v kotoba >/dev/null 2>&1; then
  echo "kotoba CLI is required (kotoba-lang/kotoba v0.7.2)" >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to parse kotoba --json and check wasm/fixtures" >&2
  exit 1
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

python3 - "$root" <<'PY'
import pathlib, sys
root = pathlib.Path(sys.argv[1])
fixtures = {
    "kotoba/fixtures/us-e164.txt": "+16502530000",
    "kotoba/fixtures/gb-e164.txt": "+442070313000",
    "kotoba/fixtures/us-international.txt": "+1 650 253 0000",
    "kotoba/fixtures/us-nsn.txt": "6502530000",
}


def e164_looking(s: str) -> bool:
    if not (3 <= len(s) <= 16):
        return False
    if s[0] != "+":
        return False
    return all("0" <= ch <= "9" for ch in s[1:])


for rel, expected in fixtures.items():
    got = (root / rel).read_text().rstrip("\n")
    if got != expected:
        raise SystemExit("%s is %r, expected %r" % (rel, got, expected))

accept_us = e164_looking(fixtures["kotoba/fixtures/us-e164.txt"])
accept_gb = e164_looking(fixtures["kotoba/fixtures/gb-e164.txt"])
reject_spaces = not e164_looking(fixtures["kotoba/fixtures/us-international.txt"])
reject_nsn = not e164_looking(fixtures["kotoba/fixtures/us-nsn.txt"])
print("fixture independent")
print("  accept +16502530000", accept_us)
print("  accept +442070313000", accept_gb)
print("  reject +1 650 253 0000", reject_spaces)
print("  reject 6502530000", reject_nsn)
if not (accept_us and accept_gb and reject_spaces and reject_nsn):
    raise SystemExit("independent E.164-looking predicate disagreed with fixture table")
PY

compile_json=$(kotoba compile kotoba/phonenumber.kotoba --target wasm --output "$work/phonenumber.wasm" --json)
COMPILE_JSON="$compile_json" WASM_PATH="$work/phonenumber.wasm" python3 <<'PY'
import json, os
wasm_path = os.environ["WASM_PATH"]
d = json.loads(os.environ["COMPILE_JSON"])
ok = d.get("kotoba.cli/ok?")
code = d.get("kotoba.cli/code")
data = d.get("kotoba.cli/data") or {}
profile = data.get("value-profile")
compat = data.get("compatibility") or {}
print("wasm", ok, code, "profile", profile, "abi", compat.get("value-abi"), "target", compat.get("target"))
if not ok or code != "emitted":
    raise SystemExit(d.get("kotoba.cli/message") or "wasm compile failed")
if profile != "i64-v1":
    raise SystemExit("expected value-profile i64-v1, got %r" % (profile,))
if compat.get("value-abi") != "direct-v1":
    raise SystemExit("expected value-abi direct-v1")
if compat.get("target") != "wasm32-kotoba-v1":
    raise SystemExit("expected target wasm32-kotoba-v1")
blob = open(wasm_path, "rb").read()
if blob[:4] != b"\x00asm":
    raise SystemExit("not wasm magic")

def read_u32(buf, i):
    n = 0
    shift = 0
    while True:
        b = buf[i]
        i += 1
        n |= (b & 0x7F) << shift
        if b < 0x80:
            return n, i
        shift += 7

i = 8
imports = False
while i < len(blob):
    sid = blob[i]
    i += 1
    size, i = read_u32(blob, i)
    if sid == 2:
        imports = True
    i += size
if imports:
    raise SystemExit("wasm has an import section; host-independent i64-v1 must not")
print("wasm host-independent", len(blob), "bytes")
PY

run_json=$(kotoba compile kotoba/phonenumber.kotoba --target web --output "$work/phonenumber.mjs" --run --json)
RUN_JSON="$run_json" python3 <<'PY'
import json, os
d = json.loads(os.environ["RUN_JSON"])
ok = d.get("kotoba.cli/ok?")
code = d.get("kotoba.cli/code")
data = d.get("kotoba.cli/data") or {}
result = data.get("result")
profile = data.get("value-profile")
print("web", ok, code, "result", result, "profile", profile)
if not ok or code != "ran":
    raise SystemExit(d.get("kotoba.cli/message") or "web fixture run failed")
if profile != "i64-v1":
    raise SystemExit("expected value-profile i64-v1 on web run")
if result != 1:
    raise SystemExit("main returned %r, expected 1 (accept US/GB and reject spaces/NSN)" % (result,))
print("accept/reject fixture match ok")
PY
