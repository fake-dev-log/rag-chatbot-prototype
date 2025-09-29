import { Outlet } from 'react-router-dom';
import Sidebar from '@components/Sidebar';

export default function MainLayout() {
  return (
    <div className="flex h-screen bg-background-light dark:bg-background-dark">
      <Sidebar />
      <main className="flex-1 relative overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}
