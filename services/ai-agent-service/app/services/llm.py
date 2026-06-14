from typing import Protocol

import httpx


class LlmClient(Protocol):
    def chat(self, messages: list[dict[str, str]]) -> str:
        ...


class DeepSeekClient:
    def __init__(
        self,
        api_key: str,
        base_url: str = "https://api.deepseek.com",
        model: str = "deepseek-chat",
        http_client: httpx.Client | None = None,
    ) -> None:
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.http_client = http_client or httpx.Client(timeout=30)

    def chat(self, messages: list[dict[str, str]]) -> str:
        response = self.http_client.post(
            f"{self.base_url}/chat/completions",
            headers={"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"},
            json={"model": self.model, "messages": messages, "temperature": 0.2},
        )
        response.raise_for_status()
        data = response.json()
        return data["choices"][0]["message"]["content"]


class LocalLlm:
    def chat(self, messages: list[dict[str, str]]) -> str:
        user = next((message["content"] for message in reversed(messages) if message["role"] == "user"), "")
        context = next((message["content"] for message in messages if message["role"] == "system" and "Context:" in message["content"]), "")
        return context.replace("Context:", "").strip() or f"Local agent response: {user}"
