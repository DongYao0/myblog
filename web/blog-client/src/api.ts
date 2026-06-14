export type Article = {
  id: number;
  title: string;
  content: string;
  authorId: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
};

export type AgentResponse = {
  answer: string;
  tools_used: string[];
  context: string[];
  memory: string[];
};

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(path, options);
  if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
  return response.json() as Promise<T>;
}

export const listArticles = () => request<{ articles: Article[] }>("/api/blog/articles");

export const getArticle = (id: number) => request<Article>(`/api/blog/articles/${id}`);

export const searchArticles = (keyword: string) =>
  request<{ results: Article[] }>(`/api/blog/search?keyword=${encodeURIComponent(keyword)}`);

export const askReader = (message: string, sessionId: string) =>
  request<AgentResponse>("/api/ai/agent/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message, session_id: sessionId })
  });
