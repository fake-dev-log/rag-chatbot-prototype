import {useLocation, useParams} from 'react-router-dom';
import { useChatStore } from '@stores/chat';
import {useChat, useChatHistory} from '@apis/hooks/chat';
import NotFound from '@pages/error/NotFound';
import ChatWindow from '@pages/chat/ChatWindow';
import {useEffect, useRef, useState} from "react";
import {useDocumentCategories} from "@apis/hooks/document.ts";
import {convertToMessage, type Message} from "@apis/types/chat.ts";
import NoSuchContent from "@pages/error/NoSuchContent.tsx";

const emptyMessages: Message[] = []; // Stable empty array reference to prevent re-renders

export default function ChatRoom() {
  const { chatId } = useParams<{ chatId: string }>();
  const id = chatId ? Number(chatId) : NaN;
  const { state } = useLocation();
  const initialQuery = useRef(state?.initialQuery);
  const initialCategory = useRef(state?.category);

  const { mutate } = useChat();
  const { data: chatHistory, isFetching } = useChatHistory(id, { skipWhenInitialQuery: !!initialQuery.current });
  const { data: categories } = useDocumentCategories();
  const [selectedCategory, setSelectedCategory] = useState<string>("");

  // Select state and actions from the store. Actions are stable and won't cause re-renders.
  const messages = useChatStore(s => s.messagesByChatId[id] ?? emptyMessages);
  const setMessagesForChat = useChatStore(s => s.setMessagesForChat);
  const clearMessagesForChat = useChatStore(s => s.clearMessagesForChat);
  const streamingChatIds = useChatStore(s => s.streamingChatIds);

  const isThisRoomStreaming = streamingChatIds.includes(id);

  // Effect for handling the initial query from the home page
  useEffect(() => {
    if (initialQuery.current) {
      const categoryToUse = initialCategory.current ?? "";
      if (initialCategory.current) {
        setSelectedCategory(initialCategory.current);
      }
      clearMessagesForChat(id);
      mutate({ chatId: id, query: initialQuery.current, category: categoryToUse });
      initialQuery.current = undefined;
      initialCategory.current = undefined;
    }
  }, [id, clearMessagesForChat, mutate, setSelectedCategory]);

  // Effect for loading chat history from the server into the store
  useEffect(() => {
    // This effect synchronizes server-side chat history with the client-side Zustand store.
    // It runs only when chatHistory is fetched and the local store for this chat is empty.
    // This prevents an infinite loop where the store update would cause a re-render,
    // which would then re-trigger this effect.
    if (chatHistory && messages.length === 0 && !isThisRoomStreaming) {
      setMessagesForChat(id, convertToMessage(chatHistory.messages ?? []));
    }
  }, [id, chatHistory, messages.length, isThisRoomStreaming, setMessagesForChat]);

  if (isNaN(id)) return <NotFound />;
  if (!chatHistory && !isFetching && messages.length === 0 && !initialQuery.current) return <NoSuchContent />;

  return (
    <div
      className="
        flex flex-col h-screen w-full p-4
        bg-background-light dark:bg-background-dark
        transition-colors duration-300
      "
    >
      <ChatWindow
        messages={messages}
        isStreaming={isThisRoomStreaming}
        onSubmit={query => mutate({chatId: id, query, category: selectedCategory})}
        categories={categories ?? []}
        selectedCategory={selectedCategory}
        setSelectedCategory={setSelectedCategory}
      />
    </div>
  );
}