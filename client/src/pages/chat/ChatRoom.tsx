import {useLocation, useParams} from 'react-router-dom';
import { useChatStore } from '@stores/chat';
import {useChat, useChatHistory} from '@apis/hooks/chat';
import NotFound from '@pages/error/NotFound';
import ChatWindow from '@pages/chat/ChatWindow';
import {useEffect, useRef} from "react";
import {convertToMessage} from "@apis/types/chat.ts";
import NoSuchContent from "@pages/error/NoSuchContent.tsx";

export default function ChatRoom() {
  const { chatId } = useParams<{ chatId: string }>();
  const id = chatId ? Number(chatId) : NaN;
  const { state } = useLocation();
  const initialQueryRef = useRef(state?.initialQuery);

  const { mutate, isPending } = useChat();
  const { data: chatHistory, isFetching } = useChatHistory(id, { skipWhenInitialQuery: !!initialQueryRef.current });
  const messages = useChatStore(s => s.messages);
  const setMessages = useChatStore(s => s.setMessages)
  const clearMessages = useChatStore(s => s.clearMessages)

  useEffect(() => {
    clearMessages();

    if (initialQueryRef.current) {
      mutate({ chatId: id, query: initialQueryRef.current });
      initialQueryRef.current = undefined;
    }
  }, [clearMessages, id, mutate]);

  useEffect(() => {
    if (chatHistory && !isPending) {
      setMessages(convertToMessage(chatHistory.messages ?? []));
    }
  }, [chatHistory, isPending, setMessages]);

  if (isNaN(id) && !isPending && !isFetching) return <NotFound />;
  if (!chatHistory && !isPending && !isFetching) return <NoSuchContent />;

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
        isPending={isPending}
        onSubmit={query => mutate({chatId: id, query})}
      />
    </div>
  );
}