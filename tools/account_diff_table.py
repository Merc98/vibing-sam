#!/usr/bin/env python3
"""Build a reconciliation table for account+location values across files.

By default it prints the complete merged data.
Use --only-differences to show only rows where results conflict.

Supported inputs: .csv, .json (list of objects), and .txt logs with lines similar to:
***6014531444572197=491212058001? 0807   23-04   ok
"""

from __future__ import annotations

import argparse
import csv
import json
import re
from collections import defaultdict
from pathlib import Path
from typing import Iterable

LINE_RE = re.compile(r"^\*+(?P<account_left>\d+)\s*=\s*(?P<account_right>[0-9?]+)\s+(?P<tail>.+)$")
LOCATION_RE = re.compile(r"\b\d{2,4}-+\d{2}\b")


RESULT_TOKENS = {"ok", "error", "err", "fail", "failed", "?", "xxxx", "????"}


def parse_text_line(line: str) -> dict[str, str] | None:
    """Extract structured fields from a raw reconciliation line."""
    stripped = line.strip()
    if not stripped:
        return None

    match = LINE_RE.match(stripped)
    if not match:
        return None

    account_left = match.group("account_left")
    account_right = match.group("account_right")
    tail = match.group("tail")

    location = ""
    location_match = LOCATION_RE.search(tail)
    if location_match:
        location = location_match.group(0).replace("--", "-")

    tokens = tail.split()
    status = ""
    for token in reversed(tokens):
        normalized = token.strip().lower()
        if normalized in RESULT_TOKENS:
            status = token
            break

    extras = []
    for token in tokens:
        cleaned = token.strip()
        if not cleaned:
            continue
        if location and cleaned.replace("--", "-") == location:
            continue
        if status and cleaned == status:
            continue
        extras.append(cleaned)

    return {
        "account_left": account_left,
        "account_right": account_right,
        "location_code": location,
        "status": status,
        "extra": " ".join(extras).strip(),
        "raw": stripped,
    }


def load_rows(path: Path) -> list[dict[str, str]]:
    suffix = path.suffix.lower()
    if suffix == ".csv":
        with path.open("r", encoding="utf-8-sig", newline="") as file_obj:
            return list(csv.DictReader(file_obj))
    if suffix == ".json":
        with path.open("r", encoding="utf-8") as file_obj:
            data = json.load(file_obj)
        if isinstance(data, list):
            return [row for row in data if isinstance(row, dict)]
        raise ValueError(f"JSON file must contain a list of objects: {path}")
    if suffix == ".txt":
        rows: list[dict[str, str]] = []
        with path.open("r", encoding="utf-8", errors="ignore") as file_obj:
            for line in file_obj:
                row = parse_text_line(line)
                if row:
                    rows.append(row)
        return rows
    raise ValueError(f"Unsupported file type for {path}. Use .csv, .json, or .txt")


def normalize(value: object) -> str:
    return str(value).strip()


def to_markdown(headers: list[str], rows: Iterable[list[str]]) -> str:
    lines = ["| " + " | ".join(headers) + " |", "|" + "|".join(["---"] * len(headers)) + "|"]
    for row in rows:
        safe = [cell.replace("|", "\\|") for cell in row]
        lines.append("| " + " | ".join(safe) + " |")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("files", nargs="+", help="Input CSV/JSON/TXT documents")
    parser.add_argument(
        "--account-col",
        default="account_left",
        help="Account column name (for TXT, default is account_left)",
    )
    parser.add_argument(
        "--location-col",
        default="location_code",
        help="Location column name (for TXT, default is location_code)",
    )
    parser.add_argument(
        "--result-col",
        default="status",
        help="Result/status column name (for TXT, default is status)",
    )
    parser.add_argument(
        "--only-differences",
        action="store_true",
        help="Show only account+location keys with conflicting results across documents",
    )
    args = parser.parse_args()

    grouped: dict[tuple[str, str], list[tuple[str, str, str]]] = defaultdict(list)

    for raw_path in args.files:
        path = Path(raw_path)
        rows = load_rows(path)
        for row in rows:
            account = normalize(row.get(args.account_col, ""))
            location = normalize(row.get(args.location_col, ""))
            result = normalize(row.get(args.result_col, ""))
            extra = normalize(row.get("extra", ""))
            if not account or not location:
                continue
            grouped[(account, location)].append((path.name, result, extra))

    table_rows = []
    for (account, location), entries in grouped.items():
        distinct_results = sorted({result for _, result, _ in entries if result})
        is_conflict = len(distinct_results) > 1
        if args.only_differences and not is_conflict:
            continue

        details = []
        for file_name, result, extra in entries:
            pieces = [f"{file_name}: {result or '[vacío]'}"]
            if extra:
                pieces.append(f"extra={extra}")
            details.append(" (".join([pieces[0], ", ".join(pieces[1:]) + ")"]) if len(pieces) > 1 else pieces[0])

        status = "DIFERENTE" if is_conflict else "IGUAL"
        table_rows.append(
            [
                account,
                location,
                status,
                ", ".join(distinct_results) if distinct_results else "[vacío]",
                "; ".join(details),
            ]
        )

    table_rows.sort(key=lambda row: (row[0], row[1]))

    if not table_rows:
        if args.only_differences:
            print("No se encontraron diferencias de resultado para la misma cuenta+ubicación.")
        else:
            print("No se encontraron filas válidas con cuenta+ubicación en los documentos.")
        return 0

    headers = [
        "Cuenta",
        "Ubicación",
        "Estado",
        "Resultados únicos",
        "Detalle por documento",
    ]
    print(to_markdown(headers, table_rows))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
