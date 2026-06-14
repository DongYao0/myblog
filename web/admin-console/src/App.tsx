import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Bot,
  Database,
  ExternalLink,
  FilePlus2,
  Gauge,
  Layers3,
  LockKeyhole,
  Radio,
  Search,
  Send,
  Sparkles,
  Trash2
} from "lucide-react";
import {
  AiTask,
  Article,
  Auth,
  askAgent,
  createArticle,
  deleteArticle,
  getTask,
  indexAgentDocument,
  listArticles,
  login,
  register,
  searchArticles,
  streamAi,
  submitSummary,
  updateArticle
} from "./api";

type Notice = { kind: "ok" | "error"; text: string };
type View = "dashboard" | "articles" | "search" | "ai" | "rag";

export function App() {
  const [auth, setAuth] = useState<Auth | null>(() => {
    const raw = localStorage.getItem("myblog-auth");
    return raw ? JSON.parse(raw) : null;
  });
  const [view, setView] = useState<View>("dashboard");
  const [notice, setNotice] = useState<Notice>({ kind: "ok", text: "控制台待命" });
  const [articles, setArticles] = useState<Article[]>([]);
  const [selected, setSelected] = useState<Article | null>(null);
  const [task, setTask] = useState<AiTask | null>(null);
  const [searchRows, setSearchRows] = useState<Article[]>([]);
  const [agentAnswer, setAgentAnswer] = useState("");
  const [agentMemory, setAgentMemory] = useState<string[]>([]);
  const [streamText, setStreamText] = useState("");
  const [sessionId, setSessionId] = useState("commander");
  const authed = useMemo(() => Boolean(auth?.token), [auth]);

  useEffect(() => {
    if (auth) void refreshArticles(auth.token);
  }, [auth]);

  async function refreshArticles(token = auth?.token) {
    if (!token) return;
    try {
      const response = await listArticles(token);
      setArticles(response.articles);
    } catch (error) {
      localStorage.removeItem("myblog-auth");
      setAuth(null);
      setArticles([]);
      setNotice({ kind: "error", text: `登录已过期，请重新登录：${(error as Error).message}` });
    }
  }

  async function handleAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const username = String(form.get("username"));
    const password = String(form.get("password"));
    const mode = String(form.get("mode"));
    try {
      const result = mode === "register" ? await register(username, password) : await login(username, password);
      localStorage.setItem("myblog-auth", JSON.stringify(result));
      setAuth(result);
      setNotice({ kind: "ok", text: `已接入：${result.username}` });
    } catch (error) {
      setNotice({ kind: "error", text: `认证失败：${(error as Error).message}` });
    }
  }

  async function handleSaveArticle(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auth) return;
    const form = new FormData(event.currentTarget);
    const title = String(form.get("title"));
    const content = String(form.get("content"));
    try {
      const saved = selected
        ? await updateArticle(auth.token, selected.id, title, content)
        : await createArticle(auth.token, title, content, auth.userId);
      setSelected(saved);
      await refreshArticles(auth.token);
      setNotice({ kind: "ok", text: `文章 #${saved.id} 已保存` });
    } catch (error) {
      setNotice({ kind: "error", text: `保存失败：${(error as Error).message}` });
    }
  }

  async function handleDelete(article: Article) {
    if (!auth) return;
    await deleteArticle(auth.token, article.id);
    setSelected(null);
    await refreshArticles(auth.token);
    setNotice({ kind: "ok", text: `文章 #${article.id} 已删除` });
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auth) return;
    const keyword = String(new FormData(event.currentTarget).get("keyword"));
    const response = await searchArticles(auth.token, keyword);
    setSearchRows(response.results || []);
  }

  async function handleSummary(article: Article | null = selected) {
    if (!article || !auth) return;
    const submitted = await submitSummary(auth.token, article.id);
    setTask(submitted);
    for (let i = 0; i < 20; i++) {
      await new Promise((resolve) => setTimeout(resolve, 800));
      const current = await getTask(auth.token, submitted.id);
      setTask(current);
      if (current.status === "SUCCESS" || current.status === "FAILED") break;
    }
  }

  async function handleAgent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auth) return;
    const message = String(new FormData(event.currentTarget).get("message"));
    const response = await askAgent(auth.token, message, sessionId);
    setAgentAnswer(`${response.answer}\n\n工具：${response.tools_used.join(", ") || "deepseek_chat"}`);
    setAgentMemory(response.memory || []);
  }

  async function handleIndexDocument(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auth) return;
    const form = new FormData(event.currentTarget);
    await indexAgentDocument(auth.token, String(form.get("documentId")), String(form.get("content")));
    setNotice({ kind: "ok", text: "RAG 文档已写入向量库" });
  }

  async function handleIndexSelected() {
    if (!auth || !selected) return;
    await indexAgentDocument(auth.token, `article-${selected.id}`, `${selected.title}\n${selected.content}`);
    setNotice({ kind: "ok", text: `文章 #${selected.id} 已写入向量库` });
  }

  async function handleStream(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auth) return;
    setStreamText("");
    const message = String(new FormData(event.currentTarget).get("stream"));
    await streamAi(auth.token, message, (chunk) => setStreamText((value) => value + chunk));
  }

  return (
    <main className="shell">
      <aside className="rail">
        <div className="brand"><Sparkles /> MyBlog AI Cloud</div>
        <a className="client-link" href="http://127.0.0.1:15174" target="_blank">
          打开博客客户端 <ExternalLink size={15} />
        </a>
        <form className="panel compact" onSubmit={handleAuth}>
          <h2><LockKeyhole /> 身份</h2>
          <input name="username" placeholder="commander" defaultValue="commander" />
          <input name="password" placeholder="secret123" defaultValue="secret123" type="password" />
          <div className="split">
            <button name="mode" value="login">登录</button>
            <button name="mode" value="register">注册</button>
          </div>
        </form>
        <nav className="nav">
          {[
            ["dashboard", Gauge, "总览"],
            ["articles", FilePlus2, "文章管理"],
            ["search", Search, "全文搜索"],
            ["ai", Bot, "AI 对话"],
            ["rag", Database, "RAG 知识库"]
          ].map(([key, Icon, label]) => (
            <button key={key as string} className={view === key ? "active" : ""} onClick={() => setView(key as View)}>
              <Icon size={17} /> {label as string}
            </button>
          ))}
        </nav>
        <div className={`notice ${notice.kind}`}>{notice.text}</div>
        <div className="metric"><span>JWT Gateway</span><b>{authed ? "ON" : "LOCKED"}</b></div>
        <div className="metric"><span>DeepSeek</span><b>READY</b></div>
        <div className="metric"><span>RocketMQ</span><b>{task?.status || "IDLE"}</b></div>
      </aside>

      <section className="workspace">
        {view === "dashboard" && (
          <div className="dashboard">
            <section className="hero">
              <h1>管理端控制台</h1>
              <p>面向运营和开发者：内容生产、搜索索引、异步摘要、DeepSeek Agent、长期记忆和 RAG 知识库都在这里维护。</p>
            </section>
            <div className="grid stats">
              <Metric title="文章数量" value={articles.length} icon={<Layers3 />} />
              <Metric title="当前用户" value={auth?.username || "未登录"} icon={<LockKeyhole />} />
              <Metric title="最近任务" value={task?.status || "无"} icon={<Radio />} />
              <Metric title="记忆条数" value={agentMemory.length} icon={<Database />} />
            </div>
            <section className="panel timeline">
              <h2><Radio /> 当前系统分层</h2>
              <p><b>博客客户端：</b>读者访问、文章阅读、搜索、AI 阅读助手。</p>
              <p><b>管理端：</b>登录、文章 CRUD、RAG 入库、AI 调试、任务观察。</p>
              <p><b>服务端：</b>Gateway 鉴权路由，Blog 业务，FastAPI Agent，Docker 中间件。</p>
            </section>
          </div>
        )}

        {view === "articles" && (
          <div className="content-grid">
            <section className="panel list">
              <h2><Layers3 /> 文章列表</h2>
              <button disabled={!authed} onClick={() => setSelected(null)}>新建文章</button>
              {articles.map((item) => (
                <article className="row" key={item.id} onClick={() => setSelected(item)}>
                  <b>#{item.id} {item.title}</b>
                  <small>{item.status}</small>
                  <button onClick={(event) => { event.stopPropagation(); void handleDelete(item); }}><Trash2 size={15} /></button>
                </article>
              ))}
            </section>
            <form className="panel editor" onSubmit={handleSaveArticle}>
              <h2><FilePlus2 /> {selected ? `编辑 #${selected.id}` : "新建文章"}</h2>
              <input key={selected?.id || "new-title"} name="title" placeholder="Spring Cloud AI Blog" defaultValue={selected?.title || ""} disabled={!authed} />
              <textarea key={selected?.id || "new-content"} name="content" placeholder="Gateway Nacos Redis RocketMQ Elasticsearch FastAPI" defaultValue={selected?.content || ""} disabled={!authed} />
              <div className="split">
                <button disabled={!authed}>保存</button>
                <button type="button" disabled={!selected} onClick={() => handleSummary()}>摘要任务</button>
              </div>
              <button type="button" disabled={!selected} onClick={handleIndexSelected}>写入 RAG</button>
            </form>
          </div>
        )}

        {view === "search" && (
          <form className="panel" onSubmit={handleSearch}>
            <h2><Search /> Elasticsearch 全文搜索</h2>
            <input name="keyword" placeholder="RocketMQ" disabled={!authed} />
            <button disabled={!authed}>搜索</button>
            <div className="result-list">
              {searchRows.map((row) => (
                <article key={row.id}>
                  <b>#{row.id} {row.title}</b>
                  <p>{row.content}</p>
                </article>
              ))}
            </div>
          </form>
        )}

        {view === "ai" && (
          <div className="grid">
            <form className="panel" onSubmit={handleAgent}>
              <h2><Bot /> DeepSeek Agent</h2>
              <input value={sessionId} onChange={(event) => setSessionId(event.target.value)} />
              <input name="message" placeholder="结合项目知识库说明 RocketMQ 的作用" disabled={!authed} />
              <button disabled={!authed}><Send size={16} /> 询问</button>
              <pre>{agentAnswer || "等待 Agent 响应"}</pre>
            </form>
            <form className="panel" onSubmit={handleStream}>
              <h2><Bot /> SSE 流式窗口</h2>
              <input name="stream" placeholder="hello ai console" disabled={!authed} />
              <button disabled={!authed}>流式发送</button>
              <pre>{streamText || "等待流式输出"}</pre>
            </form>
            <section className="panel span">
              <h2><Database /> 长期记忆</h2>
              <pre>{agentMemory.join("\n") || "当前会话暂无记忆"}</pre>
            </section>
          </div>
        )}

        {view === "rag" && (
          <form className="panel editor" onSubmit={handleIndexDocument}>
            <h2><Database /> 向量知识库</h2>
            <input name="documentId" placeholder="project-architecture" disabled={!authed} />
            <textarea name="content" placeholder="把项目架构、面试话术、业务知识写入这里，Agent 会检索后再回答。" disabled={!authed} />
            <button disabled={!authed}>写入向量库</button>
          </form>
        )}
      </section>
    </main>
  );
}

function Metric({ title, value, icon }: { title: string; value: string | number; icon: React.ReactNode }) {
  return <section className="panel metric-card">{icon}<span>{title}</span><b>{value}</b></section>;
}
