export type Auth = { userId: number; username: string; token: string };
export type Article = { id: number; title: string; content: string; authorId: number; status: string };
export type AiTask = { id: number; articleId: number; status: string; result?: string };
export type AgentResponse = { answer: string; tools_used: string[]; context: string[]; memory: string[] };

const jsonHeaders = (token?: string) => ({
  "Content-Type": "application/json",
  ...(token ? { Authorization: `Bearer ${token}` } : {})
});

export async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const response = await fetch(path, { ...options, headers: { ...jsonHeaders(token), ...(options.headers || {}) } });
  if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
  return response.json() as Promise<T>;
}

export const login = (username: string, password: string) =>
  request<Auth>("/api/blog/auth/login", { method: "POST", body: JSON.stringify({ username, password }) });

export const register = (username: string, password: string) =>
  request<Auth>("/api/blog/auth/register", { method: "POST", body: JSON.stringify({ username, password }) });

export const createArticle = (token: string, title: string, content: string, authorId: number) =>
  request<Article>("/api/blog/articles", { method: "POST", body: JSON.stringify({ title, content, authorId }) }, token);

export const listArticles = (token: string) =>
  request<{ articles: Article[] }>("/api/blog/articles", {}, token);

export const updateArticle = (token: string, id: number, title: string, content: string) =>
  request<Article>(`/api/blog/articles/${id}`, { method: "PUT", body: JSON.stringify({ title, content }) }, token);

export const deleteArticle = (token: string, id: number) =>
  request<{ deleted: boolean }>(`/api/blog/articles/${id}`, { method: "DELETE" }, token);

export const searchArticles = (token: string, keyword: string) =>
  request<{ results: Article[] }>(`/api/blog/search?keyword=${encodeURIComponent(keyword)}`, {}, token);

export const submitSummary = (token: string, articleId: number) =>
  request<AiTask>("/api/blog/ai/tasks/summary", { method: "POST", body: JSON.stringify({ articleId }) }, token);

export const getTask = (token: string, id: number) => request<AiTask>(`/api/blog/ai/tasks/${id}`, {}, token);

export const askAgent = (token: string, message: string, sessionId: string) =>
  request<AgentResponse>("/api/ai/agent/chat", { method: "POST", body: JSON.stringify({ message, session_id: sessionId }) }, token);

export const indexAgentDocument = (token: string, documentId: string, content: string) =>
  request<{ status: string; document_id: string }>(
    "/api/ai/agent/documents",
    { method: "POST", body: JSON.stringify({ document_id: documentId, content }) },
    token
  );

export async function streamAi(token: string, message: string, onText: (text: string) => void) {
  const response = await fetch(`/api/ai/chat/stream?message=${encodeURIComponent(message)}`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  if (!response.ok || !response.body) throw new Error(`${response.status} ${response.statusText}`);
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    onText(decoder.decode(value));
  }
}
