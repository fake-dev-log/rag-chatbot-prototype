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
        const message = err instanceof Error ? err.message : 'Unknown error'
        // Use the toast system to show error
        useToastStore.getState().addToast(`[Error] ${message}`, 'error');
      },
      retry: false,
    },
  },
  queryCache: new QueryCache({
    // This will be called by every query error
    onError: (err: unknown) => {
      const message = err instanceof Error ? err.message : 'Unknown error'
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
