import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/auth/auth-context";
import { handleSupabaseResult } from "@/lib/api";
import { supabase } from "@/lib/supabase";

// profiles テーブルの行型のサンプル。プロジェクトのスキーマに合わせて書き換える。
export interface UserProfile {
  id: string;
  display_name: string | null;
}

// ログイン中ユーザーのプロフィール取得 Query Hook のサンプル
// (references/data-layer-pattern.md の Query Hook パターン)。
// クエリキーは ["<domain>", "<scope>", ...identifiers] の階層構造にする。
export function useUserProfile() {
  const { user } = useAuth();

  const { data, isLoading } = useQuery({
    queryKey: ["auth", "profile", user?.id],
    queryFn: async () => {
      const result = await supabase
        .from("profiles")
        .select("id, display_name")
        .eq("id", user!.id)
        .maybeSingle();
      const row = await handleSupabaseResult(result);
      // profiles 行が未作成のユーザーは null (画面側でフォールバック表示する)
      return row as UserProfile | null;
    },
    enabled: !!user,
  });

  return { profile: data ?? null, loading: isLoading };
}
