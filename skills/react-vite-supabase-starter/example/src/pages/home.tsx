import { useAuth } from "@/auth/auth-context";
import { LoadingSpinner } from "@/components/loading-spinner";
import { PageTitle } from "@/components/page-title";
import { useUserProfile } from "@/data/auth/use-user-profile";

// 認証必須のホームページのサンプル。
// データ取得は src/data/ の hook 経由 (supabase を直接 import しない)。
export function HomePage() {
  const { user } = useAuth();
  const { profile, loading } = useUserProfile();

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4">
      <PageTitle>Home</PageTitle>
      {loading ? (
        <LoadingSpinner className="py-8" />
      ) : (
        <p className="text-sm text-muted-foreground">
          Signed in as {profile?.display_name ?? user?.email}
        </p>
      )}
    </div>
  );
}
