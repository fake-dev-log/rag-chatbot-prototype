import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query"
import { useChatStore } from "@stores/chat"
import { API_BASE_URL, baseURL } from "@apis/types/common"
import {type ChatResponse, convertToSourceDocuments, type OriginalSource} from "@apis/types/chat"
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
    enabled: !!chatId && !(opts?.skipWhenInitialQuery ?? false),
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
  // Get actions from the Zustand chat store to update the UI state.
  const addMessage = useChatStore((s) => s.addMessage);
  const appendToLastBot = useChatStore((s) => s.appendToLastBot);
  const setLastBotTime = useChatStore((s) => s.setLastBotTime);
  const setSourcesOnLastBot = useChatStore((s) => s.setSourcesOnLastBot);

  return useMutation<ReadableStreamDefaultReader<Uint8Array>, Error, { chatId: number, query: string }>({
    /**
     * The core mutation function that sends the request to the server.
     * @returns A ReadableStreamDefaultReader to process the streamed response.
     */
    mutationFn: async ({ chatId, query }) => {
      const res = await fetchWithAuth(`${baseURL}${API_BASE_URL.chats}/${chatId}/message`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ query }),
      })
      if (!res.ok) {
        const errorResponse = await res.json();
        throw new Error(errorResponse.message || `HTTP ${res.status}`);
      }
      return res.body!.getReader()
    },

    /**
     * Called before the mutation function. This performs an optimistic update
     * to make the UI feel more responsive.
     */
    onMutate: ({ query }) => {
      // Immediately add the user's message to the chat window.
      addMessage({ from: 'USER', text: query, createdAt: new Date() });
      // Add an empty placeholder for the bot's response, which will be filled by the stream.
      addMessage({ from: "BOT", text: "" })
    },

    /**
     * Called on successful mutation, with the stream reader as the payload.
     * This function reads and processes the NDJSON stream from the server.
     */
    onSuccess: async (reader) => {
      const decoder = new TextDecoder()
      let buf = ""
      let done = false

      while (!done) {
        const { value, done: finished } = await reader.read()
        done = finished
        buf += decoder.decode(value, { stream: true })

        // Process buffer line by line, as it may contain multiple NDJSON objects.
        let idx: number
        while ((idx = buf.indexOf("\n")) !== -1) {
          const line = buf.slice(0, idx).trim()
          buf = buf.slice(idx + 1)
          if (!line) continue

          const { type, data } = JSON.parse(line) as {
            type: "token" | "sources" | "done" | "error"
            data: string | OriginalSource[] | boolean
          }

          // Handle different event types from the stream.
          if (type === "token") {
            appendToLastBot(data as string) // Append token to the bot's message.
          } else if (type === "sources") {
            setSourcesOnLastBot(convertToSourceDocuments(data as OriginalSource[])) // Set source documents.
          } else if (type === "error") {
            throw new Error(data as string || "An unknown error occurred");
          } else if (type === "done") {
            done = data as boolean // Mark the end of the stream.
          }
        }
      }
      // Set the final timestamp for the bot's message.
      setLastBotTime();
    },
  })
}