import { useState } from 'react';
import { FiChevronLeft, FiChevronRight, FiHome, FiUsers, FiLogOut, FiFileText } from 'react-icons/fi';
import { NavLink } from 'react-router-dom';
import Logo from '@components/Logo.tsx';
import {APP_NAME} from "@constants";
import useAuthStore from "@stores/auth.ts";
import { useSignOut } from "@apis/hooks/auth.ts";

function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const [iconCollapsed, setIconCollapsed] = useState(false);
  const role = useAuthStore((state) => state.role);
  const { mutate: signOut } = useSignOut();

  const toggleSidebar = () => {
    setCollapsed(prev => !prev);
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
      <div className="flex items-center justify-between p-4 border-b border-gray-300 dark:border-gray-600">
        <NavLink to="/" className="flex items-center cursor-pointer">
          <Logo size={24} />
          {!collapsed && <span className="ml-2 text-lg font-semibold text-gray-800 dark:text-gray-100">{APP_NAME}</span>}
        </NavLink>
        <button
          onClick={toggleSidebar}
          className="transform text-gray-600 dark:text-gray-300 hover:text-indigo-600 dark:hover:text-indigo-400"
        >
          {iconCollapsed ? <FiChevronRight size={20} /> : <FiChevronLeft size={20} />}
        </button>
      </div>

      <nav className="flex-1 px-2 py-4 space-y-2">
        <NavLink
          to="/"
          className={({ isActive }) =>
            isActive
              ? "flex items-center px-4 py-2 text-gray-700 bg-gray-200 rounded-md dark:bg-gray-700 dark:text-gray-200"
              : "flex items-center px-4 py-2 text-gray-600 transition-colors duration-300 transform rounded-md dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 dark:hover:text-gray-200 hover:text-gray-700"
          }
        >
          <FiHome />
          {!collapsed && <span className="mx-4 font-medium">Home</span>}
        </NavLink>
        { role === "ADMIN" &&
        <>
          <NavLink
            to="/admin/documents"
            className={({ isActive }) =>
              isActive
                ? "flex items-center px-4 py-2 text-gray-700 bg-gray-200 rounded-md dark:bg-gray-700 dark:text-gray-200"
                : "flex items-center px-4 py-2 mt-3 text-gray-600 transition-colors duration-300 transform rounded-md dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 dark:hover:text-gray-200 hover:text-gray-700"
            }
          >
            <FiUsers />
            {!collapsed && <span className="mx-4 font-medium">Documents</span>}
          </NavLink>
          <NavLink
            to="/admin/prompts"
            className={({ isActive }) =>
              isActive
                ? "flex items-center px-4 py-2 text-gray-700 bg-gray-200 rounded-md dark:bg-gray-700 dark:text-gray-200"
                : "flex items-center px-4 py-2 mt-3 text-gray-600 transition-colors duration-300 transform rounded-md dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 dark:hover:text-gray-200 hover:text-gray-700"
            }
          >
            <FiFileText />
            {!collapsed && <span className="mx-4 font-medium">Prompts</span>}
          </NavLink>
        </>
        }
      </nav>

      <div className="px-2 py-4">
        <button
          onClick={() => signOut()}
          className="flex items-center px-4 py-2 w-full text-gray-600 transition-colors duration-300 transform rounded-md dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 dark:hover:text-gray-200 hover:text-gray-700"
        >
          <FiLogOut />
          {!collapsed && <span className="mx-4 font-medium">Sign Out</span>}
        </button>
      </div>
    </div>
  );
}

export default Sidebar;
