from app.services.agent import BlogAgent


class FakeLlm:
    def chat(self, messages):
        return "DeepSeek grounded answer"


def test_agent_uses_calculator_tool():
    agent = BlogAgent()

    response = agent.chat("please calculate 6 * 7")

    assert response.answer == "42"
    assert "calculator" in response.tools_used


def test_agent_uses_article_rag_context():
    agent = BlogAgent(llm=FakeLlm())
    agent.add_article("spring-cloud", "Gateway Nacos Redis RocketMQ Elasticsearch FastAPI")

    response = agent.chat("What is RocketMQ used for in this project?")

    assert response.answer == "DeepSeek grounded answer"
    assert "article_retriever" in response.tools_used
    assert response.context
