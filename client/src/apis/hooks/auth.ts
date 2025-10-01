import {API_PATHS} from "@apis/types/common.ts";
import {useNavigate} from "react-router-dom";
import {useMutation} from "@tanstack/react-query";
import api from "src/apis";
import type {AuthRequest, AuthResponse} from "@apis/types/auth.ts";
import useAuthStore from "@stores/auth.ts";

const useSignIn = () => {
  const navigate = useNavigate();
  return useMutation<AuthResponse, Error, AuthRequest>({
    mutationFn: async (params) => {
      const response = await api.post<AuthResponse>(API_PATHS.auth.signIn, params);
      return response.data;
    },
    onSuccess: async (data: AuthResponse) => {
      useAuthStore.getState().setAuth(data);
      navigate("/");
    }
  });
}

const useSignOut = () => {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: async () => {
      await api.post<void>(API_PATHS.auth.signOut);
    },
    onSuccess: () => {
      useAuthStore.getState().signOut();
      navigate("/sign-in");
    }
  });
};

export {
  useSignIn,
  useSignOut,
}