import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import ProtectedRoute from "./ProtectedRoute.tsx";
import SignIn from "@pages/auth/SignIn.tsx";
import useAuthStore from "@stores/auth.ts";
import ChatRoom from "@pages/chat/ChatRoom.tsx";
import NotFound from "@pages/error/NotFound.tsx";
import Home from "@pages/Home.tsx";
import MainLayout from "@layouts/MainLayout.tsx";

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
        </Route>
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  )
}