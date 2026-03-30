# react-vite-supabase-starter

React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase のスタックで新規 Web アプリをスキャフォールドする。

## 概要

以下の技術スタックで SPA を構築するための完全なプロジェクト雛形を提供する:

- **React 19** + TypeScript (strict モード)
- **Vite** (ビルドツール、`@` パスエイリアス)
- **Tailwind CSS v4** (CSS 変数によるテーマ)
- **shadcn/ui** (Radix UI ベースのアクセシブルコンポーネント)
- **TanStack Router** (型安全ルーティング + 認証ガード)
- **TanStack Query** (サーバー状態管理)
- **Supabase** (認証・データベース、省略可)
- **Vitest** + Testing Library (単体テスト)
- **Playwright** (E2E テスト)
- **pnpm workspace** (モノレポ構成)

## 生成されるファイル構成

```
<project>/
├── package.json              # ワークスペースルート
├── pnpm-workspace.yaml
└── apps/web/
    ├── 設定ファイル群         # vite, tsconfig, eslint, shadcn, playwright
    └── src/
        ├── main.tsx          # エントリポイント (グローバルエラーハンドラ)
        ├── App.tsx           # プロバイダ構成 (ErrorBoundary > Query > Auth > Router)
        ├── router.tsx        # 型安全ルーティング + 認証ガード
        ├── index.css         # Tailwind テーマ (カスタマイズ可能)
        ├── auth/             # Supabase 認証コンテキスト
        ├── components/       # レイアウト・共通コンポーネント・shadcn/ui
        ├── data/             # データアクセス Hook (Supabase 隠蔽)
        ├── lib/              # ユーティリティ (query-client, logger, toast 等)
        └── hooks/            # 汎用 Hook (useDebounce 等)
```

## 主要パターン

### データアクセス層

画面コンポーネントから Supabase を直接 import しない。全データ操作は `src/data/<domain>/` の Hook 経由。詳細は `references/data-layer-pattern.md` を参照。

### 認証フロー

1. `AuthProvider` がマウント時にセッション復元
2. ルーターの `beforeLoad` で未認証ユーザーを `/login` へリダイレクト
3. 認証済みルートは `AppLayout` 内でレンダリング

### テーマ

`index.css` の CSS 変数でカラースキーム全体を制御。`--primary`, `--accent`, `--ring` をブランドカラーに変更する。

## 前提条件

- Node.js 20+
- pnpm 9+
- Supabase プロジェクト (認証・DB を使う場合)

## 使い方

```
/react-vite-supabase-starter
```

プロジェクト名、アプリ表示名、テーマカラーを聞かれた後、プロジェクト一式が生成される。
