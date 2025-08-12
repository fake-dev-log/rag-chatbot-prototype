import { useState } from 'react';
import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import type {ChatResponse} from "@apis/types/chat.ts";
import Logo from "@components/Logo.tsx";

type SidebarProps = {
  chats: ChatResponse[];
  onSelect: (id: number) => void;
  onHome: () => void;
};

export default function Sidebar({ chats, onSelect, onHome }: SidebarProps) {
  const [collapsed, setCollapsed] = useState(false);
  const [iconCollapsed, setIconCollapsed] = useState(false);

  const toggleSidebar = () => {
    setCollapsed(prev => !prev);
    // Delay icon flip to match width transition
    setTimeout(() => {
      setIconCollapsed(prev => !prev);
    }, 300);
  };

  return (
    <div
      className={`sidebar flex flex-col bg-white dark:bg-gray-800 border-r border-gray-300 dark:border-gray-600 shadow-md transition-width duration-300 ease-in-out ${
        collapsed ? 'w-16' : 'w-64'
      }`}
    >
      <div className={`flex ${collapsed ? 'flex-col' : 'flex-row'}`}>
        <div onClick={onHome} className="flex grow items-center p-4 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors duration-200">
          <Logo size={24} />
          {!collapsed && <span className="ml-2 text-lg font-semibold text-gray-800 dark:text-gray-100">DemoRAG</span>}
        </div>
        {/* toggle button aligned right */}
        <div className="flex justify-end hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors duration-200">
          <button
            onClick={toggleSidebar}
            className="p-2 m-2 text-gray-600 dark:text-gray-300 hover:text-indigo-600 dark:hover:text-indigo-400 focus:outline-none rounded cursor-pointer transition-colors duration-200"
          >
            {iconCollapsed ? <FiChevronRight size={20} /> : <FiChevronLeft size={20} />}
          </button>
        </div>
      </div>

      {/* chat list */}
      <ul className="flex-1 overflow-y-auto">
        {chats.map(chat => (
          <li
            key={chat.id}
            onClick={() => onSelect(chat.id)}
            className="group flex items-center p-2 m-1 rounded hover:bg-indigo-100 dark:hover:bg-indigo-900 cursor-pointer transition-colors duration-200"
          >
            {!collapsed ? (
              <div className="flex flex-col">
                <span className="truncate text-gray-800 dark:text-gray-100">
                  {chat.title}
                </span>
                <div className="overflow-hidden">
                  <span className="block whitespace-nowrap text-sm text-gray-600 dark:text-gray-300 animate-marquee">
                    {chat.lastMessagePreview}
                  </span>
                </div>
              </div>
            ) : (
              <span className="block whitespace-nowrap text-gray-800 dark:text-gray-100 font-semibold animate-marquee-fast">
                {chat.title}
              </span>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}