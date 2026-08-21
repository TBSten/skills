# react-vite-supabase-starter プロンプト

[English](./react-vite-supabase-starter.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[react-vite-supabase-starter スキル](../skills/react-vite-supabase-starter.ja.md)の一回限りプロンプト版。React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase のスタックで、認証・ルーティング・データアクセス層・レイアウト・テスト環境を備えた新規 Web アプリを pnpm workspace モノレポとしてスキャフォールドする。スキルのインストールは不要。

## 実行方法

以下を Claude Code (または任意のコーディングエージェント) に貼り付ける:

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/react-vite-supabase-starter/PROMPT.md を取得して、その指示に従って実行して
```

## 何をするか

- スキル一式 (scaffold script + 自己完結した `example/`) をこのリポジトリの sparse clone で取得する
- プロジェクト名・アプリ表示名・テーマカラー・Supabase の利用有無を確認する
- `scripts/scaffold.sh` をそのまま実行する (手順の再実装はしない): 全ファイル配置、プロジェクト名 / アプリ名 / プライマリカラーの置換、`pnpm install`、`pnpm dlx shadcn@latest add` による shadcn/ui コンポーネント生成、`.env.local` 雛形作成を一括で行う
- `pnpm test` と `pnpm build` が通ることを確認する (`pnpm lint` も含め生成直後から green)
- Supabase を使わない場合は、script が出力する ACTION_REQUIRED の手順に従って認証関連コードを除去する
- その後、ユーザーの要件に合わせて調整する (データアクセス Hook、テーマ変数、ページ/ルート追加)

## 参照ファイル

プロンプトはローカルへのスキルインストールの代わりに、sparse clone でスキルディレクトリ全体を GitHub から取得する:

- [skills/react-vite-supabase-starter/scripts/scaffold.sh](https://github.com/TBSten/skills/blob/main/skills/react-vite-supabase-starter/scripts/scaffold.sh) — スキャフォールド script (セットアップ手順の SSoT)
- [skills/react-vite-supabase-starter/example/](https://github.com/TBSten/skills/tree/main/skills/react-vite-supabase-starter/example) — 自己完結の雛形一式: `root/` (ワークスペースルートのファイル)、`web/` (アプリの package.json / index.html / env 雛形)、`config/` (vite, tsconfig, eslint, vitest, playwright, shadcn `components.json`)、`src/` (エントリポイント、認証ガード付きルーター、認証コンテキスト、ページ、レイアウト・共通コンポーネント、データアクセス Hook、`lib/` ユーティリティ、テーマ CSS、サンプルテスト)
- [skills/react-vite-supabase-starter/references/data-layer-pattern.md](https://github.com/TBSten/skills/blob/main/skills/react-vite-supabase-starter/references/data-layer-pattern.md) — データアクセス層パターン (Query/Mutation Hook、クエリキー規約)

## 関連

- スキル版: [skills/react-vite-supabase-starter](../skills/react-vite-supabase-starter.ja.md) — `gh skill install tbsten/skills react-vite-supabase-starter` でインストール
