export interface SourceDocument {
  fileName: string;
  title: string;
  pageNumber: number;
  snippet: string;
}

export interface Message {
  from: "USER" | "BOT";
  text: string;
  sources?: SourceDocument[];
  createdAt?: Date;
}

export interface MessageResponse {
  id: string;
  chatId: number;
  sender: 'USER' | 'BOT';
  content: unknown;
  contentType: string;
  sequence: number;
  sources: SourceDocument[];
  createdAt: Date
}

export interface ChatResponse {
  id: number;
  memberEmail: string;
  title: string;
  lastMessagePreview: string;
  archived: boolean;
  messages: MessageResponse[];
  createdAt: Date;
  updatedAt: Date;
}

export function convertToMessage(messages: MessageResponse[]): Message[] {
  const result: Message[] = []
  messages.forEach(message => {
    result.push({
      from: message.sender,
      text: `${message.content}`,
      sources: message.sources,
      createdAt: message.createdAt,
    })
  })
  return result;
}