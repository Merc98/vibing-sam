#!/usr/bin/env python3
"""Build a reconciliation table for account+location values across files.

By default it prints the complete merged data.
Use --only-differences to show only rows where results conflict.

Usage:
  python tools/account_diff_table.py file1.csv file2.csv \
    --account-col account --location-col location --result-col result
"""

from __future__ import annotations

import argparse
import csv
import json
from collections import defaultdict
from pathlib import Path
from typing import Iterable


def load_rows(path: Path) -> list[dict[str, str]]:
    suffix = path.suffix.lower()
    if suffix == ".csv":
        with path.open("r", encoding="utf-8-sig", newline="") as f:
            return list(csv.DictReader(f))
    if suffix == ".json":
        with path.open("r", encoding="utf-8") as f:
            data = json.load(f)
        if isinstance(data, list):
            return [r for r in data if isinstance(r, dict)]
        raise ValueError(f"JSON file must contain a list of objects: {path}")
    raise ValueError(f"Unsupported file type for {path}. Use .csv or .json")


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
    parser.add_argument("files", nargs="+", help="Input CSV/JSON documents")
    parser.add_argument("--account-col", default="account", help="Account number column name")
    parser.add_argument("--location-col", default="location", help="Location column name")
    parser.add_argument("--result-col", default="result", help="Result column name")
    parser.add_argument(
        "--only-differences",
        action="store_true",
        help="Show only account+location keys with conflicting results across documents",
    )
    args = parser.parse_args()

    grouped: dict[tuple[str, str], list[tuple[str, str]]] = defaultdict(list)

    for raw_path in args.files:
        path = Path(raw_path)
        rows = load_rows(path)
        for row in rows:
            account = normalize(row.get(args.account_col, ""))
            location = normalize(row.get(args.location_col, ""))
            result = normalize(row.get(args.result_col, ""))
            if not account or not location:
                continue
            grouped[(account, location)].append((path.name, result))

    table_rows = []
    for (account, location), entries in grouped.items():
        distinct_results = sorted({result for _, result in entries if result})
        is_conflict = len(distinct_results) > 1
        if args.only_differences and not is_conflict:
            continue

        by_file = "; ".join(f"{fname}: {res or '[vacío]'}" for fname, res in entries)
        status = "DIFERENTE" if is_conflict else "IGUAL"
        table_rows.append([
            account,
            location,
            status,
            ", ".join(distinct_results) if distinct_results else "[vacío]",
            by_file,
        ])

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
