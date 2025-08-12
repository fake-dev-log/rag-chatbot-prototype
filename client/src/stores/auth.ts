import {create} from "zustand"
import { persist } from 'zustand/middleware'

interface AuthState {
  accessToken: string | null;
  setAccessToken: (token: string) => void;
  signOut: () => void;
}

const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      setAccessToken: (token) => set({ accessToken:token }),
      signOut: () => {
        set({accessToken: null})
        localStorage.clear();
      },
    }),
    {
      name: 'auth-storage'
    }
  ));

export default useAuthStore;