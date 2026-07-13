export interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  tokens?: number;
  timeMs?: number;
}

export interface Session {
  id: string;
  name: string;
  timestamp: string;
  messages: Message[];
}
