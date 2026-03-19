import { useState, useRef, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useMutation } from "@tanstack/react-query";
import { Bot, Send, X, Maximize2, AlertCircle } from "lucide-react";
import { clsx } from "clsx";
import { aiApi } from "../api/aiApi";
import type { AiChatResponse, AiArtifact } from "../api/aiApi";

interface WidgetMessage {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
}

const STORAGE_KEY = "ai-widget-open";

export default function AiChatWidget() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(() => localStorage.getItem(STORAGE_KEY) === "true");
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<WidgetMessage[]>([]);
  const [input, setInput] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const toggle = useCallback(() => {
    setOpen((prev) => {
      const next = !prev;
      localStorage.setItem(STORAGE_KEY, String(next));
      return next;
    });
  }, []);

  // Keyboard shortcut: Ctrl+Shift+A
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key === "A") {
        e.preventDefault();
        toggle();
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [toggle]);

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
          },
        ]);
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

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed || chatMutation.isPending) return;

    setMessages((prev) => [
      ...prev,
      { id: `user-${Date.now()}`, role: "USER", content: trimmed, createdAt: new Date().toISOString() },
    ]);
    setInput("");
    chatMutation.mutate(trimmed);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  if (!open) {
    return (
      <button
        onClick={toggle}
        className="fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-br from-brand-600 to-sky-500 text-white shadow-lg hover:shadow-xl transition-all hover:scale-105"
        title="AI Assistant (Ctrl+Shift+A)"
      >
        <Bot size={24} />
      </button>
    );
  }

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col w-[400px] h-[500px] rounded-2xl border border-slate-200 bg-white shadow-2xl overflow-hidden animate-in slide-in-from-bottom-4 duration-200">
      {/* Header */}
      <div className="flex items-center justify-between bg-gradient-to-r from-brand-600 to-sky-500 px-4 py-3">
        <div className="flex items-center gap-2 text-white">
          <Bot size={18} />
          <span className="text-sm font-semibold">{t("ai.title")}</span>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={() => {
              toggle();
              navigate("/ai-chat");
            }}
            className="rounded-lg p-1.5 text-white/80 hover:text-white hover:bg-white/10 transition"
            title={t("ai.openFullScreen")}
          >
            <Maximize2 size={14} />
          </button>
          <button
            onClick={toggle}
            className="rounded-lg p-1.5 text-white/80 hover:text-white hover:bg-white/10 transition"
          >
            <X size={14} />
          </button>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center">
            <Bot size={32} className="text-brand-300 mb-3" />
            <p className="text-sm text-slate-500">{t("ai.welcome")}</p>
            <p className="text-xs text-slate-400 mt-1">{t("ai.welcomeDesc")}</p>
          </div>
        )}

        {messages.map((msg) => (
          <div
            key={msg.id}
            className={clsx("flex", msg.role === "USER" ? "justify-end" : "justify-start")}
          >
            <div
              className={clsx(
                "max-w-[80%] rounded-2xl px-3.5 py-2.5 text-sm",
                msg.role === "USER"
                  ? "bg-brand-600 text-white"
                  : msg.id.startsWith("error")
                  ? "bg-red-50 text-red-700 border border-red-100"
                  : "bg-slate-100 text-slate-800"
              )}
            >
              {msg.id.startsWith("error") && <AlertCircle size={12} className="inline mr-1 -mt-0.5" />}
              <span className="whitespace-pre-wrap">{msg.content}</span>
            </div>
          </div>
        ))}

        {chatMutation.isPending && (
          <div className="flex justify-start">
            <div className="bg-slate-100 rounded-2xl px-3.5 py-2.5">
              <div className="flex gap-1">
                <span className="h-1.5 w-1.5 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: "0ms" }} />
                <span className="h-1.5 w-1.5 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: "150ms" }} />
                <span className="h-1.5 w-1.5 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: "300ms" }} />
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Full Screen Link */}
      <div className="px-4 pb-1">
        <button
          onClick={() => {
            toggle();
            navigate("/ai-chat");
          }}
          className="text-xs text-brand-600 hover:text-brand-700 hover:underline transition"
        >
          {t("ai.openFullScreen")}
        </button>
      </div>

      {/* Input */}
      <div className="border-t border-slate-100 px-3 py-2">
        <div className="flex items-end gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 focus-within:border-brand-300">
          <textarea
            ref={textareaRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={t("ai.placeholder")}
            rows={1}
            className="flex-1 resize-none border-0 bg-transparent py-1 text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none focus:ring-0"
            style={{ maxHeight: 80 }}
          />
          <button
            onClick={handleSend}
            disabled={!input.trim() || chatMutation.isPending}
            className={clsx(
              "flex h-7 w-7 shrink-0 items-center justify-center rounded-lg transition",
              input.trim() && !chatMutation.isPending
                ? "bg-brand-600 text-white hover:bg-brand-700"
                : "bg-slate-200 text-slate-400 cursor-not-allowed"
            )}
          >
            <Send size={13} />
          </button>
        </div>
      </div>
    </div>
  );
}
