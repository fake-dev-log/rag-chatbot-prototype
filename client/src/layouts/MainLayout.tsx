import { Outlet, Link } from 'react-router-dom';
import Sidebar from '@components/Sidebar';
import { useChatList } from '@apis/hooks/chat';
import { useState } from 'react';
import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';

export default function MainLayout() {
  const { data: chats } = useChatList();
  const [historyCollapsed, setHistoryCollapsed] = useState(false);

  return (
    <div className="flex h-screen">
      <Sidebar />
      <div className={`bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-200 transition-all duration-300 ${historyCollapsed ? 'w-0' : 'w-64'} overflow-hidden`}>
        <div className="p-4">
          <h2 className="text-lg font-semibold mb-4">Chat History</h2>
          <ul>
            {chats?.map(chat => (
              <li key={chat.id} className="mb-2">
                <Link to={`/chats/${chat.id}`}
                      className="block p-2 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700">
                  <div className="hover-scroll-container">
                    <span className="hover-scroll-text text-gray-800 dark:text-gray-100">
                      {chat.title}
                    </span>
                  </div>
                  <div className="hover-scroll-container">
                    <span className="hover-scroll-text text-sm text-gray-600 dark:text-gray-300">
                      {chat.lastMessagePreview}
                    </span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <div className="flex-1 bg-background-light dark:bg-background-dark relative">
        <button
          onClick={() => setHistoryCollapsed(!historyCollapsed)} 
          className="
            absolute top-1/2 -left-3 transform -translate-y-1/2
            bg-white dark:bg-gray-800 p-2 rounded-full shadow-md z-10
            border border-gray-300 dark:border-gray-600
            text-gray-600 dark:text-gray-300 hover:text-indigo-600 dark:hover:text-indigo-400
          "
        >
          {historyCollapsed ? <FiChevronRight size={20} /> : <FiChevronLeft size={20} />}
        </button>
        <Outlet />
      </div>
    </div>
  );
}