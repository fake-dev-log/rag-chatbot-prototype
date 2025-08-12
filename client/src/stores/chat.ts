import { create } from "zustand";
import type {Message, SourceDocument } from "@apis/types/chat";

interface ChatState {
  messages: Message[];
  setMessages: (messages: Message[]) => void;
  addMessage: (msg: Message) => void;
  appendToLastBot: (text: string) => void;
  setLastBotTime: () => void;
  setSourcesOnLastBot: (sources: SourceDocument[]) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],

  setMessages: (messages) => {
    set((state) => ({
      messages: [...state.messages, ...messages]
    }))
  },

  addMessage: (msg) =>
    set((state) => ({
      messages: [...state.messages, msg],
    })),

  appendToLastBot: (text) =>
    set((state) => {
      const msgs = [...state.messages]
      if (msgs.length === 0) return state
      const last = msgs[msgs.length - 1]
      if (last.from === "BOT") {
        msgs[msgs.length - 1] = {
          ...last,
          text: last.text + text,
        }
      }
      return { messages: msgs }
    }),

  setLastBotTime: () =>
    set((state) => {
      const msgs = [...state.messages]
      if (msgs.length === 0) return state
      const last = msgs[msgs.length - 1]
      if (last.from === 'BOT') {
        msgs[msgs.length - 1] = {
          ...last,
          createdAt: new Date(),
        }
      }
      return { messages: msgs }
    }),

  setSourcesOnLastBot: (sources) =>
    set((state) => {
      const msgs = [...state.messages]
      if (msgs.length === 0) return state
      const last = msgs[msgs.length - 1]
      if (last.from === "BOT") {
        msgs[msgs.length - 1] = {
          ...last,
          sources,         // ← attach sources
        }
      }
      return { messages: msgs }
    }),

  clearMessages: () =>
    set(() => ({
      messages: [],
    })),
}))
