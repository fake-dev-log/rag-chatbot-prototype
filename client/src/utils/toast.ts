import { useToastStore } from "@stores/toast";

export const toast = {
  success: (message: string) => {
    useToastStore.getState().addToast(message, "success");
  },
  error: (message: string) => {
    useToastStore.getState().addToast(message, "error");
  },
  info: (message: string) => {
    useToastStore.getState().addToast(message, "info");
  },
};