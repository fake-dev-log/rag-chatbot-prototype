import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import ProtectedRoute from "./ProtectedRoute.tsx";
import SignIn from "@pages/auth/SignIn.tsx";
import useAuthStore from "@stores/auth.ts";
import ChatRoom from "@pages/chat/ChatRoom.tsx";
import NotFound from "@pages/error/NotFound.tsx";
import Home from "@pages/Home.tsx";
import MainLayout from "@layouts/MainLayout.tsx";
import AdminLayout from "@layouts/AdminLayout.tsx";
import Admin from "@pages/admin/Admin.tsx";
import DocumentList from "@pages/admin/documents/DocumentList.tsx";
import AdminRoute from "@routes/AdminRoute.tsx";
import NoSuchContent from "@pages/error/NoSuchContent.tsx";

export default function RouteProvider() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path={"/sign-in"}
          element={accessToken == null ? <SignIn /> : <Navigate replace to={'/'} />}
        />
        <Route element={<ProtectedRoute isAuthenticated={accessToken != null}/>}>
          <Route element={<MainLayout />}>
            <Route
              path="/"
              element={<Home />}
            />
            <Route
              path="/chats/:chatId"
              element={<ChatRoom />}
            />
          </Route>
          <Route element={<AdminRoute isAuthenticated={accessToken != null}/>}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<Admin />} />
              <Route path="documents" element={<DocumentList />} />
            </Route>
          </Route>
        </Route>
        <Route path="/no-such-content" element={<NoSuchContent />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  )
}
