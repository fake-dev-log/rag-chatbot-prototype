import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query"
import { useChatStore } from "@stores/chat"
import { API_BASE_URL, baseURL } from "@apis/types/common"
import {type ChatResponse, type SourceDocument} from "@apis/types/chat"
import {fetchWithAuth} from "@apis/fetchWithAuth.ts";
import api from "@apis/index";

/**
 * Fetches a list of all chat rooms for the current user.
 * @returns A TanStack Query object for the chat list.
 */
export function useChatList() {
  return useQuery({
    queryKey: ['chat', 'list'],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_BASE_URL.chats}`);
      return response.data as ChatResponse[];
    },
  })
}

/**
 * Fetches the complete message history for a specific chat room.
 * @param chatId The ID of the chat room to fetch.
 * @param opts Options to control the query's behavior.
 * @returns A TanStack Query object for the chat history.
 */
export function useChatHistory(chatId: number, opts?: { skipWhenInitialQuery: boolean}) {
  return useQuery({
    queryKey: ['chat', chatId],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_BASE_URL.chats}/${chatId}`);
      return response.data as ChatResponse
    },
    // The query will only run if `chatId` is a valid number.
    // `skipWhenInitialQuery` can be used to prevent fetching until a condition is met.
    enabled: !isNaN(chatId) && !(opts?.skipWhenInitialQuery ?? false),
  })
}

/**
 * A mutation hook for creating a new chat room.
 * @returns A TanStack Mutation object for creating a chat.
 */
export function useCreateChat() {
  const queryClient = useQueryClient();
  return useMutation<number, Error, void>({
    mutationFn: async () => {
      const res = await fetchWithAuth(`${baseURL}${API_BASE_URL.chats}`, {
        method: "POST",
      });
      if (!res.ok) {
        const errorResponse = await res.json();
        throw new Error(errorResponse.message || "Failed to create chat room");
      }
      return res.json();
    },
    // After a new chat is created successfully, invalidate the chat list query
    // to trigger a refetch and update the UI with the new chat room.
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['chat', 'list'] });
    }
  });
}

/**
 * A mutation hook for sending a message to a chat room and processing the streamed response.
 * This hook handles the entire lifecycle of a message send, including optimistic updates
 * and real-time processing of the NDJSON stream from the server.
 * @returns A TanStack Mutation object for sending a message.
 */
export function useChat() {
  const addMessageToChat = useChatStore((s) => s.addMessageToChat);
  const appendToLastBotInChat = useChatStore((s) => s.appendToLastBotInChat);
  const setLastBotTimeInChat = useChatStore((s) => s.setLastBotTimeInChat);
  const setSourcesOnLastBotInChat = useChatStore((s) => s.setSourcesOnLastBotInChat);
  const addStreamingId = useChatStore((s) => s.addStreamingId);
  const removeStreamingId = useChatStore((s) => s.removeStreamingId);

  return useMutation<ReadableStreamDefaultReader<Uint8Array>, Error, { chatId: number, query: string, category?: string | null }>({
    mutationFn: async ({ chatId, query, category }) => {
      const res = await fetchWithAuth(`${baseURL}${API_BASE_URL.chats}/${chatId}/message`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ query, category }),
      });
      if (!res.ok) {
        const errorResponse = await res.json();
        throw new Error(errorResponse.message || `HTTP ${res.status}`);
      }
      return res.body!.getReader();
    },

    onMutate: ({ chatId, query }) => {
      addMessageToChat(chatId, { from: 'USER', text: query, createdAt: new Date() });
      addMessageToChat(chatId, { from: "BOT", text: "" });
      addStreamingId(chatId);
    },

    onSuccess: async (reader, { chatId }) => {
      const decoder = new TextDecoder();
      let buf = "";
      let done = false;

      while (!done) {
        const { value, done: finished } = await reader.read();
        done = finished;
        buf += decoder.decode(value, { stream: true });

        let idx: number;
        while ((idx = buf.indexOf("\n")) !== -1) {
          const line = buf.slice(0, idx).trim();
          buf = buf.slice(idx + 1);
          if (!line) continue;

          const { type, data } = JSON.parse(line) as {
            type: "token" | "sources" | "done" | "error";
            data: string | SourceDocument[] | boolean;
          };

          if (type === "token") {
            appendToLastBotInChat(chatId, data as string);
          } else if (type === "sources") {
            setSourcesOnLastBotInChat(chatId, data as SourceDocument[]);
          } else if (type === "error") {
            throw new Error(data as string || "An unknown error occurred");
          } else if (type === "done") {
            done = data as boolean;
          }
        }
      }
      setLastBotTimeInChat(chatId);
      removeStreamingId(chatId);
    },

    onError: (_, { chatId }) => {
      removeStreamingId(chatId);
      // You might want to add a message to the chat indicating an error occurred.
    },
  });
}