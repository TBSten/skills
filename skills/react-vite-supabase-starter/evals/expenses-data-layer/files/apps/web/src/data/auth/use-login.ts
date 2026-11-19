import { getErrorMessage } from "@/lib/error-messages";
import { supabase } from "@/lib/supabase";
import { useMutate } from "@/lib/use-mutate";

interface LoginParams {
  email: string;
  password: string;
}

// ログイン Mutation Hook のサンプル。
// 画面コンポーネントは supabase を直接 import せず、この hook を経由する
// (references/data-layer-pattern.md の Mutation Hook パターン)。
export function useLogin(options?: { onSuccess?: () => void }) {
  return useMutate({
    mutationFn: async ({ email, password }: LoginParams) => {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      });
      if (error) {
        throw new Error(getErrorMessage(error.code, error.message));
      }
      return data;
    },
    onSuccess: options?.onSuccess,
  });
}
