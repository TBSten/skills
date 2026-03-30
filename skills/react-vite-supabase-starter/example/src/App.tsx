import { QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "@tanstack/react-router";
import { AuthProvider, useAuth } from "@/auth/auth-context";
import { ErrorBoundary } from "@/components/error-boundary";
import { LoadingSpinner } from "@/components/loading-spinner";
import { queryClient } from "@/lib/query-client";
import { router } from "@/router";

function InnerApp() {
  const { user, loading: authLoading } = useAuth();

  if (authLoading) {
    return <LoadingSpinner className="min-h-screen" />;
  }

  return (
    <RouterProvider
      router={router}
      context={{
        user,
        userLoading: false,
        queryClient,
      }}
    />
  );
}

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <InnerApp />
        </AuthProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
