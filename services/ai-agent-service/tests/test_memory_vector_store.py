from app.services.memory import AgentMemoryStore
from app.services.vector_store import VectorStore


def test_memory_store_persists_messages(tmp_path):
    store = AgentMemoryStore(tmp_path / "memory.db")

    store.add("commander", "user", "RocketMQ handles async summary tasks")

    assert store.recent("commander")[0].content == "RocketMQ handles async summary tasks"


def test_vector_store_retrieves_semantic_documents(tmp_path):
    store = VectorStore(tmp_path / "vectors.db")
    store.upsert("doc-1", "RocketMQ handles async workload and summary tasks", {"kind": "article"})
    store.upsert("doc-2", "Redis stores hot article cache entries", {"kind": "article"})

    results = store.search("RocketMQ async task", limit=1)

    assert results[0].document_id == "doc-1"
    assert results[0].score > 0
