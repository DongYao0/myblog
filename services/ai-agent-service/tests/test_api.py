from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_returns_service_status():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"service": "ai-agent-service", "status": "UP"}


def test_summary_returns_local_summary():
    response = client.post("/api/ai/summary", json={"content": "one two three four five six"})

    assert response.status_code == 200
    assert response.json()["summary"] == "one two three four five"


def test_chat_stream_returns_sse_events():
    with client.stream("GET", "/api/ai/chat/stream?message=hello") as response:
        body = "".join(response.iter_text())

    assert response.status_code == 200
    assert "event: token" in body
    assert "data: hello" in body


def test_agent_chat_returns_tools_and_answer():
    response = client.post("/api/ai/agent/chat", json={"message": "请计算 3 + 5"})

    assert response.status_code == 200
    assert response.json()["answer"] == "8"
    assert "calculator" in response.json()["tools_used"]
