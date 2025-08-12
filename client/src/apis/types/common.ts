export const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const API_BASE_URL = {
  auth: "/auth",
  chats: "/chats",
} as const