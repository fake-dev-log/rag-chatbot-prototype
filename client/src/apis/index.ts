import axios from "axios";
import useAuthStore from "@stores/auth.ts";
import {baseURL} from "@apis/types/common.ts";
import { useToastStore } from "@stores/toast.ts"; // Import Zustand store

const api = axios.create({
  baseURL: baseURL,
  withCredentials: true,
})

api.interceptors.request.use(
  config => {
    const token = useAuthStore.getState().accessToken;
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  error => Promise.reject(error)
)

api.interceptors.response.use(
  res => res,
  async error => {
    const originalRequest = error.config;

    switch (error.response.status) {
      case 400:
        throw new Error(error.response.data?.message ?? "로그인에 실패했습니다.");
      case 401:
        if (originalRequest.url.includes("/auth/refresh")) {
          return Promise.reject(error);
        }

        try {
          if (!originalRequest._retry) {
            originalRequest._retry = true
            const response = await api.post("/auth/refresh");
            if (response.status === 200) {
              const {accessToken} = response.data.data;
              useAuthStore.setState({ accessToken: accessToken });
              return api(originalRequest);
            } else {
              const errorData = response.data;
              await handleExpired(errorData.message || "로그인이 만료되었습니다. 다시 로그인해주세요.");
            }
          }
        } catch (e) {
          const errorMessage = (e as any)?.response?.data?.message || "로그인이 만료되었습니다. 다시 로그인해주세요.";                          
          await handleExpired(errorMessage);
        }
        const finalErrorMessage = error.response?.data?.message || "로그인이 만료되었습니다. 다시 로그인해주세요."
        await handleExpired(finalErrorMessage);
        break;
      case 500:
        throw new Error(error.response.data.message ?? "서버 에러가 발생했습니다.");
      default:
        break;
    }

    return Promise.reject(error);
  }
)

function handleExpired(message: string = "로그인이 만료되었습니다. 다시 로그인해주세요.") {
  useAuthStore.getState().signOut();
  useToastStore.getState().addToast(message, 'error'); // Use Zustand store
  window.location.href = "/sign-in";
  return Promise.reject(new Error(message));
}

export default api