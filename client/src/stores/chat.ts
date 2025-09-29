import { create } from "zustand";
import type {Message, SourceDocument } from "@apis/types/chat";

interface ChatState {
  messagesByChatId: Record<number, Message[]>;
  streamingChatIds: number[];

  setMessagesForChat: (chatId: number, messages: Message[]) => void;
  addMessageToChat: (chatId: number, msg: Message) => void;
  appendToLastBotInChat: (chatId: number, text: string) => void;
  setSourcesOnLastBotInChat: (chatId: number, sources: SourceDocument[]) => void;
  setLastBotTimeInChat: (chatId: number) => void;
  clearMessagesForChat: (chatId: number) => void;

  addStreamingId: (chatId: number) => void;
  removeStreamingId: (chatId: number) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messagesByChatId: {},
  streamingChatIds: [],

  addStreamingId: (chatId) =>
    set((state) => ({
      streamingChatIds: [...state.streamingChatIds, chatId],
    })),

  removeStreamingId: (chatId) =>
    set((state) => ({
      streamingChatIds: state.streamingChatIds.filter((id) => id !== chatId),
    })),

  setMessagesForChat: (chatId, messages) =>
    set((state) => ({
      messagesByChatId: {
        ...state.messagesByChatId,
        [chatId]: messages,
      },
    })),

  addMessageToChat: (chatId, msg) =>
    set((state) => {
      const currentMessages = state.messagesByChatId[chatId] ?? [];
      return {
        messagesByChatId: {
          ...state.messagesByChatId,
          [chatId]: [...currentMessages, msg],
        },
      };
    }),

  appendToLastBotInChat: (chatId, text) =>
    set((state) => {
      const currentMessages = state.messagesByChatId[chatId] ?? [];
      if (currentMessages.length === 0) return state;

      const last = currentMessages[currentMessages.length - 1];
      if (last.from === "BOT") {
        const updatedLast = { ...last, text: last.text + text };
        return {
          messagesByChatId: {
            ...state.messagesByChatId,
            [chatId]: [...currentMessages.slice(0, -1), updatedLast],
          },
        };
      }
      return state;
    }),

  setSourcesOnLastBotInChat: (chatId, sources) =>
    set((state) => {
      const currentMessages = state.messagesByChatId[chatId] ?? [];
      if (currentMessages.length === 0) return state;

      const last = currentMessages[currentMessages.length - 1];
      if (last.from === "BOT") {
        const updatedLast = { ...last, sources };
        return {
          messagesByChatId: {
            ...state.messagesByChatId,
            [chatId]: [...currentMessages.slice(0, -1), updatedLast],
          },
        };
      }
      return state;
    }),

  setLastBotTimeInChat: (chatId) =>
    set((state) => {
      const currentMessages = state.messagesByChatId[chatId] ?? [];
      if (currentMessages.length === 0) return state;

      const last = currentMessages[currentMessages.length - 1];
      if (last.from === 'BOT') {
        const updatedLast = { ...last, createdAt: new Date() };
        return {
          messagesByChatId: {
            ...state.messagesByChatId,
            [chatId]: [...currentMessages.slice(0, -1), updatedLast],
          },
        };
      }
      return state;
    }),

  clearMessagesForChat: (chatId) =>
    set((state) => ({
      messagesByChatId: {
        ...state.messagesByChatId,
        [chatId]: [],
      },
    })),
}));
