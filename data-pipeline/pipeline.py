import argparse
import hashlib
import json
from collections import Counter
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from urllib.request import urlopen

REVISION = "6c21457b9ebf0a6a7f5b91e11281d65d456ea30e"
REMOTE = "data/train-00000-of-00001.parquet"
BASE = "https://huggingface.co/datasets/FronkonGames/steam-games-dataset"
FIELDS = [
    "appID",
    "name",
    "release_date",
    "price",
    "short_description",
    "genres",
    "developers",
    "publishers",
]


def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def write_json(path, data):
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    temporary.replace(path)


def download(root):
    root.mkdir(parents=True, exist_ok=True)
    path = root / "games.parquet"
    manifest = root / "manifest.json"

    if path.exists():
        if not manifest.exists():
            raise ValueError("Existing file has no manifest; refusing to overwrite")
        meta = json.loads(manifest.read_text())
        if meta.get("revision") != REVISION or meta.get("sha256") != digest(path):
            raise ValueError("Cached snapshot failed revision/checksum verification")
        print("Cached snapshot verified", flush=True)
        return

    url = f"{BASE}/resolve/{REVISION}/{REMOTE}"
    partial = root / "games.parquet.part"
    print(f"Downloading revision {REVISION}", flush=True)
    with urlopen(url, timeout=120) as response, partial.open("wb") as output:
        for chunk in iter(lambda: response.read(1024 * 1024), b""):
            output.write(chunk)

    checksum = digest(partial)
    partial.replace(path)
    write_json(manifest, {"source": BASE, "revision": REVISION, "file": REMOTE,
                         "sha256": checksum, "bytes": path.stat().st_size,
                         "downloaded_at": datetime.now(timezone.utc).isoformat(),
                         "license_declared": "MIT", "doi": "10.57967/hf/0511"})
    print(f"Downloaded {path.stat().st_size} bytes; SHA256 {checksum}")


def blank(value):
    return value is None or (isinstance(value, str) and not value.strip()) or value == []


def date_kind(value):
    if blank(value):
        return "missing"
    value = str(value).strip()
    for fmt in ("%b %d, %Y", "%Y-%m-%d", "%Y-%m-%d %H:%M:%S"):
        try:
            datetime.strptime(value, fmt)
            return "exact"
        except ValueError:
            pass

    try:
        datetime.strptime(value, "%b %Y")
        return "month_only"
    except ValueError:
        return "unrecognized"


def profile(root):
    import duckdb
    path = root / "games.parquet"

    meta = json.loads((root / "manifest.json").read_text())
    if meta.get("revision") != REVISION or digest(path) != meta["sha256"]:
        raise ValueError("Snapshot checksum/revision mismatch")

    con = duckdb.connect()
    relation = con.read_parquet(str(path))
    absent = set(FIELDS) - set(relation.columns)
    if absent:
        raise ValueError(f"Missing required columns: {sorted(absent)}")

    report = {"source": meta, "schema": dict(zip(relation.columns, map(str, relation.types))),
              "rows": 0, "empty": Counter(), "dates": Counter(), "prices": Counter(),
              "lists": {}, "invalid_id": 0, "examples": {"dates": []}}

    ids = Counter()
    list_stats = {f: {"non_list": 0, "empty_elements": 0, "repeated_elements": 0,
                      "lengths": Counter(), "values": Counter()} for f in FIELDS[5:]}

    cursor = relation.project(", ".join('"' + f + '"' for f in FIELDS)).execute()

    while rows := cursor.fetchmany(2000):
        for row in rows:
            item = dict(zip(FIELDS, row))
            report["rows"] += 1
            for field, value in item.items():
                if blank(value):
                    report["empty"][field] += 1

            raw_id = str(item["appID"]).strip()
            if (
                not raw_id.isascii()
                or not raw_id.isdecimal()
                or not 0 < int(raw_id) <= 9223372036854775807
            ):
                report["invalid_id"] += 1
            else:
                ids[int(raw_id)] += 1

            kind = date_kind(item["release_date"])
            report["dates"][kind] += 1
            if kind in ("month_only", "unrecognized") and len(report["examples"]["dates"]) < 15:
                report["examples"]["dates"].append({"app_id": raw_id, "value": item["release_date"]})

            value = item["price"]
            try:
                if blank(value):
                    kind = "missing"
                else:
                    number = Decimal(str(value))
                    if not number.is_finite():
                        kind = "non_finite"
                    elif number < 0:
                        kind = "negative"
                    elif number >= Decimal("10000000000"):
                        kind = "out_of_range"
                    elif number != number.quantize(Decimal("0.01")):
                        kind = "more_than_two_decimals"
                    else:
                        kind = "zero" if number == 0 else "positive"
            except (InvalidOperation, ValueError):
                kind = "invalid"
            report["prices"][kind] += 1

            for field, stats in list_stats.items():
                values = item[field]
                if values is None:
                    continue
                if not isinstance(values, list):
                    stats["non_list"] += 1
                    continue
                stats["lengths"][len(values)] += 1
                cleaned = [v.strip() for v in values if isinstance(v, str) and v.strip()]
                stats["empty_elements"] += len(values) - len(cleaned)
                stats["repeated_elements"] += len(cleaned) - len(set(cleaned))
                stats["values"].update(set(cleaned))

    report["duplicate_id_excess"] = sum(n - 1 for n in ids.values())
    report["duplicate_id_examples"] = [k for k, n in ids.items() if n > 1][:15]

    for field, stats in list_stats.items():
        values = stats.pop("values")
        stats["distinct_trimmed_values"] = len(values)
        stats["most_frequent"] = values.most_common(10)
        report["lists"][field] = stats

    report["note"] = "Read-only profile. No rows removed or imported. Blank means NULL, whitespace-only string, or empty list; literal null/None strings are not silently treated as missing."
    write_json(root / "profile.json", report)
    print(json.dumps({k: report[k] for k in ("rows", "empty", "dates", "prices", "invalid_id", "duplicate_id_excess")}, indent=2))
    print("Full report: /data/profile.json")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["download", "profile", "import"])
    parser.add_argument("--data-dir", type=Path, default=Path("/data"))

    args = parser.parse_args()

    try:
        if args.command == "import":
            from importer import import_data
            import_data(args.data_dir)
        else:
            {"download": download, "profile": profile}[args.command](args.data_dir)
    except Exception as error:
        parser.exit(1, f"Error: {error}\n")
