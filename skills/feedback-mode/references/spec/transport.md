# 共通仕様: ストレージ（Sink）とクライアント ⇔ 受け口の通信

画面（フィードバッククライアント）から記録が置き場に入るまでの通り道。adapter は「画面側」と「Transport」だけを差し替え、受け口と Sink は共通のまま使う。

```
画面（adapter）──Transport──▶ 受け口 ──▶ Sink（置き場）◀── loop（list / read / archive）
 部品を選ぶ・材料を集める   IPC / HTTP /      submitFeedback:        ファイル / メモリ / HTTP
 （data.md §4 の body）     toolbar のメッセージ  添付 → 組み立て → redact → 保存
```

- Sink を変えるのは受け口の側だけで、画面側は変えない。
- Transport を変えるのは adapter だけで、受け口の処理（`submitFeedback`）は変えない。

## 1. Sink の契約

コード: `assets/templates/core/feedback-core.mjs` の JSDoc（JS）、`assets/templates/core-ts/sink-types.ts`（TypeScript）。

```
Sink { kind, capabilities{put,list,read,archive,doc}
  putAttachments(files[{name,data,mime}], {now}) → { refs{name→参照}, dir, error }  先に呼ぶ。参照が文に書かれる
  putEntry({text, now})        → { id, ref, state:'open', bytes, attempts }         採番して保存（text に関数を渡すと、採番した id を受けて文を作れる: core-ts）
  list({state:'open'|'archived'|'all'}) → EntryInfo[]
  read(id)                     → { text, ...EntryInfo }
  archive(id, {note?})         → EntryInfo{state:'archived'}                        2 回目は何もしない
  writeDoc(relPath, text)      → void          任意（capabilities.doc）。上書きする文書（メモ）。採番しない・list に出ない
  readDoc(relPath)             → text | null   任意（capabilities.doc）
}
```

守る規則:

1. 採番は `data.md` §3.1（Archive の番号も数える・競合を避ける・上書きしない）。
2. id・名前に人の入力を入れない。
3. スクリーンショットの失敗は投げず、理由を返す（文に「撮れませんでした: …」と出る）。
4. 保存の失敗は理由つきで投げる（画面が吹き出しに出して、モードを続ける）。
5. 記録の保存が失敗したら、先に置いた添付を消す（孤児を残さない）。添付の合計にも上限を置く。
6. 文書（`writeDoc`）のパスは置き場からの相対の正規形（`safeDocPath`。絶対パス・`..`・空・`\`・NUL を拒否）。ファイルではシンボリックリンクで外に出るものも拒否し、tmp に書いて rename する。
7. redact は Sink ではなく組み立て（`buildFeedbackMarkdown`）の 1 か所で掛ける。HTTP の受け口は `entries` に来た文にも掛ける。

契約テスト: `assets/templates/core/sink-contract.test.mjs`（JS。メモリ・ファイル・HTTP に同じテスト）、`assets/templates/core-ts/sink-contract.test.ts`（vitest。メモリ・ファイル）。新しい Sink を作ったら、ここに足して通す。

## 2. Sink の実装

| 実装 | ファイル | 概要 |
|---|---|---|
| ローカルのファイル（既定） | `core/file-sink.mjs`・`core-ts/file-sink.ts` | `<root>/<dir>/yymmdd-xx.md`・`<dir>/archived/`・添付は `attachDir/<日時>/`（TS 版は `<dir>/attachments/`）。`archive` は rename + 対応メモ |
| メモリ | `core/memory-sink.mjs`・`core-ts/memory-sink.ts` | テスト・モックアップ用。契約テストの基準 |
| HTTP | `core/http-sink.mjs` | 別マシンの受け口（下の §3.2）に送る・読む・閉じる。画面で文を組み立てられる環境向け |

ほかの置き場（Issue トラッカーなど）に入れたいときは、§1 の契約どおりに Sink を作り、契約テストに足して通してから使う（同梱はしていない）。loop 側の読み方・閉じ方は `references/loop/loop.md` §2.1。

## 3. Transport と受け口

| Transport | 受け口 | 使う adapter | ファイル |
|---|---|---|---|
| IPC | Electron の main（`registerFeedback`。チャネル `app:feedback-save`） | electron | `adapters/desktop/electron/main-feedback.mjs` |
| toolbar のメッセージ | Astro の統合（`toolbar.on('feedback-mode:save')`。HMR の WebSocket に乗る） | astro | `adapters/web/astro/feedback-integration.ts` |
| HTTP | 開発用サーバ `feedback-dev-server.mjs`（`node` で別プロセス） | dom・react・tauri・（モバイル） | `core/feedback-dev-server.mjs` |
| クリップボード | 無い（人が会話に貼る） | single-html | （HTML の中に書く） |

### 3.1 結果の形

どの Transport も、結果は `{ ok, id, ref, error }`。`ok: false` なら画面は `error` を吹き出しに出してモードを続け、`ok: true` ならモードを終える。toolbar のメッセージのように非同期で返る Transport は、`requestId` で依頼と結果を対応づけ、時間切れ（約 10 秒）で失敗にする。

### 3.2 HTTP の受け口の API

| メソッドとパス | 中身 | 使う側 |
|---|---|---|
| `POST /__feedback` | submission（`data.md` §4。画像は base64）→ `submitFeedback` → `{ ok, id, ref, bytes, shots }` | 文を組み立てない画面（Flutter・ネイティブ・dom） |
| `POST /__feedback/attachments` | `{ files:[{name,mime,dataBase64}], now? }` → `{ ok, refs, dir, error }` | HTTP の Sink |
| `POST /__feedback/entries` | `{ text, now? }` → `{ ok, entry }`（受け口でも redact） | HTTP の Sink |
| `GET /__feedback/entries?state=open` | 一覧 | loop（別マシンのとき） |
| `GET /__feedback/entries/:id` | 全文 | loop |
| `POST /__feedback/entries/:id/archive` | `{ note }` | loop |

環境変数: `FEEDBACK_REPO_ROOT`・`FEEDBACK_DIR`（既定 `feedback`）・`FEEDBACK_ATTACH_DIR`（既定 `.local/tmp/feedback`）・`FEEDBACK_HOST`（既定 `127.0.0.1`）・`FEEDBACK_PORT`（既定 `7373`）・`FEEDBACK_TOKEN`。

## 4. 守り

- 受け口は開発時だけ起動・登録する（本番から外す方法は各 adapter のファイル）。
- 既定は `127.0.0.1` にだけ bind する。ブラウザの Origin は localhost 系だけ許す（ほかは 403）。
- LAN に出す（スマホ実機・別マシン・`astro dev --host`）ときは作業中だけにし、`FEEDBACK_TOKEN`（ヘッダ `x-feedback-token`）を必須にする。起動時に「LAN から書き込める」と警告を 1 行出す。
- 依頼の形・コメントの字数（20,000 字）・全体の大きさ（30MB）・添付の数と大きさを受け口で確かめる。
- 送信先の URL・鍵は開発用のビルド変数（`.env.development` など。コミットしない）だけに置く。テンプレートは URL の既定値を持たない。
