import { FormEvent, useMemo, useState } from "react";
import { Bot, FilePlus2, LockKeyhole, Radio, Search, Send, Sparkles } from "lucide-react";
import {
  AiTask,
  Article,
  Auth,
  askAgent,
  createArticle,
  getTask,
  login,
  register,
  searchArticles,
  streamAi,
  submitSummary
} from "./api";

type Notice = { kind: "ok" | "error"; text: string };

export function App() {
  const [auth, setAuth] = useState<Auth | null>(() => {
    const raw = localStorage.getItem("myblog-auth");
    return raw ? JSON.parse(raw) : null;
  });
  const [notice, setNotice] = useState<Notice>({ kind: "ok", text: "控制台待命" });
  const [article, setArticle] = useState<Article | null>(null);
  const [task, setTask] = useState<AiTask | null>(null);
  const [searchRows, setSearchRows] = useState<Article[]>([]);
  const [agentAnswer, setAgentAnswer] = useState("");
  const [streamText, setStreamText] = useState("");
  const authed = useMemo(() => Boolean(auth?.token), [auth]);

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
      setNotice({ kind: "ok", text: `已接入 ${result.username}` });
    } catch (error) {
      setNotice({ kind: "error", text: `认证失败：${(error as Error).message}` });
    }
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    if (!auth) return;
    try {
      const created = await createArticle(auth.token, String(form.get("title")), String(form.get("content")), auth.userId);
      setArticle(created);
      setNotice({ kind: "ok", text: `文章 #${created.id} 已发布` });
    } catch (error) {
      setNotice({ kind: "error", text: `发布失败：${(error as Error).message}` });
    }
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auth) return;
    const keyword = String(new FormData(event.currentTarget).get("keyword"));
    const response = await searchArticles(auth.token, keyword);
    setSearchRows(response.results || []);
  }

  async function handleSummary() {
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
    const response = await askAgent(auth.token, message);
    setAgentAnswer(`${response.answer}\n工具：${response.tools_used.join(", ") || "none"}`);
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
      <section className="rail">
        <div className="brand"><Sparkles /> MyBlog AI Cloud</div>
        <form className="panel compact" onSubmit={handleAuth}>
          <h2><LockKeyhole /> 身份</h2>
          <input name="username" placeholder="commander" defaultValue="commander" />
          <input name="password" placeholder="secret123" defaultValue="secret123" type="password" />
          <div className="split">
            <button name="mode" value="login">登录</button>
            <button name="mode" value="register">注册</button>
          </div>
        </form>
        <div className={`notice ${notice.kind}`}>{notice.text}</div>
        <div className="metric"><span>JWT Gateway</span><b>{authed ? "ON" : "LOCKED"}</b></div>
        <div className="metric"><span>RocketMQ Task</span><b>{task?.status || "IDLE"}</b></div>
      </section>
      <section className="workspace">
        <form className="panel editor" onSubmit={handleCreate}>
          <h2><FilePlus2 /> 内容生产</h2>
          <input name="title" placeholder="Spring Cloud AI Blog" disabled={!authed} />
          <textarea name="content" placeholder="Gateway Nacos Redis RocketMQ Elasticsearch FastAPI" disabled={!authed} />
          <button disabled={!authed}>发布文章</button>
        </form>
        <div className="grid">
          <form className="panel" onSubmit={handleSearch}>
            <h2><Search /> 全文搜索</h2>
            <input name="keyword" placeholder="RocketMQ" disabled={!authed} />
            <button disabled={!authed}>搜索</button>
            <pre>{searchRows.map((row) => `#${row.id} ${row.title}`).join("\n") || "等待检索"}</pre>
          </form>
          <section className="panel">
            <h2><Radio /> 异步摘要</h2>
            <button onClick={handleSummary} disabled={!article}>提交摘要任务</button>
            <pre>{task ? JSON.stringify(task, null, 2) : "先发布文章"}</pre>
          </section>
          <form className="panel" onSubmit={handleAgent}>
            <h2><Bot /> Agent 工具调用</h2>
            <input name="message" placeholder="请计算 6 * 7" disabled={!authed} />
            <button disabled={!authed}><Send size={16} /> 询问</button>
            <pre>{agentAnswer || "等待 Agent 响应"}</pre>
          </form>
          <form className="panel" onSubmit={handleStream}>
            <h2><Bot /> SSE 流式对话</h2>
            <input name="stream" placeholder="hello ai console" disabled={!authed} />
            <button disabled={!authed}>流式发送</button>
            <pre>{streamText || "等待流式输出"}</pre>
          </form>
        </div>
      </section>
    </main>
  );
}
