import { useState, useRef, useEffect, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  MessageSquare, Send, Download, Copy, Trash2, Plus, Bot, User,
  Check, AlertCircle, Maximize2, ChevronLeft,
} from "lucide-react";
import { clsx } from "clsx";
import { aiApi } from "../../../shared/api/aiApi";
import type { AiMessage, AiConversation, AiChatResponse, AiArtifact } from "../../../shared/api/aiApi";

function formatRelativeTime(dateStr: string): string {
  const now = Date.now();
  const diff = now - new Date(dateStr).getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return "방금 전";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "어제";
  if (days < 7) return `${days}일 전`;
  return new Date(dateStr).toLocaleDateString();
}

function renderMarkdown(text: string): string {
  let html = text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

  // Code blocks
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_m, _lang, code) =>
    `<pre class="bg-slate-800 text-slate-100 rounded-lg p-4 my-2 overflow-x-auto text-sm"><code>${code.trim()}</code></pre>`
  );
  // Inline code
  html = html.replace(/`([^`]+)`/g, '<code class="bg-slate-100 text-slate-800 px-1.5 py-0.5 rounded text-sm">$1</code>');
  // Bold
  html = html.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
  // Italic
  html = html.replace(/\*(.+?)\*/g, "<em>$1</em>");
  // Unordered lists
  html = html.replace(/^- (.+)$/gm, '<li class="ml-4 list-disc">$1</li>');
  // Ordered lists
  html = html.replace(/^\d+\. (.+)$/gm, '<li class="ml-4 list-decimal">$1</li>');
  // Line breaks
  html = html.replace(/\n/g, "<br/>");

  return html;
}

interface ChatMessage {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
  artifacts?: AiArtifact[];
  queryResult?: AiChatResponse["queryResult"];
}

function ArtifactButton({ artifact }: { artifact: AiArtifact }) {
  const { t } = useTranslation();
  const colorMap: Record<string, string> = {
    excel: "text-green-600 bg-green-50",
    pdf: "text-red-600 bg-red-50",
    csv: "text-slate-600 bg-slate-100",
    chart: "text-blue-600 bg-blue-50",
  };
  return (
    <a
      href={artifact.downloadUrl}
      download={artifact.filename}
      className={clsx(
        "inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition hover:opacity-80",
        colorMap[artifact.type] || "text-slate-600 bg-slate-100"
      )}
    >
      <Download size={14} />
      {artifact.filename}
      <span className="text-xs opacity-60">{t("ai.download")}</span>
    </a>
  );
}

function QueryResultTable({ queryResult }: { queryResult: NonNullable<AiChatResponse["queryResult"]> }) {
  return (
    <div className="my-3 overflow-x-auto rounded-lg border border-slate-200">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-slate-50">
            {queryResult.columns.map((col, i) => (
              <th key={i} className="px-3 py-2 text-left font-semibold text-slate-700 whitespace-nowrap">
                {col}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {queryResult.data.map((row, ri) => (
            <tr key={ri} className="border-t border-slate-100">
              {row.map((cell, ci) => (
                <td key={ci} className="px-3 py-2 text-slate-600 whitespace-nowrap">
                  {String(cell ?? "")}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {queryResult.totalCount > queryResult.data.length && (
        <div className="bg-slate-50 px-3 py-1.5 text-xs text-slate-400 text-right">
          {queryResult.data.length} / {queryResult.totalCount}
        </div>
      )}
    </div>
  );
}

export default function AiChatPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const [sessionId, setSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [hoveredConv, setHoveredConv] = useState<string | null>(null);

  // Fetch conversations
  const { data: conversations } = useQuery({
    queryKey: ["ai-conversations"],
    queryFn: () => aiApi.getConversations(),
    select: (res) => res.data?.data || [],
  });

  // Chat mutation
  const chatMutation = useMutation({
    mutationFn: (message: string) => aiApi.chat({ sessionId: sessionId || undefined, message }),
    onSuccess: (res) => {
      const data = res.data?.data;
      if (data) {
        if (!sessionId) setSessionId(data.sessionId);
        setMessages((prev) => [
          ...prev,
          {
            id: `assistant-${Date.now()}`,
            role: "ASSISTANT",
            content: data.message,
            createdAt: new Date().toISOString(),
            artifacts: data.artifacts,
            queryResult: data.queryResult,
          },
        ]);
        queryClient.invalidateQueries({ queryKey: ["ai-conversations"] });
      }
    },
    onError: () => {
      setMessages((prev) => [
        ...prev,
        {
          id: `error-${Date.now()}`,
          role: "ASSISTANT",
          content: t("ai.errorMessage"),
          createdAt: new Date().toISOString(),
        },
      ]);
    },
  });

  // Delete conversation
  const deleteMutation = useMutation({
    mutationFn: (sid: string) => aiApi.deleteConversation(sid),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ai-conversations"] });
      if (sessionId && conversations?.find((c) => c.sessionId === sessionId)) {
        handleNewConversation();
      }
    },
  });

  // Load conversation messages
  const loadConversation = useCallback(async (conv: AiConversation) => {
    setSessionId(conv.sessionId);
    try {
      const res = await aiApi.getMessages(conv.sessionId);
      const msgs: AiMessage[] = res.data?.data || [];
      setMessages(
        msgs
          .filter((m) => m.role !== "SYSTEM")
          .map((m) => ({
            id: String(m.id),
            role: m.role as "USER" | "ASSISTANT",
            content: m.content,
            createdAt: m.createdAt,
          }))
      );
    } catch {
      setMessages([]);
    }
  }, []);

  const handleNewConversation = () => {
    setSessionId(null);
    setMessages([]);
    setInput("");
  };

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed || chatMutation.isPending) return;

    const userMsg: ChatMessage = {
      id: `user-${Date.now()}`,
      role: "USER",
      content: trimmed,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    chatMutation.mutate(trimmed);

    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleCopy = (content: string, id: string) => {
    navigator.clipboard.writeText(content);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleQuickAction = (action: string) => {
    setInput(action);
    setTimeout(() => handleSend(), 0);
  };

  // Auto-scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Auto-resize textarea
  const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    e.target.style.height = "auto";
    e.target.style.height = Math.min(e.target.scrollHeight, 120) + "px";
  };

  const quickActions = [
    { key: "suggestedSales", label: t("ai.suggestedSales") },
    { key: "suggestedStock", label: t("ai.suggestedStock") },
    { key: "suggestedReport", label: t("ai.suggestedReport") },
    { key: "suggestedPR", label: t("ai.suggestedPR") },
  ];

  return (
    <div className="flex h-[calc(100vh-12rem)] -m-6 rounded-[32px] overflow-hidden">
      {/* Sidebar */}
      <div
        className={clsx(
          "flex flex-col border-r border-slate-100 bg-white/60 transition-all duration-200",
          sidebarOpen ? "w-72" : "w-0 overflow-hidden"
        )}
      >
        <div className="flex items-center justify-between px-4 py-4 border-b border-slate-100">
          <h2 className="text-sm font-semibold text-slate-700 flex items-center gap-2">
            <MessageSquare size={16} />
            {t("ai.title")}
          </h2>
          <button
            onClick={handleNewConversation}
            className="flex items-center gap-1.5 rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-700 transition"
          >
            <Plus size={14} />
            {t("ai.newConversation")}
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-2 space-y-0.5">
          {!conversations || conversations.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-32 text-slate-400 text-sm">
              <MessageSquare size={24} className="mb-2 opacity-40" />
              {t("ai.noConversations")}
            </div>
          ) : (
            conversations.map((conv) => (
              <div
                key={conv.sessionId}
                onMouseEnter={() => setHoveredConv(conv.sessionId)}
                onMouseLeave={() => setHoveredConv(null)}
                onClick={() => loadConversation(conv)}
                className={clsx(
                  "flex items-center justify-between rounded-lg px-3 py-2.5 cursor-pointer transition-colors",
                  sessionId === conv.sessionId
                    ? "bg-brand-50 text-brand-700"
                    : "text-slate-600 hover:bg-slate-50"
                )}
              >
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium truncate">{conv.title}</p>
                  <p className="text-xs text-slate-400 mt-0.5">
                    {formatRelativeTime(conv.lastMessageAt)} · {conv.messageCount}
                  </p>
                </div>
                {hoveredConv === conv.sessionId && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      if (confirm(t("ai.deleteConfirm"))) {
                        deleteMutation.mutate(conv.sessionId);
                      }
                    }}
                    className="ml-2 rounded p-1 text-slate-400 hover:text-red-500 hover:bg-red-50 transition"
                  >
                    <Trash2 size={14} />
                  </button>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex flex-1 flex-col bg-white/40">
        {/* Chat Header */}
        <div className="flex items-center gap-3 border-b border-slate-100 px-5 py-3">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition md:hidden"
          >
            <ChevronLeft size={18} />
          </button>
          <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-brand-500 to-sky-500">
            <Bot size={16} className="text-white" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-slate-800">{t("ai.title")}</h3>
          </div>
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="ml-auto rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition hidden md:block"
          >
            <Maximize2 size={16} />
          </button>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
          {messages.length === 0 && (
            <div className="flex flex-col items-center justify-center h-full text-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-100 to-sky-100 mb-4">
                <Bot size={28} className="text-brand-600" />
              </div>
              <h3 className="text-lg font-semibold text-slate-800 mb-2">{t("ai.welcome")}</h3>
              <p className="text-sm text-slate-500 max-w-md mb-6">{t("ai.welcomeDesc")}</p>
              <div className="flex flex-wrap justify-center gap-2">
                {quickActions.map((qa) => (
                  <button
                    key={qa.key}
                    onClick={() => handleQuickAction(qa.label)}
                    className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 hover:border-brand-300 hover:text-brand-700 hover:bg-brand-50 transition"
                  >
                    {qa.label}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg) => (
            <div
              key={msg.id}
              className={clsx("flex gap-3", msg.role === "USER" ? "justify-end" : "justify-start")}
            >
              {msg.role === "ASSISTANT" && (
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-brand-500 to-sky-500 mt-0.5">
                  <Bot size={14} className="text-white" />
                </div>
              )}

              <div
                className={clsx(
                  "max-w-[70%] rounded-2xl px-4 py-3 relative group",
                  msg.role === "USER"
                    ? "bg-brand-600 text-white"
                    : msg.id.startsWith("error")
                    ? "bg-red-50 text-red-700 border border-red-100"
                    : "bg-white border border-slate-100 text-slate-800 shadow-sm"
                )}
              >
                {msg.id.startsWith("error") && (
                  <AlertCircle size={14} className="inline mr-1.5 -mt-0.5" />
                )}

                <div
                  className={clsx("text-sm leading-relaxed", msg.role === "USER" ? "text-white" : "")}
                  dangerouslySetInnerHTML={{
                    __html: msg.role === "USER" ? msg.content.replace(/\n/g, "<br/>") : renderMarkdown(msg.content),
                  }}
                />

                {msg.queryResult && <QueryResultTable queryResult={msg.queryResult} />}

                {msg.artifacts && msg.artifacts.length > 0 && (
                  <div className="flex flex-wrap gap-2 mt-3">
                    {msg.artifacts.map((a, i) => (
                      <ArtifactButton key={i} artifact={a} />
                    ))}
                  </div>
                )}

                <div className="flex items-center justify-between mt-2">
                  <span
                    className={clsx(
                      "text-[11px]",
                      msg.role === "USER" ? "text-white/60" : "text-slate-400"
                    )}
                  >
                    {formatRelativeTime(msg.createdAt)}
                  </span>

                  {msg.role === "ASSISTANT" && !msg.id.startsWith("error") && (
                    <button
                      onClick={() => handleCopy(msg.content, msg.id)}
                      className="opacity-0 group-hover:opacity-100 rounded p-1 text-slate-400 hover:text-slate-600 transition"
                      title={t("ai.copied")}
                    >
                      {copiedId === msg.id ? <Check size={13} /> : <Copy size={13} />}
                    </button>
                  )}
                </div>
              </div>

              {msg.role === "USER" && (
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-brand-100 mt-0.5">
                  <User size={14} className="text-brand-700" />
                </div>
              )}
            </div>
          ))}

          {chatMutation.isPending && (
            <div className="flex gap-3">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-brand-500 to-sky-500 mt-0.5">
                <Bot size={14} className="text-white" />
              </div>
              <div className="bg-white border border-slate-100 rounded-2xl px-4 py-3 shadow-sm">
                <div className="flex items-center gap-2 text-sm text-slate-500">
                  <div className="flex gap-1">
                    <span className="h-2 w-2 rounded-full bg-brand-400 animate-bounce" style={{ animationDelay: "0ms" }} />
                    <span className="h-2 w-2 rounded-full bg-brand-400 animate-bounce" style={{ animationDelay: "150ms" }} />
                    <span className="h-2 w-2 rounded-full bg-brand-400 animate-bounce" style={{ animationDelay: "300ms" }} />
                  </div>
                  {t("ai.thinking")}
                </div>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Quick Actions (when there are messages) */}
        {messages.length > 0 && !chatMutation.isPending && (
          <div className="flex flex-wrap gap-1.5 px-5 pb-2">
            {quickActions.map((qa) => (
              <button
                key={qa.key}
                onClick={() => handleQuickAction(qa.label)}
                className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs text-slate-500 hover:border-brand-300 hover:text-brand-600 transition"
              >
                {qa.label}
              </button>
            ))}
          </div>
        )}

        {/* Input Area */}
        <div className="border-t border-slate-100 px-5 py-3">
          <div className="flex items-end gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-2 focus-within:border-brand-300 focus-within:ring-2 focus-within:ring-brand-100 transition">
            <textarea
              ref={textareaRef}
              value={input}
              onChange={handleTextareaChange}
              onKeyDown={handleKeyDown}
              placeholder={t("ai.placeholder")}
              rows={1}
              className="flex-1 resize-none border-0 bg-transparent py-1.5 text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none focus:ring-0"
              style={{ maxHeight: 120 }}
            />
            <button
              onClick={handleSend}
              disabled={!input.trim() || chatMutation.isPending}
              className={clsx(
                "flex h-9 w-9 shrink-0 items-center justify-center rounded-xl transition",
                input.trim() && !chatMutation.isPending
                  ? "bg-brand-600 text-white hover:bg-brand-700"
                  : "bg-slate-100 text-slate-400 cursor-not-allowed"
              )}
            >
              <Send size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
