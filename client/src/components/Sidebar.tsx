import { useState } from 'react';
import { FiChevronLeft, FiChevronRight, FiPlus, FiUsers, FiLogOut, FiFileText } from 'react-icons/fi';
import { Link, NavLink } from 'react-router-dom';
import Logo from '@components/Logo.tsx';
import { APP_NAME } from '@constants';
import useAuthStore from '@stores/auth.ts';
import { useSignOut } from '@apis/hooks/auth.ts';
import HoverScrollText from './HoverScrollText';
import { useChatList } from '@apis/hooks/chat';

function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const role = useAuthStore((state) => state.role);
  const { mutate: signOut } = useSignOut();
  const { data: chats } = useChatList();

  return (
    <div
      className={`
        bg-gray-100 dark:bg-gray-900 text-gray-800 dark:text-gray-200 
        flex flex-col h-screen p-2
        transition-width duration-300 ease-in-out
        ${collapsed ? 'w-16' : 'w-64'}
      `}
    >
      {/* Header */}
      <div className="flex items-center justify-between pb-2 mb-2 border-b border-gray-300 dark:border-gray-700">
        <Link to="/" className="flex items-center min-w-0">
          <Logo size={24} />
          {!collapsed && <span className="ml-2 text-lg font-semibold truncate">{APP_NAME}</span>}
        </Link>
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="text-gray-600 dark:text-gray-300 hover:text-indigo-600 dark:hover:text-indigo-400"
        >
          {collapsed ? <FiChevronRight size={20} /> : <FiChevronLeft size={20} />}
        </button>
      </div>

      {/* New Chat Button */}
      <Link
        to="/"
        className="
          flex items-center justify-center shrink-0 px-4 py-2 mb-2
          text-gray-600 dark:text-gray-300
          bg-white dark:bg-gray-800 rounded-md 
          hover:bg-gray-200 dark:hover:bg-gray-700
          border border-gray-300 dark:border-gray-600
        "
      >
        <FiPlus />
        {!collapsed && <span className="mx-4 font-medium">New Chat</span>}
      </Link>

      {/* Chat History */}
      <div className="flex-1 overflow-y-auto space-y-1">
        {chats?.map(chat => (
          <Link
            key={chat.id}
            to={`/chats/${chat.id}`}
            className="block p-2 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 group"
          >
            <HoverScrollText
              text={chat.title}
              className="text-sm font-medium text-gray-800 dark:text-gray-100 group-hover:text-gray-900 dark:group-hover:text-white"
            />
            <HoverScrollText
              text={chat.lastMessagePreview}
              className="text-xs text-gray-600 dark:text-gray-400 group-hover:text-gray-700 dark:group-hover:text-gray-200"
            />
          </Link>
        ))}
      </div>

      {/* Footer - Admin & User Controls */}
      <div className="mt-2 pt-2 border-t border-gray-300 dark:border-gray-700">
        {role === 'ADMIN' && (
          <>
            <NavLink
              to="/admin/documents"
              className={({ isActive }) =>
                `flex items-center px-4 py-2 mt-1 text-sm rounded-md transition-colors duration-200 ${
                  isActive
                    ? 'bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-100'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700'
                }`
              }
            >
              <FiUsers />
              {!collapsed && <span className="mx-4 font-medium">Documents</span>}
            </NavLink>
            <NavLink
              to="/admin/prompts"
              className={({ isActive }) =>
                `flex items-center px-4 py-2 mt-1 text-sm rounded-md transition-colors duration-200 ${
                  isActive
                    ? 'bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-100'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700'
                }`
              }
            >
              <FiFileText />
              {!collapsed && <span className="mx-4 font-medium">Prompts</span>}
            </NavLink>
          </>
        )}
        <button
          onClick={() => signOut()}
          className="flex items-center w-full px-4 py-2 mt-1 text-sm text-gray-600 dark:text-gray-400 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700"
        >
          <FiLogOut />
          {!collapsed && <span className="mx-4 font-medium">Sign Out</span>}
        </button>
      </div>
    </div>
  );
}

export default Sidebar;