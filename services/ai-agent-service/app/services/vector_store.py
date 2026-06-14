from dataclasses import dataclass
import json
import math
from pathlib import Path
import re
import sqlite3


@dataclass
class VectorResult:
    document_id: str
    content: str
    metadata: dict[str, str]
    score: float


class VectorStore:
    def __init__(self, path: str | Path = "data/vector_store.db") -> None:
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._init()

    def upsert(self, document_id: str, content: str, metadata: dict[str, str] | None = None) -> None:
        vector = _embed(content)
        with sqlite3.connect(self.path) as connection:
            connection.execute(
                """
                insert into vectors(document_id, content, metadata, vector)
                values (?, ?, ?, ?)
                on conflict(document_id) do update set
                  content = excluded.content,
                  metadata = excluded.metadata,
                  vector = excluded.vector
                """,
                (document_id, content, json.dumps(metadata or {}), json.dumps(vector)),
            )

    def search(self, query: str, limit: int = 3) -> list[VectorResult]:
        query_vector = _embed(query)
        with sqlite3.connect(self.path) as connection:
            rows = connection.execute("select document_id, content, metadata, vector from vectors").fetchall()
        scored = []
        for document_id, content, metadata, vector in rows:
            score = _cosine(query_vector, json.loads(vector))
            if score > 0:
                scored.append(VectorResult(document_id, content, json.loads(metadata), score))
        return sorted(scored, key=lambda item: item.score, reverse=True)[:limit]

    def _init(self) -> None:
        with sqlite3.connect(self.path) as connection:
            connection.execute(
                """
                create table if not exists vectors(
                    document_id text primary key,
                    content text not null,
                    metadata text not null,
                    vector text not null
                )
                """
            )


def _embed(text: str) -> dict[str, float]:
    tokens = re.findall(r"[A-Za-z0-9\u4e00-\u9fff]+", text.lower())
    counts: dict[str, float] = {}
    for token in tokens:
        counts[token] = counts.get(token, 0) + 1.0
    return counts


def _cosine(left: dict[str, float], right: dict[str, float]) -> float:
    numerator = sum(left.get(key, 0) * right.get(key, 0) for key in left)
    left_norm = math.sqrt(sum(value * value for value in left.values()))
    right_norm = math.sqrt(sum(value * value for value in right.values()))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return numerator / (left_norm * right_norm)
