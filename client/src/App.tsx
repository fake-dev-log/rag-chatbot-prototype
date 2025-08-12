import './App.css'
import {QueryClient, QueryCache, QueryClientProvider} from "@tanstack/react-query";
import RouteProvider from "@routes/RouteProvider.tsx";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
    mutations: {
      onError: (err: unknown) => {
        const message = err instanceof Error ? err.message : '알 수 없는 에러'
        alert(`[Error] ${message}`)
      },
      retry: false,
    },
  },
  queryCache: new QueryCache({
    // 쿼리에서 에러가 날 때마다 호출됩니다
    onError: (err: unknown) => {
      const message = err instanceof Error ? err.message : '알 수 없는 에러'
      alert(`[Error] ${message}`)
    },
  }),
});

function App() {

  return (
    <QueryClientProvider client={queryClient}>
      <RouteProvider />
    </QueryClientProvider>
  )
}

export default App
