# react-vite-supabase-starter

React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase のスタックで新規 Web アプリをスキャフォールドする。

## インストール

```sh
gh skill install tbsten/skills react-vite-supabase-starter
```

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

セットアップの SSoT は `scripts/scaffold.sh`。自己完結した `example/` (そのままコンパイルが通る)
の全ファイル配置、プロジェクト名・アプリ表示名・プライマリカラーの置換、`pnpm install`、
shadcn/ui コンポーネント生成、`.env.local` 雛形作成までを 1 回の実行で決定的に行う。
AI は手順を再実装せず、script をそのまま実行する。

```sh
scripts/scaffold.sh --name <project> --app-name "<アプリ名>" --primary "#RRGGBB"
# + --dest <dir> / --no-supabase / --skip-install / --dry-run / --force
```

## 生成されるファイル構成

```
<project>/
├── package.json              # ワークスペースルート
├── pnpm-workspace.yaml
├── .gitignore
└── apps/web/
    ├── 設定ファイル群         # vite, vitest, tsconfig, eslint, shadcn, playwright
    ├── index.html
    ├── .env.local(.example)
    └── src/
        ├── main.tsx          # エントリポイント (グローバルエラーハンドラ)
        ├── App.tsx           # プロバイダ構成 (ErrorBoundary > Query > Auth > Router)
        ├── router.tsx        # 型安全ルーティング + 認証ガード + 404/500
        ├── index.css         # Tailwind テーマ (カスタマイズ可能)
        ├── auth/             # Supabase 認証コンテキスト
        ├── pages/            # login / home / not-found / error
        ├── components/       # レイアウト・共通コンポーネント・shadcn/ui
        ├── data/             # データアクセス Hook (Supabase 隠蔽)
        ├── lib/              # ユーティリティ (query-client, logger, toast 等)
        └── hooks/            # 汎用 Hook (useDebounce 等)
```

スキャフォールド直後から `pnpm test` / `pnpm build` / `pnpm lint` が全て通る。

## 主要パターン

### データアクセス層

画面コンポーネントから Supabase を直接 import しない。全データ操作は `src/data/<domain>/` の Hook 経由 (サンプル: `use-login.ts`, `use-user-profile.ts`)。詳細は `references/data-layer-pattern.md` を参照。

### 認証フロー

1. `AuthProvider` がマウント時にセッション復元
2. ルーターの `beforeLoad` で未認証ユーザーを `/login` へリダイレクト
3. 認証済みルートは `AppLayout` 内でレンダリング

### テーマ

`index.css` の CSS 変数でカラースキーム全体を制御。`scaffold.sh --primary` が `--primary` と `--ring` を設定する。`--accent` などはブランドに合わせて変更する。

## 前提条件

- Node.js 20+
- pnpm 9+
- ネットワークアクセス (`pnpm install` + shadcn/ui 生成に必要。配置だけなら `--skip-install`)
- Supabase プロジェクト (認証・DB を使う場合)

## 使い方

```
/react-vite-supabase-starter
```

プロジェクト名・アプリ表示名・テーマカラー・Supabase の利用有無を確認した後、`scripts/scaffold.sh` を実行し、`pnpm test` / `pnpm build` で動作確認する。Supabase を使わない場合は script が ACTION_REQUIRED を出力し、AI が認証関連コードを除去する。
