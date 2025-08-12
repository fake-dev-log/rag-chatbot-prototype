import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateChat } from '@apis/hooks/chat';
import { Spinner } from '@components/Spinner';
import Logo from "@components/Logo.tsx";
import {APP_NAME} from "@constants";

export default function Home() {
  const [query, setQuery] = useState('');
  const navigate = useNavigate();
  const createChat = useCreateChat();

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!query.trim() || createChat.isPending) return;

    try {
      // 1) 채팅방 생성
      const newId = await createChat.mutateAsync();
      // 2) 생성된 방으로 네비게이트 + 초기 쿼리 전달
      navigate(`/chats/${newId}`, { state: { initialQuery: query } });
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div
      className="
        flex flex-col h-screen w-full
        bg-background-light dark:bg-background-dark
        transition-colors duration-300
        justify-center items-center p-4
      "
    >
      <div className="flex flex-col p-8 items-center">
        <Logo size={64} />
        <h1 className="text-5xl font-bold mt-4 text-gray-800 dark:text-gray-100">{APP_NAME}</h1>
      </div>
      <form
        onSubmit={onSubmit}
        className="w-full max-w-lg flex flex-col space-y-4"
      >
        <input
          type="text"
          className="
            w-full p-3 rounded-lg border
            bg-surface-light text-text-light border-secondary-light
            dark:bg-surface-dark dark:text-text-dark dark:border-secondary-dark
            focus:outline-none focus:ring-2 focus:ring-primary-light dark:focus:ring-primary-dark
            disabled:opacity-50 transition-colors duration-300
          "
          placeholder="무엇이 궁금하신가요?"
          value={query}
          onChange={e => setQuery(e.target.value)}
          disabled={createChat.isPending}
          autoFocus
        />
        <button
          type="submit"
          disabled={createChat.isPending}
          className="
            w-full py-3 rounded-lg text-surface-light
            bg-primary-light hover:bg-primary-dark
            dark:bg-primary-dark dark:hover:bg-primary-light
            disabled:opacity-50 transition-colors duration-300
          "
        >
          {createChat.isPending ? <Spinner /> : '대화 시작'}
        </button>
      </form>
    </div>
  );
}