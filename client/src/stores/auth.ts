import {create} from "zustand"
import { persist } from 'zustand/middleware'
import type {AuthResponse} from "@apis/types/auth.ts";

/**
 * Represents the state and actions for authentication.
 */
interface AuthState {
  /** The JWT access token for the authenticated user. Null if not authenticated. */
  accessToken: string | null;
  /** The role of the authenticated user (e.g., 'USER', 'ADMIN'). Null if not authenticated. */
  role: string | null;
  /**
   * Sets the authentication state upon successful sign-in.
   * @param response The authentication response from the API, containing the access token and role.
   */
  setAuth: (response: AuthResponse) => void;
  /**
   * Clears the authentication state upon logout.
   */
  signOut: () => void;
}

/**
 * A Zustand store for managing global authentication state.
 *
 * This store uses the `persist` middleware to save the authentication state
 * to the browser's localStorage. This ensures that the user remains logged in
 * even after a page refresh.
 */
const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      role: null,
      setAuth: (response) => set({ accessToken: response.accessToken, role: response.role }),
      signOut: () => {
        // Clear the state in the store.
        set({accessToken: null, role: null})
        // Explicitly clear the persisted storage to ensure a clean logout.
        localStorage.clear();
      },
    }),
    {
      // The name of the key in localStorage where the state will be stored.
      name: 'auth-storage'
    }
  ));

export default useAuthStore;
