from app.services.agent import BlogAgent


def test_agent_uses_calculator_tool():
    agent = BlogAgent()

    response = agent.chat("请计算 6 * 7")

    assert response.answer == "42"
    assert "calculator" in response.tools_used


def test_agent_uses_article_rag_context():
    agent = BlogAgent()
    agent.add_article("spring-cloud", "Gateway Nacos Redis RocketMQ Elasticsearch FastAPI")

    response = agent.chat("项目里 RocketMQ 用来做什么？")

    assert "RocketMQ" in response.answer
    assert "article_retriever" in response.tools_used
