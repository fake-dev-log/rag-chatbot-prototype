import {Navigate, Outlet} from "react-router-dom";
import useAuthStore from "@stores/auth.ts";

export default function AdminRoute({isAuthenticated}: {isAuthenticated: boolean}) {
  const {role} = useAuthStore();
  const isAdmin = role === 'ADMIN';

  if (!isAuthenticated) {
    return <Navigate replace to={'/sign-in'}/>;
  }

  return isAdmin ? <Outlet /> : <Navigate replace to={'/no-such-content'} />;
}