#!/usr/bin/env python3
"""Writes docs/modules/ROOT/pages/deviations.adoc from docs/deviations.md (the single source). Re-run after edits."""
import re
from pathlib import Path

root = Path(__file__).resolve().parents[2]
rows = []
for line in (root / "docs/deviations.md").read_text(encoding="utf-8").splitlines():
    if line.startswith("| ") and not line.startswith("| Area |"):
        cells = [c.strip() for c in line.strip().strip("|").split(" | ")]
        rows.append(cells)


def adoc(text):
    # Markdown inline code becomes AsciiDoc monospace; pipes inside cells are escaped
    return re.sub(r"`([^`]+)`", r"`+\1+`", text).replace("|", "\\|")


out = [
    "= Differences from IPT 3",
    ":page-toclevels: 0",
    "",
    "IPT 4 keeps IPT 3's pages, URLs and behaviour. These are the intentional differences.",
    "Generated from `docs/deviations.md` by `tools/docs/deviations-to-adoc.py`.",
    "",
    '[cols="2,3,3,3",options="header"]',
    "|===",
    "|Area |IPT 3 |IPT 4 |Why",
]
for cells in rows:
    out.append("")
    out.extend("|" + adoc(c) for c in cells)
out.append("|===")
(root / "docs/modules/ROOT/pages/deviations.adoc").write_text("\n".join(out) + "\n", encoding="utf-8")
print(f"{len(rows)} deviations")
