# GPG 鍵のセットアップ

Maven Central に公開するアーティファクトの署名に必要な GPG 鍵の生成・エクスポート手順。

## 1. GPG 鍵の生成

```bash
gpg --full-generate-key
```

選択肢:
- **鍵の種類**: RSA and RSA（デフォルト）
- **鍵長**: 3072 bit 以上推奨
- **有効期限**: 任意（0 = 無期限）
- **名前**: 公開される開発者名
- **メールアドレス**: 公開されるメールアドレス
- **パスフレーズ**: 強力なパスフレーズを設定 → `SIGNING_PASSWORD` として使用

## 2. 鍵情報の確認

```bash
gpg --list-keys --keyid-format long
```

出力例:
```
pub   rsa3072/3A318B3456DE4680 2026-03-26 [SC]
      B60C75AAF77204CDC5BE8FBC3A318B3456DE4680
uid                 [ultimate] Name (project) <email@example.com>
```

- **Key ID (短縮)**: フィンガープリントの末尾 8 桁 → `56DE4680` → `SIGNING_KEY_ID`
- **フィンガープリント**: `B60C75AAF77204CDC5BE8FBC3A318B3456DE4680`

## 3. 公開鍵をキーサーバーに登録

Maven Central はアーティファクトの署名を検証するため、公開鍵をキーサーバーに登録する必要がある。

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <FINGERPRINT>
```

複数のキーサーバーに送信しておくと信頼性が上がる:

```bash
gpg --keyserver keys.openpgp.org --send-keys <FINGERPRINT>
gpg --keyserver pgp.mit.edu --send-keys <FINGERPRINT>
```

## 4. 秘密鍵のエクスポート

GitHub Actions の in-memory signing で使用するため、秘密鍵を ASCII armor 形式でエクスポートする。

```bash
gpg --armor --export-secret-keys <KEY_ID>
```

出力全体（`-----BEGIN PGP PRIVATE KEY BLOCK-----` から `-----END PGP PRIVATE KEY BLOCK-----` まで）を `GPG_KEY_CONTENTS` として GitHub Secrets に登録する。

## まとめ

| 項目 | 取得方法 | GitHub Secret 名 |
|---|---|---|
| GPG Key ID | フィンガープリント末尾 8 桁 | `SIGNING_KEY_ID` |
| パスフレーズ | 鍵生成時に設定 | `SIGNING_PASSWORD` |
| 秘密鍵 (ASCII armor) | `gpg --armor --export-secret-keys` | `GPG_KEY_CONTENTS` |
