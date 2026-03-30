---
name: react-vite-supabase-starter
description: >
  React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase
  のスタックで新規 Web アプリをスキャフォールドするスキル。
  pnpm workspace のモノレポ構成で、認証・ルーティング・データアクセス層・レイアウト・テスト環境を一式セットアップする。
  Use when requested: "React + Supabase でプロジェクトを始めたい", "Vite + React の新規プロジェクト",
  "shadcn + Tailwind v4 のセットアップ", "TanStack Router + Query の初期構成",
  "Supabase 認証付き SPA テンプレート", "react-vite-supabase-starter".
---

# react-vite-supabase-starter

React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + Supabase の技術スタックで新規 Web アプリを立ち上げる。

## Usage

以下を確認してから開始する:

1. **プロジェクト名** — kebab-case (例: `my-app`)
2. **アプリ名** — UI に表示する名前 (例: 「マイアプリ」)
3. **テーマカラー** — プライマリカラーの hex 値。指定がなければデフォルト (#8F5A3C) を使用
4. **Supabase の利用有無** — 不要なら認証・Supabase 関連コードをスキップ

## Step 1: プロジェクト作成

```bash
mkdir <project-name> && cd <project-name>
pnpm init
```

ルートの `package.json` を作成:

```json
{
  "name": "<project-name>",
  "private": true,
  "scripts": {
    "dev": "pnpm --filter web dev",
    "build": "pnpm --filter web build",
    "test": "pnpm --filter web test",
    "lint": "pnpm --filter web lint"
  }
}
```

`pnpm-workspace.yaml` を作成:

```yaml
packages:
  - "apps/*"
  - "packages/*"
```

## Step 2: Vite + React アプリの作成

```bash
mkdir -p apps/web && cd apps/web
pnpm create vite . --template react-ts
```

example/config/ 内のファイルを参照し、以下を設定:

1. **vite.config.ts** — `@` パスエイリアス + Tailwind CSS vite プラグイン
2. **tsconfig.json** / **tsconfig.app.json** / **tsconfig.node.json** — strict + パスエイリアス
3. **eslint.config.js** — flat config + React Hooks + React Refresh
4. **vitest.config.ts** — jsdom + globals + setup file
5. **playwright.config.ts** — E2E テスト設定

## Step 3: 依存パッケージのインストール

```bash
# Core
pnpm add react react-dom

# Routing & State
pnpm add @tanstack/react-router @tanstack/react-query

# Supabase (省略可)
pnpm add @supabase/supabase-js

# UI
pnpm add tailwindcss@latest @tailwindcss/vite shadcn radix-ui
pnpm add class-variance-authority clsx tailwind-merge tw-animate-css
pnpm add lucide-react sonner next-themes
pnpm add @fontsource-variable/inter

# Dev
pnpm add -D @vitejs/plugin-react typescript @types/react @types/react-dom @types/node
pnpm add -D @eslint/js eslint typescript-eslint eslint-plugin-react-hooks eslint-plugin-react-refresh globals
pnpm add -D vitest jsdom @testing-library/jest-dom @testing-library/react @testing-library/user-event
pnpm add -D @playwright/test
```

## Step 4: shadcn/ui の初期化

example/config/components.json を参照して `components.json` を配置する。

```bash
PATH="/tmp/pnpm-shim:$PATH" npx shadcn@latest init
```

必要な UI コンポーネントを追加:

```bash
PATH="/tmp/pnpm-shim:$PATH" npx shadcn@latest add button card input dialog table badge select label sonner
```

## Step 5: テーマ・グローバルスタイルの設定

example/src/index.css を参照して `src/index.css` を作成する。
ユーザーが指定したテーマカラーで `--primary` を差し替える。

## Step 6: コアインフラの配置

example/src/ 以下のファイルを参照し、以下を作成する。
ユーザーのプロジェクト要件に合わせて調整すること。

### 6.1 ユーティリティ (`src/lib/`)

- **utils.ts** — `cn()` (clsx + tailwind-merge)
- **toast.ts** — `showSuccess()` / `showError()` ラッパー
- **logger.ts** — 構造化ロガー
- **error-messages.ts** — エラーコード→ユーザー向けメッセージ変換
- **setup-error-handlers.ts** — グローバルエラーハンドラ
- **query-client.ts** — TanStack Query の QueryClient 設定
- **use-mutate.ts** — 汎用 Mutation Hook (トースト + キャッシュ無効化)

### 6.2 Supabase クライアント (`src/lib/`) ※Supabase 利用時のみ

- **supabase.ts** — Supabase クライアント初期化
- **api.ts** — ApiError + handleSupabaseResult

### 6.3 認証 (`src/auth/`) ※Supabase 利用時のみ

- **auth-context.tsx** — 認証コンテキスト (セッション復元 + 状態監視)

### 6.4 データアクセス層 (`src/data/`) ※Supabase 利用時のみ

- **data/auth/use-login.ts** — ログイン Hook のサンプル
- **data/auth/use-user-profile.ts** — プロフィール取得 Hook のサンプル

`src/data/<domain>/` ディレクトリパターンで、画面からの直接 Supabase アクセスを禁止する。
references/data-layer-pattern.md を参照すること。

### 6.5 ルーティング (`src/router.tsx`)

TanStack Router で型安全なルーティングを構成する。example/src/router.tsx を参照。
以下を含む:

- ルーターコンテキスト (user, queryClient 等)
- 認証ガード (`beforeLoad`)
- レイアウトルート

### 6.6 エントリポイント

- **main.tsx** — グローバルエラーハンドラ登録 + React DOM レンダリング
- **App.tsx** — プロバイダ構成 (ErrorBoundary → QueryClient → Auth → Router)

## Step 7: レイアウト・共通コンポーネント

example/src/components/ を参照して以下を作成:

- **layout/app-layout.tsx** — ヘッダー + メインコンテンツ + Toaster
- **layout/header.tsx** — アプリ名 + ナビゲーション + ログアウト
- **layout/nav-link.tsx** — アクティブ状態検知付きナビリンク
- **error-boundary.tsx** — React Error Boundary
- **page-title.tsx** — ページ見出し (h1)
- **loading-spinner.tsx** — ローディングスピナー
- **mutate-button.tsx** — ローディング付きアクションボタン

## Step 8: サンプルページ

最低限のページを作成して動作確認する:

- `/login` — ログインページ
- `/` — ホーム (認証必須)
- 500 / 404 エラーページ

## Step 9: テスト環境

1. `src/test/setup.ts` — `@testing-library/jest-dom/vitest` のインポート
2. サンプルテストファイルを1つ作成して `pnpm test` で動作確認
3. `pnpm build` でビルドが通ることを確認

## Step 10: 環境変数

`.env.local` (git 管理外) を作成:

```env
VITE_SUPABASE_URL=http://localhost:54321
VITE_SUPABASE_ANON_KEY=<your-anon-key>
```

`src/vite-env.d.ts` で型定義:

```typescript
interface ImportMetaEnv {
  readonly VITE_SUPABASE_URL: string;
  readonly VITE_SUPABASE_ANON_KEY: string;
}
```

## ディレクトリ構成 (完成形)

```
<project-name>/
├── package.json
├── pnpm-workspace.yaml
└── apps/web/
    ├── package.json
    ├── vite.config.ts
    ├── vitest.config.ts
    ├── tsconfig.json / tsconfig.app.json / tsconfig.node.json
    ├── eslint.config.js
    ├── components.json
    ├── playwright.config.ts
    ├── index.html
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── router.tsx
        ├── index.css
        ├── vite-env.d.ts
        ├── auth/
        │   └── auth-context.tsx
        ├── pages/
        │   ├── login.tsx
        │   └── ...
        ├── components/
        │   ├── layout/ (app-layout, header, nav-link)
        │   ├── ui/    (shadcn/ui)
        │   ├── error-boundary.tsx
        │   ├── page-title.tsx
        │   ├── loading-spinner.tsx
        │   └── mutate-button.tsx
        ├── data/
        │   └── <domain>/ (use-*.ts hooks)
        ├── lib/
        │   ├── supabase.ts
        │   ├── api.ts
        │   ├── query-client.ts
        │   ├── utils.ts
        │   ├── use-mutate.ts
        │   ├── toast.ts
        │   ├── logger.ts
        │   ├── error-messages.ts
        │   └── setup-error-handlers.ts
        ├── hooks/
        │   └── use-debounce.ts
        └── test/
            └── setup.ts
```
