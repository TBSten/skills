---
name: react-vite-supabase-starter
description: >
  React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase
  のスタックで新規 Web アプリをスキャフォールドするスキル。
  pnpm workspace のモノレポ構成で、認証・ルーティング・データアクセス層・レイアウト・テスト環境を一式セットアップする。
  Use when requested: "React + Supabase でプロジェクトを始めたい", "Vite + React の新規プロジェクト",
  "shadcn + Tailwind v4 のセットアップ", "TanStack Router + Query の初期構成",
  "Supabase 認証付き SPA テンプレート", "react-vite-supabase-starter".
metadata:
  status: Experimental
  group: Web フロントエンド
---

# react-vite-supabase-starter

React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase の技術スタックで新規 Web アプリを立ち上げる。

セットアップ手順の SSoT は `scripts/scaffold.sh`。
**script を読解・書き換え・再実装せず、そのまま実行すること。**
example/ は「コピーすればコンパイルが通る」自己完結の雛形で、script がその配置・置換を一括で行う。

## Step 1: 確認事項ヒアリング

以下を確認してから開始する:

1. **プロジェクト名** — kebab-case (例: `my-app`)
2. **アプリ名** — UI に表示する名前 (例: 「マイアプリ」)
3. **テーマカラー** — プライマリカラーの hex 値。指定がなければデフォルト (`#8F5A3C`) を使用
4. **Supabase の利用有無** — 不要なら後述「Supabase を使わない場合」の除去を行う

## Step 2: scaffold.sh 実行

```bash
bash <このスキルのディレクトリ>/scripts/scaffold.sh \
  --name <project-name> \
  --app-name "<アプリ名>" \
  --primary "#RRGGBB"
```

- 生成先はカレントの `./<project-name>` (変更するときだけ `--dest <dir>`)
- **ネットワーク必須** (pnpm install + shadcn/ui コンポーネント生成)。オフライン時は `--skip-install` で
  ファイル配置だけ行い、出力される ACTION_REQUIRED のコマンドを後で実行する
- 既存ファイルは上書きしない (冪等)。上書きしたいときだけ `--force`
- `--dry-run` で配置予定ファイルと実行予定ステップを事前確認できる
- 失敗時は `ERROR / why / fix` が stderr に出て非 0 終了する。fix の指示に従って復旧する
  (どの Step で止まったかはログの `== Step N:` 見出しで分かる)
- 正常終了時は末尾に 1 行 JSON (`{"ok":true,...}`) が出る

script が行うこと (Step 1〜6): example/ 全ファイルの配置、`<project-name>` / `<app-name>` /
`--primary` (index.css の `--primary` と `--ring`) の置換、`pnpm install`、
`pnpm dlx shadcn@latest add button card input dialog table badge select label sonner`
(components.json 配置済みのため `shadcn init` は不要)、`.env.local` 雛形生成。

## Step 3: 動作確認

```bash
cd <project-name>
pnpm test    # vitest (サンプルテスト付き)
pnpm build   # tsc -b + vite build
```

両方 green になることを確認してから次へ進む。`pnpm lint` も通る状態で生成される。

## Step 4: 要件に合わせた調整

- **環境変数**: `apps/web/.env.local` の `VITE_SUPABASE_URL` / `VITE_SUPABASE_ANON_KEY` を実値に書き換える
- **データアクセス層**: 画面コンポーネントから supabase を直接 import しない。
  `src/data/<domain>/` の hook 経由にする (references/data-layer-pattern.md を参照)。
  サンプル: `data/auth/use-login.ts` (Mutation) / `data/auth/use-user-profile.ts` (Query)
- **テーマ**: `src/index.css` の CSS 変数 (`--primary` / `--accent` / `--ring` など) で全体を制御
- **ページ追加**: `src/pages/` にコンポーネントを作り、`src/router.tsx` にルートを追加
- **UI コンポーネント追加**: `cd apps/web && pnpm dlx shadcn@latest add <component>`
- **アプリ名の変更**: `src/components/layout/header.tsx` の `APP_NAME` と `index.html` の `<title>`

### Supabase を使わない場合

`--no-supabase` を付けても配置は Supabase あり構成のまま行われ、除去手順が
ACTION_REQUIRED として出力される (script の分岐を単純に保つための v1 仕様)。
以下を AI が実施する:

1. 削除: `src/lib/supabase.ts`, `src/lib/api.ts`, `src/auth/`, `src/data/`,
   `src/pages/login.tsx`, `apps/web/.env.local.example`
2. 編集:
   - `App.tsx` — AuthProvider / useAuth / InnerApp の認証状態を除去し RouterProvider を直接描画
   - `router.tsx` — 認証ガード (`beforeLoad`) と login ルートを除去
   - `components/layout/header.tsx` — useAuth / Sign out ボタンを除去
   - `pages/home.tsx` — useAuth / useUserProfile 依存を除去
   - `vite-env.d.ts` — `VITE_SUPABASE_*` の型定義を除去
3. `cd apps/web && pnpm remove @supabase/supabase-js`
4. `pnpm test && pnpm build` で green を確認

## ディレクトリ構成 (完成形)

```
<project-name>/
├── package.json              # workspace root (pnpm --filter web ...)
├── pnpm-workspace.yaml
├── .gitignore
└── apps/web/
    ├── package.json
    ├── index.html
    ├── components.json       # shadcn/ui 設定
    ├── vite.config.ts / vitest.config.ts / playwright.config.ts
    ├── tsconfig.json / tsconfig.app.json / tsconfig.node.json
    ├── eslint.config.js
    ├── .env.local.example / .env.local
    └── src/
        ├── main.tsx          # エントリポイント (グローバルエラーハンドラ登録)
        ├── App.tsx           # プロバイダ構成 (ErrorBoundary → Query → Auth → Router)
        ├── router.tsx        # 型安全ルーティング + 認証ガード + 404/500
        ├── index.css         # Tailwind v4 テーマ (CSS 変数)
        ├── vite-env.d.ts
        ├── auth/
        │   └── auth-context.tsx
        ├── pages/
        │   ├── login.tsx / home.tsx / not-found.tsx / error.tsx
        ├── components/
        │   ├── layout/ (app-layout, header, nav-link)
        │   ├── ui/    (shadcn/ui — scaffold.sh Step 5 で生成)
        │   ├── error-boundary.tsx / page-title.tsx
        │   └── loading-spinner.tsx / mutate-button.tsx
        ├── data/
        │   └── auth/ (use-login.ts, use-user-profile.ts)
        ├── lib/
        │   ├── supabase.ts / api.ts
        │   ├── query-client.ts / use-mutate.ts
        │   ├── utils.ts / utils.test.ts
        │   └── toast.ts / logger.ts / error-messages.ts / setup-error-handlers.ts
        ├── hooks/
        │   └── use-debounce.ts
        └── test/
            └── setup.ts
```

## example/ と配置先のマッピング

| example/ | 配置先 |
|---|---|
| `root/gitignore` | `.gitignore` |
| `root/*` | `/` (workspace root) |
| `web/env.local.example` | `apps/web/.env.local.example` (+ `.env.local` 生成) |
| `web/*` | `apps/web/` |
| `config/*` | `apps/web/` |
| `src/**` | `apps/web/src/**` |
| (script が生成) shadcn add | `apps/web/src/components/ui/` |
