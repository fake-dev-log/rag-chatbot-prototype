import {API_BASE_URL} from "@apis/types/common.ts";
import {useNavigate} from "react-router-dom";
import {useMutation} from "@tanstack/react-query";
import api from "src/apis";
import type {LoginRequest, LoginResponse} from "@apis/types/auth.ts";
import useAuthStore from "@stores/auth.ts";

const baseUri = API_BASE_URL.auth

const useLogin = () => {
  const navigate = useNavigate();
  return useMutation<LoginResponse, Error, LoginRequest>({
    mutationFn: async (params) => {
      const response = await api.post<LoginResponse>(`${baseUri}/sign-in`, params);
      return response.data;
    },
    onSuccess: async (data: LoginResponse) => {
      useAuthStore.setState({ accessToken: data.accessToken });
      navigate("/");
    }
  });
}

const useLogout = () => {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: async () => {
      await api.post<void>(`${baseUri}/sign-out`);
    },
    onSuccess: () => {
      useAuthStore.getState().signOut();
      navigate("/sign-in");
    }
  });
};

export {
  useLogin,
  useLogout,
}