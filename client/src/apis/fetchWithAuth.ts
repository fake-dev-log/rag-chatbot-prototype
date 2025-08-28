import useAuthStore from "@stores/auth";
import { baseURL } from "./types/common";

let refreshing: Promise<void> | null = null;

async function doRefresh() {
  // only one in-flight refresh at a time
  if (!refreshing) {
    refreshing = fetch(`${baseURL}/auth/refresh`, {
      method: "POST",
      credentials: "include",
    })
      .then(async (res) => {
        if (!res.ok) {
          const errorResponse = await res.json();
          throw new Error(errorResponse.message || "리프레시 토큰 만료");
        }
        const { data } = await res.json();
        useAuthStore.getState().setAuth(data);
      })
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

export async function fetchWithAuth(
  input: RequestInfo,
  init: RequestInit = {},
  retry = true
): Promise<Response> {
  const store = useAuthStore.getState();
  const token = store.accessToken;

  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let res = await fetch(input, {
    ...init,
    headers,
    credentials: "include",
  });

  if (res.status === 401 && retry) {
    try {
      await doRefresh();
      // retry original
      const newToken = useAuthStore.getState().accessToken;
      if (newToken) headers.set("Authorization", `Bearer ${newToken}`);
      res = await fetch(input, { ...init, headers, credentials: "include" });
    } catch (e) {
      // refresh failed → sign out
      const errorMessage = (e instanceof Error) ? e.message : "로그인이 만료되었습니다. 다시 로그인해주세요.";
      store.signOut();
      window.location.href = "/sign-in";
      throw new Error(errorMessage);
    }
  }

  return res;
}
