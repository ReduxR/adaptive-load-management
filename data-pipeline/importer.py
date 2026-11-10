"""Transactional, empty-catalog-only import. Schema is owned by Liquibase."""

import json
from uuid import uuid4
from collections import Counter
from datetime import datetime
from decimal import Decimal

import duckdb
import psycopg
from psycopg import sql


def import_data(root):
    from pipeline import REVISION, FIELDS, digest, blank, write_json

    meta = json.loads((root / "manifest.json").read_text())
    path = root / "games.parquet"
    if meta.get("revision") != REVISION or digest(path) != meta["sha256"]:
        raise ValueError("Snapshot revision/checksum mismatch")

    result_path = root / ("import-" + uuid4().hex + ".json")

    report = {"source": meta, "rejections": [],
              "normalization": Counter(), "rows_read": 0}

    games, links, seen = [], {}, set()
    names = {entity: set() for entity in ("genre", "developer", "publisher")}

    source = duckdb.connect()
    relation = source.read_parquet(str(path))

    cursor = relation.project(", ".join('"' + f + '"' for f in FIELDS)).execute()

    while batch := cursor.fetchmany(2000):
        for raw in batch:
            row = dict(zip(FIELDS, raw))
            report["rows_read"] += 1

            raw_id = str(row["appID"]).strip()
            if (
                not raw_id.isascii()
                or not raw_id.isdecimal()
                or not 0 < int(raw_id) <= 9223372036854775807
            ):
                raise ValueError("Invalid app ID; import aborted")
            app_id = int(raw_id)
            if app_id in seen:
                raise ValueError(f"Duplicate app ID {app_id}; import aborted")
            seen.add(app_id)

            if blank(row["name"]):
                report["rejections"].append({"app_id": app_id, "reason": "missing_name"})
                continue

            date = None
            if not blank(row["release_date"]):
                for fmt in ("%b %d, %Y", "%Y-%m-%d", "%Y-%m-%d %H:%M:%S"):
                    try:
                        date = datetime.strptime(str(row["release_date"]).strip(), fmt).date()
                        break
                    except ValueError:
                        pass
                if date is None:
                    raise ValueError(f"Unresolved release date for {app_id}; no date invented")

            price = None if blank(row["price"]) else Decimal(str(row["price"]))
            if price is not None and (
                not price.is_finite()
                or price < 0
                or price >= Decimal("10000000000")
                or price != price.quantize(Decimal("0.01"))
            ):
                raise ValueError(f"Invalid price for {app_id}")

            description = None if blank(row["short_description"]) else row["short_description"].strip()
            games.append((app_id, row["name"].strip(), date, price, description))

            links[app_id] = {}

            for entity in names:
                values = row[entity + "s"]
                if values is None:
                    values = []
                if not isinstance(values, list) or any(v is not None and not isinstance(v, str) for v in values):
                    raise ValueError(f"Invalid {entity} list for {app_id}")

                clean = [v.strip() for v in values if v is not None and v.strip()]
                report["normalization"][entity + "_empty_elements"] += len(values) - len(clean)
                report["normalization"][entity + "_repeated_elements"] += len(clean) - len(set(clean))
                links[app_id][entity] = sorted(set(clean))
                names[entity].update(clean)

    dictionaries = {
        entity: {name: i for i, name in enumerate(sorted(values), 1)}
        for entity, values in names.items()
    }

    tables = {"game": games}
    columns = {"game": ["app_id", "name", "release_date", "price", "short_description"]}

    for entity, mapping in dictionaries.items():
        tables[entity] = [(i, name) for name, i in mapping.items()]
        columns[entity] = [entity + "_id", "name"]

    for entity, mapping in dictionaries.items():
        table = "game_" + entity
        tables[table] = [
            (app_id, mapping[name])
            for app_id, group in sorted(links.items())
            for name in group[entity]
        ]
        columns[table] = ["app_id", entity + "_id"]

    report["counts"] = {table: len(rows) for table, rows in tables.items()}

    try:
        with psycopg.connect(connect_timeout=10) as con:
            con.execute("SET LOCAL lock_timeout = '10s'")
            con.execute(sql.SQL("LOCK TABLE {} IN ACCESS EXCLUSIVE MODE").format(sql.SQL(", ").join(map(sql.Identifier, sorted(tables)))))

            for table in tables:
                if con.execute(sql.SQL("SELECT EXISTS(SELECT 1 FROM {})").format(sql.Identifier(table))).fetchone()[0]:
                    raise ValueError(f"Catalog is not empty ({table}); nothing changed")

            for table, rows in tables.items():
                statement = sql.SQL("COPY {} ({}) FROM STDIN").format(sql.Identifier(table), sql.SQL(", ").join(map(sql.Identifier, columns[table])))
                with con.cursor().copy(statement) as copy:
                    for row in rows:
                        copy.write_row(row)

                count = con.execute(sql.SQL("SELECT count(*) FROM {}").format(sql.Identifier(table))).fetchone()[0]
                if count != len(rows):
                    raise ValueError(f"Count mismatch in {table}")

            for entity, mapping in dictionaries.items():
                sequence = con.execute("SELECT pg_get_serial_sequence(%s, %s)", (entity, entity + "_id")).fetchone()[0]
                if sequence is None:
                    raise ValueError(f"Missing identity sequence for {entity}")
                con.execute(sql.SQL("ALTER SEQUENCE {} RESTART WITH {}").format(sql.Identifier(*sequence.split(".")), sql.Literal(len(mapping) + 1)))
        report["status"] = "committed"
    except Exception:
        report["status"] = "failed"
        write_json(result_path, report)
        raise

    write_json(result_path, report)
    print(json.dumps({"status": report["status"], "counts": report["counts"], "rejected": len(report["rejections"])}, indent=2))
