# React + Vite + Supabase スタックの新規 Web アプリスキャフォールド

このプロンプトは [TBSten/skills](https://github.com/TBSten/skills) の `prompts/react-vite-supabase-starter` として配布されている一回限りのプロンプト。React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase のスタックで、認証・ルーティング・データアクセス層・レイアウト・テスト環境を一式備えた新規 Web アプリ (pnpm workspace モノレポ) をスキャフォールドする。

セットアップ手順の SSoT は skill 内の `scripts/scaffold.sh`。
**script を読解・書き換え・再実装せず、そのまま実行すること。**

## Step 1: スキル一式の取得 (sparse clone)

scaffold script が example/ 一式を参照するため、sparse clone でスキルを丸ごと取得する:

```sh
git clone --depth 1 --filter=blob:none --sparse https://github.com/TBSten/skills.git /tmp/tbsten-skills
git -C /tmp/tbsten-skills sparse-checkout set skills/react-vite-supabase-starter
```

以降、`/tmp/tbsten-skills/skills/react-vite-supabase-starter/` を `<starter>/` と表記する。

## Step 2: 確認事項ヒアリング

以下を確認してから開始する:

1. **プロジェクト名** — kebab-case (例: `my-app`)
2. **アプリ名** — UI に表示する名前 (例: 「マイアプリ」)
3. **テーマカラー** — プライマリカラーの hex 値。指定がなければデフォルト (`#8F5A3C`) を使用
4. **Supabase の利用有無** — 不要なら `--no-supabase` を付け、出力される除去手順に従う

## Step 3: scaffold.sh 実行

```sh
bash <starter>/scripts/scaffold.sh \
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
- ACTION_REQUIRED が出力されたら、その内容 (追加コマンドの実行や Supabase 除去) を必ず実施する

## Step 4: 動作確認

```sh
cd <project-name>
pnpm test    # vitest (サンプルテスト付き)
pnpm build   # tsc -b + vite build
```

両方 green になることを確認してから次へ進む。`pnpm lint` も通る状態で生成される。

## Step 5: 要件に合わせた調整

- **環境変数**: `apps/web/.env.local` の `VITE_SUPABASE_URL` / `VITE_SUPABASE_ANON_KEY` を実値に書き換える
- **データアクセス層**: 画面コンポーネントから supabase を直接 import しない。
  `src/data/<domain>/` の hook 経由にする (`<starter>/references/data-layer-pattern.md` を参照)。
  サンプル: `data/auth/use-login.ts` (Mutation) / `data/auth/use-user-profile.ts` (Query)
- **テーマ**: `src/index.css` の CSS 変数 (`--primary` / `--accent` / `--ring` など) で全体を制御
- **ページ追加**: `src/pages/` にコンポーネントを作り、`src/router.tsx` にルートを追加
- **UI コンポーネント追加**: `cd apps/web && pnpm dlx shadcn@latest add <component>`
- **Supabase を使わない場合**: `<starter>/SKILL.md` の「Supabase を使わない場合」の手順で
  関連コードを除去し、`pnpm test && pnpm build` で確認する
