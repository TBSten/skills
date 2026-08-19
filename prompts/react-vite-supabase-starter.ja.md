# react-vite-supabase-starter プロンプト

[English](./react-vite-supabase-starter.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[react-vite-supabase-starter スキル](../skills/react-vite-supabase-starter.ja.md)の一回限りプロンプト版。React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase のスタックで、認証・ルーティング・データアクセス層・レイアウト・テスト環境を備えた新規 Web アプリを pnpm workspace モノレポとしてスキャフォールドする。スキルのインストールは不要。

## 実行方法

以下を Claude Code (または任意のコーディングエージェント) に貼り付ける:

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/react-vite-supabase-starter/PROMPT.md を取得して、その指示に従って実行して
```

## 何をするか

- プロジェクト名・アプリ表示名・テーマカラー・Supabase の利用有無を確認する
- `apps/web` 配下に Vite + React + TypeScript (strict) アプリを持つ pnpm workspace モノレポを作成する
- Tailwind CSS v4 のテーマ (CSS 変数、`--primary` のカスタマイズ可) と shadcn/ui コンポーネントを設定する
- TanStack Router (認証ガード付きの型安全ルーティング) と TanStack Query を組み込む
- Supabase 認証コンテキストと、画面から Supabase を隠蔽する `src/data/<domain>/` データアクセス層を追加する (省略可)
- レイアウト・共通コンポーネント、エラーハンドリングユーティリティ、サンプルページ (`/login`, `/`, 404/500) を配置する
- Vitest + Testing Library と Playwright をセットアップし、`pnpm test` と `pnpm build` が通ることを確認する
- 参照ファイルはローカルへのスキルインストールではなく GitHub から取得する (ファイル数が多いため sparse clone 推奨)

## 参照ファイル

プロンプトはローカルのスキルインストールの代わりに、以下を GitHub から取得する:

- [skills/react-vite-supabase-starter/example/config/](https://github.com/TBSten/skills/tree/main/skills/react-vite-supabase-starter/example/config) — 設定ファイルのサンプル (vite, tsconfig, eslint, vitest, playwright, shadcn `components.json`)
- [skills/react-vite-supabase-starter/example/src/](https://github.com/TBSten/skills/tree/main/skills/react-vite-supabase-starter/example/src) — ソースコードのサンプル (エントリポイント、認証コンテキスト、レイアウト・共通コンポーネント、`lib/` ユーティリティ、テーマ CSS、テストセットアップ)
- [skills/react-vite-supabase-starter/references/data-layer-pattern.md](https://github.com/TBSten/skills/blob/main/skills/react-vite-supabase-starter/references/data-layer-pattern.md) — データアクセス層パターン (Query/Mutation Hook、クエリキー規約)

## 関連

- スキル版: [skills/react-vite-supabase-starter](../skills/react-vite-supabase-starter.ja.md) — `gh skill install tbsten/skills react-vite-supabase-starter` でインストール
