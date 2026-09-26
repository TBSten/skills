# docs/ — ドキュメントサイト

Astro Starlight。Gradle ビルドとは独立していて、pnpm で動かす。

```bash
pnpm install
pnpm astro dev --background   # 開発サーバ。`pnpm astro dev status` / `logs` / `stop` で管理
pnpm build              # dist/ に出力。内部リンク切れがあると失敗する
```

API リファレンスまで含めて手元で見るなら、先にリポジトリのルートで `./gradlew generateApiDocs` を
回す (Dokka の HTML が `public/api-docs/` に入る。gitignore 済み)。

## 構成

| パス | 内容 |
|---|---|
| `astro.config.mjs` | サイト設定。`site` / `base` (GitHub Pages の URL)、locale、サイドバー |
| `src/content/docs/**` | 英語のページ (root locale) |
| `src/content/docs/ja/**` | 日本語のページ。英語と**同じ相対パス**に置く |
| `public/api-docs/` | Dokka の出力 (生成物。コミットしない) |
| `public/ja/api-docs/index.html` | 日本語サイドバーからの `/ja/api-docs/` を英語の 1 本へ飛ばすリダイレクト |

デプロイは `.github/workflows/docs.yml` (Dokka → `pnpm build` → GitHub Pages)。

## 決まっている判断

- **本文は人が書く。** エージェントが用意してよいのは、サイドバーの配線と空ページ (frontmatter と TODO) まで。
  頼まれたときだけ本文を書く
- **サイドバーは手書き。** `autogenerate` を使わず、順序を人が決める。ページを足したら
  `astro.config.mjs` の `sidebar` に英語 `label` と `translations.ja` を両方書く
- **en/ja は同時に更新する** (`.claude/rules/markdown.md`)。訳が間に合わなくても、ページは両方に作る
- **トップページ (`index`) はサイドバーに出さない。** サイトタイトルから辿れる
- **存在しないページへのリンクはビルドが落ちる** (starlight-links-validator)。書けるページから順に出す
- **ページ内のリンクは `base` 込みの絶対パスで書く** (`/<repo>/guides/basic-usage/`、日本語は `/<repo>/ja/...`)。
  相対リンクは validator がエラーにする
- **サイドバーの `link` には `base` を付けない。** Starlight が自動で前置するので、付けると
  `/<repo>/<repo>/...` になる
- **`/api-docs/` は validator の対象外** (`astro.config.mjs` の `exclude`)。Starlight の外で作られるため
- **コード例はコンパイルできる形で書く。** 可能なら `integrationTest/` のコードを引用し、そこへリンクする。
  docs は CI でコンパイルされないので、引用しない例はいずれ腐る
- ユーザー向けの公開 API を変えたら、Installation / Guides の例が古くなっていないか確認する

## 公開 URL を変えるとき

- `<owner>.github.io` リポジトリ (user site) やカスタムドメインで公開するなら、`astro.config.mjs` の
  `base` を消し、ページ内リンク・`public/ja/api-docs/index.html`・hero の `link` から `/<repo>` を外す
- README (en/ja) の Docs リンクも合わせて直す

## lockfile

`pnpm-lock.yaml` はコミットする。CI は `pnpm install --frozen-lockfile` で入れるため、
`package.json` を変えたら `pnpm install` して lockfile も一緒にコミットする。
