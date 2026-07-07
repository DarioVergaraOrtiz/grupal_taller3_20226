export interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

export interface Session {
  id: string;
  name: string;
  timestamp: string;
  messages: Message[];
}
