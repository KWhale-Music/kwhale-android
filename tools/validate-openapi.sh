#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

python3 - <<'PY'
import sys
import yaml

with open("api/openapi.yaml", "r", encoding="utf-8") as f:
    spec = yaml.safe_load(f)

assert spec.get("openapi", "").startswith("3.1"), "OpenAPI 3.1 required"
assert spec.get("servers"), "servers required"
assert spec.get("paths"), "paths required"

try:
    from openapi_spec_validator import validate
    validate(spec)
    print("OK: openapi.yaml valid (openapi-spec-validator)")
except ImportError:
    print("OK: openapi.yaml parsed (install openapi-spec-validator for full validation)")
except Exception as exc:
    print(f"INVALID: {exc}", file=sys.stderr)
    raise
PY
