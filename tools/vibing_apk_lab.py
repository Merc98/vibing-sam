#!/usr/bin/env python3
"""
Vibing APK Lab (safe mode)

Purpose:
- Inspect APK metadata for apps you own or are authorized to test.
- Validate local toolchain availability (apktool/jadx/zipalign/apksigner/java/adb).
- Create a clean workspace for manual review.

This script does NOT inject payloads, patch smali, or bypass protections.
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import Dict, List, Optional
from zipfile import ZipFile


TOOLS = ["apktool", "jadx", "zipalign", "apksigner", "java", "adb"]


def run(cmd: List[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, capture_output=True, text=True, check=False)


def detect_tools() -> Dict[str, str]:
    return {tool: (shutil.which(tool) or "missing") for tool in TOOLS}


def apk_basic_info(apk_path: Path) -> Dict[str, object]:
    with ZipFile(apk_path, "r") as zf:
        names = zf.namelist()
        native_libs = sorted({name.split("/")[1] for name in names if name.startswith("lib/") and name.count("/") >= 2})
        has_manifest = "AndroidManifest.xml" in names
        dex_files = sorted([name for name in names if name.endswith(".dex")])
        resources_count = sum(1 for name in names if name.startswith("res/"))
        return {
            "file": str(apk_path),
            "size_bytes": apk_path.stat().st_size,
            "has_android_manifest": has_manifest,
            "dex_files": dex_files,
            "native_abis": native_libs,
            "resource_entries": resources_count,
            "total_zip_entries": len(names),
        }


def optional_apktool_decode(apk_path: Path, output_dir: Path) -> str:
    if shutil.which("apktool") is None:
        return "apktool missing (skip)"
    result = run(["apktool", "d", str(apk_path), "-o", str(output_dir), "-f"])
    return "ok" if result.returncode == 0 else f"error: {result.stderr.strip()[:220]}"


def optional_jadx_export(apk_path: Path, output_dir: Path) -> str:
    if shutil.which("jadx") is None:
        return "jadx missing (skip)"
    result = run(["jadx", "-d", str(output_dir), str(apk_path)])
    return "ok" if result.returncode == 0 else f"error: {result.stderr.strip()[:220]}"


def adb_list_packages(limit: int = 50) -> List[str]:
    if shutil.which("adb") is None:
        return []
    result = run(["adb", "shell", "pm", "list", "packages"])
    if result.returncode != 0:
        return []
    packages = [line.replace("package:", "").strip() for line in result.stdout.splitlines() if line.strip()]
    return packages[:limit]


def adb_package_info(package_name: str) -> Dict[str, object]:
    if shutil.which("adb") is None:
        return {"status": "adb missing"}

    path_result = run(["adb", "shell", "pm", "path", package_name])
    dump_result = run(["adb", "shell", "dumpsys", "package", package_name])
    if path_result.returncode != 0 or dump_result.returncode != 0:
        return {"status": "package not found or adb unavailable"}

    apk_paths = [line.replace("package:", "").strip() for line in path_result.stdout.splitlines() if line.strip()]
    dump_text = dump_result.stdout

    version_name = ""
    version_code = ""
    debuggable = "unknown"
    for line in dump_text.splitlines():
        clean = line.strip()
        if clean.startswith("versionName="):
            version_name = clean.replace("versionName=", "", 1)
        elif clean.startswith("versionCode="):
            version_code = clean.replace("versionCode=", "", 1).split(" ", 1)[0]
        elif "pkgFlags=[" in clean and "DEBUGGABLE" in clean:
            debuggable = "true"

    return {
        "status": "ok",
        "package": package_name,
        "apk_paths": apk_paths,
        "version_name": version_name,
        "version_code": version_code,
        "debuggable_flag": debuggable,
    }


def resolve_apk_path(args: argparse.Namespace) -> Optional[Path]:
    if args.apk:
        apk_path = args.apk
        if not apk_path.exists() or not apk_path.is_file():
            raise SystemExit(f"APK not found: {apk_path}")
        return apk_path
    return None


def main() -> None:
    parser = argparse.ArgumentParser(description="Safe APK inspector/workspace generator for Vibing.")
    parser.add_argument("apk", type=Path, nargs="?", help="Path to APK file")
    parser.add_argument("--list-packages", action="store_true", help="List installed packages from adb device")
    parser.add_argument("--package", help="Inspect installed package via adb (metadata only)")
    parser.add_argument("--workspace", type=Path, help="Output workspace folder (default: temp folder)")
    parser.add_argument("--decode", action="store_true", help="Run apktool decode if available")
    parser.add_argument("--decompile", action="store_true", help="Run JADX export if available")
    args = parser.parse_args()

    if args.list_packages:
        packages = adb_list_packages()
        print(json.dumps({"packages": packages, "count": len(packages)}, indent=2))
        return

    if args.package:
        package_report = adb_package_info(args.package)
        print(json.dumps(package_report, indent=2))
        return

    apk_path = resolve_apk_path(args)
    if apk_path is None:
        raise SystemExit("Provide an APK path, or use --list-packages / --package.")

    workspace = args.workspace or Path(tempfile.mkdtemp(prefix="vibing_apk_lab_"))
    workspace.mkdir(parents=True, exist_ok=True)
    apk_copy = workspace / apk_path.name
    shutil.copy2(apk_path, apk_copy)

    report = {
        "tools": detect_tools(),
        "apk_info": apk_basic_info(apk_copy),
        "workspace": str(workspace),
        "legal_notice": "Use only with applications you own or have explicit permission to analyze.",
    }

    if args.decode:
        report["apktool_decode"] = optional_apktool_decode(apk_copy, workspace / "apktool_out")
    if args.decompile:
        report["jadx_decompile"] = optional_jadx_export(apk_copy, workspace / "jadx_out")

    report_file = workspace / "report.json"
    report_file.write_text(json.dumps(report, indent=2), encoding="utf-8")

    print(json.dumps(report, indent=2))
    print(f"\nReport saved at: {report_file}")


if __name__ == "__main__":
    main()
