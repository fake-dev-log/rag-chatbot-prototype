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
import PromptList from "@pages/admin/prompts/PromptList.tsx";
import AdminRoute from "@routes/AdminRoute.tsx";
import NoSuchContent from "@pages/error/NoSuchContent.tsx";

/**
 * Defines the routing structure for the entire application.
 * It uses `react-router-dom` to manage navigation and renders different
 * components based on the URL path and user authentication state.
 */
export default function RouteProvider() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return (
    <BrowserRouter>
      <Routes>
        {/* Public Route: Sign-in page */}
        {/* If the user is already logged in, redirect them to the home page. */}
        <Route
          path={"/sign-in"}
          element={accessToken == null ? <SignIn /> : <Navigate replace to={'/'} />}
        />

        {/* Protected Routes: For authenticated users only */}
        <Route element={<ProtectedRoute isAuthenticated={accessToken != null}/>}>
          {/* Routes with the main application layout (e.g., with sidebar) */}
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

          {/* Admin Routes: For users with the 'ADMIN' role */}
          <Route element={<AdminRoute isAuthenticated={accessToken != null}/>}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<Admin />} />
              <Route path="documents" element={<DocumentList />} />
              <Route path="prompts" element={<PromptList />} />
            </Route>
          </Route>
        </Route>

        {/* Special Error Routes */}
        <Route path="/no-such-content" element={<NoSuchContent />} />

        {/* Catch-all Route: Renders the 404 Not Found page for any unhandled paths */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  )
}