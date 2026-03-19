import client from "./client";
import type { ApiResponse } from "./client";

export interface AiChatRequest {
  sessionId?: string;
  message: string;
}

export interface AiArtifact {
  type: 'excel' | 'pdf' | 'csv' | 'chart';
  filename: string;
  downloadUrl: string;
}

export interface AiChatResponse {
  sessionId: string;
  message: string;
  artifacts?: AiArtifact[];
  suggestedActions?: string[];
  queryResult?: {
    columns: string[];
    data: any[][];
    totalCount: number;
  };
}

export interface AiConversation {
  id: number;
  sessionId: string;
  title: string;
  messageCount: number;
  lastMessageAt: string;
}

export interface AiMessage {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt: string;
  metadata?: string;
}

export const aiApi = {
  chat: (req: AiChatRequest) => client.post<ApiResponse<AiChatResponse>>('/api/v1/ai/chat', req),
  getConversations: () => client.get<ApiResponse<AiConversation[]>>('/api/v1/ai/conversations'),
  getMessages: (sessionId: string) => client.get<ApiResponse<AiMessage[]>>(`/api/v1/ai/conversations/${sessionId}/messages`),
  deleteConversation: (sessionId: string) => client.delete(`/api/v1/ai/conversations/${sessionId}`),
  query: (message: string) => client.post<ApiResponse<AiChatResponse>>('/api/v1/ai/query', { message }),
  generateReport: (message: string) => client.post<ApiResponse<AiChatResponse>>('/api/v1/ai/report', { message }),
};
