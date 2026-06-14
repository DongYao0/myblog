from dataclasses import dataclass
from pathlib import Path
import sqlite3
import time


@dataclass
class MemoryMessage:
    session_id: str
    role: str
    content: str
    created_at: float


class AgentMemoryStore:
    def __init__(self, path: str | Path = "data/agent_memory.db") -> None:
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._init()

    def add(self, session_id: str, role: str, content: str) -> None:
        with sqlite3.connect(self.path) as connection:
            connection.execute(
                "insert into memory(session_id, role, content, created_at) values (?, ?, ?, ?)",
                (session_id, role, content, time.time()),
            )

    def recent(self, session_id: str, limit: int = 8) -> list[MemoryMessage]:
        with sqlite3.connect(self.path) as connection:
            rows = connection.execute(
                "select session_id, role, content, created_at from memory where session_id = ? order by id desc limit ?",
                (session_id, limit),
            ).fetchall()
        return [MemoryMessage(*row) for row in reversed(rows)]

    def _init(self) -> None:
        with sqlite3.connect(self.path) as connection:
            connection.execute(
                """
                create table if not exists memory(
                    id integer primary key autoincrement,
                    session_id text not null,
                    role text not null,
                    content text not null,
                    created_at real not null
                )
                """
            )
