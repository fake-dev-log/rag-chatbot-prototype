import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateChat } from '@apis/hooks/chat';
import Logo from "@components/Logo.tsx";
import {APP_NAME} from "@constants";
import {useDocumentCategories} from "@apis/hooks/document.ts";
import ChatInput from "@components/ChatInput.tsx";

export default function Home() {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState("");
  const navigate = useNavigate();
  const createChat = useCreateChat();
  const { data: categories } = useDocumentCategories();

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!query.trim() || createChat.isPending) return;

    try {
      const newId = await createChat.mutateAsync();
      navigate(`/chats/${newId}`, { state: { initialQuery: query, category: category } });
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="flex flex-col flex-1 h-screen bg-background-light dark:bg-background-dark transition-colors duration-300 justify-center items-center p-4">
      <div className="flex flex-col p-8 items-center">
        <Logo size={64} />
        <h1 className="text-5xl font-bold mt-4 text-gray-800 dark:text-gray-100">{APP_NAME}</h1>
      </div>

      <ChatInput
        query={query}
        setQuery={setQuery}
        category={category}
        setCategory={setCategory}
        categories={categories ?? []}
        isStreaming={createChat.isPending}
        onSubmit={onSubmit}
      />
    </div>
  );
}
