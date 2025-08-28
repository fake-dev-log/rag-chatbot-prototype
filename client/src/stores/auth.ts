import {create} from "zustand"
import { persist } from 'zustand/middleware'
import type {AuthResponse} from "@apis/types/auth.ts";

interface AuthState {
  accessToken: string | null;
  role: string | null;
  setAuth: (response: AuthResponse) => void;
  signOut: () => void;
}

const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      role: null,
      setAuth: (response) => set({ accessToken: response.accessToken, role: response.role }),
      signOut: () => {
        set({accessToken: null, role: null})
        localStorage.clear();
      },
    }),
    {
      name: 'auth-storage'
    }
  ));

export default useAuthStore;