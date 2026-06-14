import { FormEvent, useEffect, useMemo, useState } from "react";
import { ArrowUpRight, Bot, Clock3, Search, Sparkles } from "lucide-react";
import { Article, askReader, getArticle, listArticles, searchArticles } from "./api";

export function App() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [selected, setSelected] = useState<Article | null>(null);
  const [results, setResults] = useState<Article[]>([]);
  const [assistant, setAssistant] = useState("");
  const [error, setError] = useState("");
  const [loadingAsk, setLoadingAsk] = useState(false);
  const featured = articles[0];
  const sessionId = useMemo(() => `reader-${Date.now()}`, []);

  useEffect(() => {
    listArticles()
      .then((response) => setArticles(response.articles))
      .catch((err) => setError((err as Error).message));
  }, []);

  async function openArticle(article: Article) {
    setError("");
    const loaded = await getArticle(article.id);
    setSelected(loaded);
    setAssistant("");
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    const keyword = String(new FormData(event.currentTarget).get("keyword") || "").trim();
    if (!keyword) return;
    const response = await searchArticles(keyword);
    setResults(response.results || []);
  }

  async function handleAsk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const question = String(new FormData(form).get("question") || "").trim();
    if (!question) return;
    const content = selected
      ? `文章标题：${selected.title}\n文章内容：${selected.content}\n读者问题：${question}`
      : question;
    setLoadingAsk(true);
    setAssistant("DeepSeek 正在结合当前文章思考...");
    try {
      const response = await askReader(content, sessionId);
      setAssistant(`${response.answer}\n\n工具：${response.tools_used.join(", ") || "deepseek_chat"}`);
      form.reset();
    } catch (err) {
      setAssistant(`AI 阅读助手暂不可用：${(err as Error).message}`);
    } finally {
      setLoadingAsk(false);
    }
  }

  return (
    <main className="reader">
      <header className="topbar">
        <div className="mark"><Sparkles /> MyBlog Reader</div>
        <a href="http://127.0.0.1:15173" target="_blank">管理端 <ArrowUpRight size={16} /></a>
      </header>

      <section className="hero">
        <div>
          <p className="eyebrow">Spring Cloud / AI Agent / Engineering Notes</p>
          <h1>把微服务、消息队列和 AI Agent 写成读者能看懂的工程故事。</h1>
          <p>这是博客客户端，面向普通读者：阅读文章、检索内容、打开详情，并让 AI 阅读助手围绕当前文章回答问题。</p>
        </div>
        <form className="searchbox" onSubmit={handleSearch}>
          <Search size={18} />
          <input name="keyword" placeholder="搜索 RocketMQ / Redis / Agent" />
          <button>搜索</button>
        </form>
      </section>

      {error && <p className="error">前台暂未加载到内容：{error}</p>}

      {featured && (
        <section className="featured" onClick={() => openArticle(featured)}>
          <span>Featured</span>
          <h2>{featured.title}</h2>
          <p>{excerpt(featured.content, 160)}</p>
        </section>
      )}

      <section className="section-head">
        <h2>最新文章</h2>
        <p>{articles.length} 篇内容</p>
      </section>
      <section className="article-grid">
        {articles.map((article) => (
          <article className="article-card" key={article.id} onClick={() => openArticle(article)}>
            <small><Clock3 size={14} /> {article.status}</small>
            <h3>{article.title}</h3>
            <p>{excerpt(article.content, 110)}</p>
          </article>
        ))}
      </section>

      {results.length > 0 && (
        <>
          <section className="section-head">
            <h2>搜索结果</h2>
            <p>{results.length} 条匹配</p>
          </section>
          <section className="result-strip">
            {results.map((article) => (
              <button key={article.id} onClick={() => openArticle(article)}>#{article.id} {article.title}</button>
            ))}
          </section>
        </>
      )}

      {selected && (
        <aside className="detail">
          <button className="close" onClick={() => setSelected(null)}>关闭</button>
          <article>
            <p className="eyebrow">Article #{selected.id}</p>
            <h2>{selected.title}</h2>
            <div className="prose">{selected.content}</div>
          </article>
          <form className="assistant" onSubmit={handleAsk}>
            <h3><Bot size={18} /> AI 阅读助手</h3>
            <input name="question" placeholder="这篇文章的核心技术点是什么？" disabled={loadingAsk} />
            <button disabled={loadingAsk}>提问</button>
            <pre>{assistant || "打开文章后，可以让 DeepSeek 结合当前文章回答。"}</pre>
          </form>
        </aside>
      )}
    </main>
  );
}

function excerpt(text: string, length: number) {
  return text.length > length ? `${text.slice(0, length)}...` : text;
}
