/// <reference types="vite/client" />

// Supabase を使わない構成では VITE_SUPABASE_* の 2 行を削除する
interface ImportMetaEnv {
  readonly VITE_SUPABASE_URL: string;
  readonly VITE_SUPABASE_ANON_KEY: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
