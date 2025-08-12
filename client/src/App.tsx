import './App.css'
import {QueryClient, QueryCache, QueryClientProvider} from "@tanstack/react-query";
import RouteProvider from "@routes/RouteProvider.tsx";
import { ToastProvider } from "@components/ToastProvider.tsx";
import { useToastStore } from "@stores/toast.ts"; 

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
    mutations: {
      onError: (err: unknown) => {
        const message = err instanceof Error ? err.message : '알 수 없는 에러'
        // Use the toast system to show error
        useToastStore.getState().addToast(`[Error] ${message}`, 'error');
      },
      retry: false,
    },
  },
  queryCache: new QueryCache({
    // 쿼리에서 에러가 날 때마다 호출됩니다
    onError: (err: unknown) => {
      const message = err instanceof Error ? err.message : '알 수 없는 에러'
      // Use the toast system to show error
      useToastStore.getState().addToast(`[Error] ${message}`, 'error');
    },
  }),
});

function App() {

  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <RouteProvider />
      </ToastProvider>
    </QueryClientProvider>
  )
}

export default App
