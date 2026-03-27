# GitHub Secrets の設定

Maven Central への公開に必要な GitHub Secrets の一覧と取得方法。

## 必要な Secrets

| Secret 名 | 説明 | 取得方法 |
|---|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal のユーザートークン（ユーザー名部分） | Central Portal で生成 |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal のユーザートークン（パスワード部分） | Central Portal で生成 |
| `SIGNING_KEY_ID` | GPG 鍵の短縮 ID | フィンガープリント末尾 8 桁 |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ | 鍵生成時に設定したもの |
| `GPG_KEY_CONTENTS` | GPG 秘密鍵の ASCII armor 形式 | `gpg --armor --export-secret-keys <KEY_ID>` |

## Sonatype Central Portal のセットアップ

### 1. アカウント作成

https://central.sonatype.com/ にアクセスし、GitHub アカウントでサインイン。

### 2. Namespace の登録

1. Central Portal → Namespaces → Add Namespace
2. Group ID に対応する namespace を入力（例: `com.example`）
3. GitHub による検証: 指定された名前のリポジトリを作成して所有権を証明
4. 検証完了後、namespace が Verified になる

### 3. User Token の生成

1. Central Portal → 右上アイコン → View Account
2. Generate User Token をクリック
3. 表示される `Username` と `Password` をコピー
   - **Username** → `MAVEN_CENTRAL_USERNAME`
   - **Password** → `MAVEN_CENTRAL_PASSWORD`

Token は一度しか表示されないため、必ずその場でコピーすること。

### 4. GitHub Secrets への登録

1. GitHub リポジトリ → Settings → Secrets and variables → Actions
2. "New repository secret" で以下 5 つを登録:
   - `MAVEN_CENTRAL_USERNAME`
   - `MAVEN_CENTRAL_PASSWORD`
   - `SIGNING_KEY_ID`
   - `SIGNING_PASSWORD`
   - `GPG_KEY_CONTENTS`

## 環境変数のマッピング

GitHub Actions ワークフロー内で、Secrets は以下の Gradle プロパティ環境変数にマッピングされる:

```yaml
env:
  ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
  ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
  ORG_GRADLE_PROJECT_signingInMemoryKeyId: ${{ secrets.SIGNING_KEY_ID }}
  ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.SIGNING_PASSWORD }}
  ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.GPG_KEY_CONTENTS }}
```

Vanniktech Maven Publish プラグインは `ORG_GRADLE_PROJECT_signingInMemoryKey*` 環境変数を自動的に in-memory signing の設定として使用する。
