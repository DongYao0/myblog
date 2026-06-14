import httpx

from app.services.llm import DeepSeekClient


def test_deepseek_client_uses_openai_compatible_chat_completion():
    captured = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["auth"] = request.headers["authorization"]
        captured["body"] = request.read().decode()
        return httpx.Response(200, json={"choices": [{"message": {"content": "real model answer"}}]})

    client = DeepSeekClient(
        api_key="sk-test",
        base_url="https://api.deepseek.com",
        http_client=httpx.Client(transport=httpx.MockTransport(handler)),
    )

    answer = client.chat([{"role": "user", "content": "hello"}])

    assert answer == "real model answer"
    assert captured["url"] == "https://api.deepseek.com/chat/completions"
    assert captured["auth"] == "Bearer sk-test"
    assert "deepseek-chat" in captured["body"]
