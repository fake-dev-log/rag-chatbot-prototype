import { Outlet, useNavigate } from 'react-router-dom';
import Sidebar from '@components/Sidebar';
import { useChatList } from '@apis/hooks/chat';

export default function MainLayout() {
  const { data: chats } = useChatList();
  const navigate = useNavigate();

  return (
    <div className="flex h-screen">
      <Sidebar
        chats={chats ?? []}
        onSelect={id => navigate(`/chats/${id}`)}
        onHome={() => navigate('/')}
      />
      <div className="flex-1 bg-background-light dark:bg-background-dark">
        <Outlet />
      </div>
    </div>
  );
}