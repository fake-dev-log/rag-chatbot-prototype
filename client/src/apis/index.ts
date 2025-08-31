import axios, { AxiosError } from "axios";
import useAuthStore from "@stores/auth.ts";
import {baseURL} from "@apis/types/common.ts";
import { useToastStore } from "@stores/toast.ts";

const api = axios.create({
  baseURL: baseURL,
  withCredentials: true,
});

const signInFailMessage = "Your session has expired. Please sign in again.";

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
        throw new Error(error.response.data?.message ?? "Sign-in failed.");
      case 401: {
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
              await handleExpired(errorData.message || signInFailMessage);
            }
          }
        } catch (e) {
          const errorMessage = (e as AxiosError).message || signInFailMessage;
          await handleExpired(errorMessage);
        }
        const finalErrorMessage = error.response?.data?.message || signInFailMessage;
        await handleExpired(finalErrorMessage);
        break;
      }
      case 500:
        throw new Error(error.response.data.message ?? "A server error has occurred.");
      default:
        break;
    }

    return Promise.reject(error);
  }
)

function handleExpired(message: string = signInFailMessage) {
  useAuthStore.getState().signOut();
  useToastStore.getState().addToast(message, 'error'); // Use Zustand store
  window.location.href = "/sign-in";
  return Promise.reject(new Error(message));
}

export default api