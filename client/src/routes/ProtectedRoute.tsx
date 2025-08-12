import {Navigate, Outlet} from "react-router-dom";

export default function ProtectedRoute({isAuthenticated}: {isAuthenticated: boolean}) {
  return isAuthenticated ? <Outlet /> :<Navigate replace to={'/sign-in'}/>;
}