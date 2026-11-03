import argparse
import json
import re
import shutil
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NS = "thaumicenergistics"
SOURCE = ROOT / "reference" / "te-src" / "src" / "main" / "resources" / "assets" / NS
RESOURCES = ROOT / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets" / NS
LANG_EXTRA = ROOT / "tools" / "lang"
ASSETS_EXTRA = ROOT / "tools" / "assets_extra"

TEXTURE_DIRS = {
    "blocks": "block",
    "items": "item",
    "models": "models",
    "gui": "gui",
    "research": "research",
}

FORMAT_PATTERN = re.compile(r"%(?:(\d+)\$)?([A-Za-z%]|$)")
INVALID_PATH = re.compile(r"[^a-z0-9_.\-/]")
PASS_THROUGH = re.compile(r"^(tc\.research_category\.|tc\.research_name\.|tc\.research_text\.|thaumicenergistics\.)")
FIXED_KEYS = {"itemGroup.ThaumicEnergistics": "itemGroup.thaumicenergistics"}

report = {
    "renamed": [],
    "warnings": [],
}


def warn(message):
    report["warnings"].append(message)
    print(f"warning: {message}")


def clean_name(name):
    cleaned = INVALID_PATH.sub("_", name.lower().replace(" - ", "_"))
    return re.sub(r"_+", "_", cleaned)


def remove(path):
    if path.is_dir():
        shutil.rmtree(path)
    elif path.exists():
        path.unlink()


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def clean_targets():
    for name in ("textures", "lang"):
        remove(ASSETS / name)


def convert_mcmeta(source, destination):
    data = json.loads(source.read_text(encoding="utf-8"))
    write_json(destination, data)


def convert_textures():
    counts = Counter()
    for old_dir, new_dir in TEXTURE_DIRS.items():
        source_root = SOURCE / "textures" / old_dir
        if not source_root.exists():
            continue
        for path in sorted(source_root.rglob("*")):
            if path.is_dir() or path.name == ".DS_Store":
                continue
            relative = path.relative_to(source_root).as_posix()
            name = clean_name(relative)
            if name != relative:
                report["renamed"].append(f"textures/{new_dir}/{relative} -> {name}")
            destination = ASSETS / "textures" / new_dir / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            if path.suffix == ".mcmeta":
                convert_mcmeta(path, destination)
                counts[f"{new_dir}/mcmeta"] += 1
            elif path.suffix == ".png":
                shutil.copyfile(path, destination)
                counts[f"{new_dir}/png"] += 1
            else:
                warn(f"unexpected texture file skipped: {path}")
    return counts


def load_id_map(path):
    if not path.exists():
        warn(f"id map not found: {path}, block and item keys are not renamed")
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def build_key_map(id_map):
    key_map = defaultdict(list)
    if id_map is None:
        return key_map
    block_ids = set()
    for entry in id_map["blocks"]:
        if entry.get("skip") or not entry.get("lang"):
            continue
        target = f"block.{NS}.{entry['id']}"
        if target not in key_map[entry["lang"]]:
            key_map[entry["lang"]].append(target)
        block_ids.add(entry["id"])
    for entry in id_map["items"]:
        if entry.get("skip") or not entry.get("lang") or entry["id"] in block_ids:
            continue
        target = f"item.{NS}.{entry['id']}"
        if target not in key_map[entry["lang"]]:
            key_map[entry["lang"]].append(target)
    return key_map


def skipped_keys(id_map):
    if id_map is None:
        return set()
    return {
        entry["lang"]
        for entry in id_map["blocks"] + id_map["items"]
        if entry.get("skip") and entry.get("lang")
    }


def unescape(value):
    return re.sub(r"\\(n|\\)", lambda match: "\n" if match.group(1) == "n" else "\\", value)


def escape_format(value, stats):
    def replace(match):
        index, kind = match.group(1), match.group(2)
        if kind in ("s", "%"):
            return match.group(0)
        stats["escaped"] += 1
        if kind == "d":
            return f"%{index}$s" if index else "%s"
        return "%%" + (f"{index}$" if index else "") + kind

    return FORMAT_PATTERN.sub(replace, value)


def read_lang(path):
    text = path.read_bytes().decode("utf-8-sig")
    entries = {}
    for number, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if "=" not in line:
            warn(f"{path.name}:{number} line without '=': {stripped[:40]}")
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = unescape(value)
        if key in entries and entries[key] != value:
            warn(f"{path.name}:{number} duplicate key {key}, last wins")
        entries[key] = value
    return entries


def convert_lang(id_map):
    key_map = build_key_map(id_map)
    dropped = skipped_keys(id_map)
    used_keys = set()
    results = {}
    mapped = Counter()
    unmapped = defaultdict(list)
    escape_stats = Counter()
    for path in sorted((SOURCE / "lang").glob("*.lang")):
        language = path.stem.lower()
        result = {}
        for key, value in read_lang(path).items():
            value = escape_format(value, escape_stats)
            if key in key_map:
                used_keys.add(key)
                for target in key_map[key]:
                    result[target] = value
                mapped[language, "id_map"] += 1
                continue
            if key in dropped:
                mapped[language, "skipped"] += 1
                continue
            if key in FIXED_KEYS:
                result[FIXED_KEYS[key]] = value
                mapped[language, "rewritten"] += 1
                continue
            if PASS_THROUGH.match(key):
                result[key] = value
                mapped[language, "kept"] += 1
                continue
            unmapped[language].append(key)
        extra_paths = [LANG_EXTRA / f"{language}.json", *sorted(LANG_EXTRA.glob(f"*/{language}.json"))]
        for extra_path in extra_paths:
            if extra_path.exists():
                result.update(json.loads(extra_path.read_text(encoding="utf-8")))
        results[language] = result
        write_json(ASSETS / "lang" / f"{language}.json", dict(sorted(result.items())))
    uncovered = []
    if id_map is not None:
        uncovered = sorted(
            entry["lang"]
            for entry in id_map["blocks"] + id_map["items"]
            if entry.get("lang") and not entry.get("skip") and entry["lang"] not in used_keys
        )
    return results, mapped, unmapped, uncovered, escape_stats


def validate(results):
    for path in ASSETS.rglob("*.json"):
        json.loads(path.read_text(encoding="utf-8"))
    for language, entries in results.items():
        for key, value in entries.items():
            if any(match.group(2) not in ("s", "%") for match in FORMAT_PATTERN.finditer(value)):
                warn(f"{language}: bad format in {key}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--id-map", default=str(ROOT / "tools" / "id_map.json"))
    arguments = parser.parse_args()

    if not SOURCE.exists():
        raise SystemExit("reference/te-src is missing, run tools/fetch_te.py first")
    clean_targets()
    texture_counts = convert_textures()
    id_map = load_id_map(Path(arguments.id_map))
    results, mapped, unmapped, uncovered, escape_stats = convert_lang(id_map)
    if ASSETS_EXTRA.exists():
        shutil.copytree(ASSETS_EXTRA, ASSETS, dirs_exist_ok=True)
    validate(results)

    print("\n== textures ==")
    for category, count in sorted(texture_counts.items()):
        print(f"{category}: {count}")
    print("\n== lang ==")
    for language, entries in sorted(results.items()):
        print(f"{language}: {len(entries)} keys (id_map {mapped[language, 'id_map']}, rewritten {mapped[language, 'rewritten']}, kept {mapped[language, 'kept']}, skipped {mapped[language, 'skipped']}, dropped {len(unmapped[language])})")
    print(f"format specifiers escaped: {escape_stats['escaped']}")
    print("\n== dropped en_us keys ==")
    for key in unmapped["en_us"]:
        print(key)
    print("\n== id_map lang keys without source entry ==")
    for key in uncovered:
        print(key)
    print(f"total: {len(uncovered)}")
    print("\n== renamed ==")
    for line in report["renamed"][:15]:
        print(line)
    print(f"total: {len(report['renamed'])}")
    print(f"\nwarnings: {len(report['warnings'])}")


if __name__ == "__main__":
    main()
