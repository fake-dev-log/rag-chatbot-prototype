import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query"
import { useChatStore } from "@stores/chat"
import { API_BASE_URL, baseURL } from "@apis/types/common"
import {type ChatResponse, convertToSourceDocuments, type OriginalSource} from "@apis/types/chat"
import {fetchWithAuth} from "@apis/fetchWithAuth.ts";
import api from "@apis/index";

export function useChatList() {
  return useQuery({
    queryKey: ['chat', 'list'],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_BASE_URL.chats}`);
      return response.data as ChatResponse[];
    },
  })
}

export function useChatHistory(chatId: number, opts?: { skipWhenInitialQuery: boolean}) {
  return useQuery({
    queryKey: ['chat', chatId],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_BASE_URL.chats}/${chatId}`);
      return response.data as ChatResponse
    },
    enabled: !!chatId && !(opts?.skipWhenInitialQuery ?? false),
  })
}

export function useCreateChat() {
  const queryClient = useQueryClient();
  return useMutation<number, Error, void>({
    mutationFn: async () => {
      const res = await fetchWithAuth(`${baseURL}${API_BASE_URL.chats}`, {
        method: "POST",
      });
      if (!res.ok) {
        const errorResponse = await res.json();
        throw new Error(errorResponse.message || "채팅방 생성 실패");
      }
      return res.json();
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['chat', 'list'] });
    }
  });
}

export function useChat() {
  const addMessage = useChatStore((s) => s.addMessage);
  const appendToLastBot = useChatStore((s) => s.appendToLastBot);
  const setLastBotTime = useChatStore((s) => s.setLastBotTime);
  const setSourcesOnLastBot = useChatStore((s) => s.setSourcesOnLastBot);

  return useMutation<ReadableStreamDefaultReader<Uint8Array>, Error, { chatId: number, query: string }>({
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

    onMutate: ({ query }) => {
      addMessage({ from: 'USER', text: query, createdAt: new Date() });
      addMessage({ from: "BOT", text: "" })
    },

    onSuccess: async (reader) => {
      const decoder = new TextDecoder()
      let buf = ""
      let done = false

      while (!done) {
        const { value, done: finished } = await reader.read()
        done = finished
        buf += decoder.decode(value, { stream: true })

        let idx: number
        while ((idx = buf.indexOf("\n")) !== -1) {
          const line = buf.slice(0, idx).trim()
          buf = buf.slice(idx + 1)
          if (!line) continue

          const { type, data } = JSON.parse(line) as {
            type: "token" | "sources" | "done" | "error"
            data: string | OriginalSource[] | boolean
          }

          if (type === "token") {
            appendToLastBot(data as string)
          } else if (type === "sources") {
            setSourcesOnLastBot(convertToSourceDocuments(data as OriginalSource[]))
          } else if (type === "error") {
            // Assuming data is a JSON string of ErrorResponse
            const errorData = data as string;
            throw new Error(errorData || "알 수 없는 오류 발생");
          } else if (type === "done") {
            done = data as boolean
          }
        }
      }
      setLastBotTime();
    },
  })
}
