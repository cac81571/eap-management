# EAP Manager

JBoss / WildFly EAP の起動・停止、複数 WAR プロジェクトのビルドとデプロイを、Java Swing + FlatLaf の GUI から操作するツールです。

## できること

- EAP の起動 / デバッグ起動 / 停止（`standalone --debug` と `jboss-cli`）
- 選択プロジェクトへ VS Code 用 `launch.json`（Attach to EAP）と `tasks.json`（GUI と同じ起動 / 停止 / ビルド / デプロイ / アンデプロイ。パスは `settings.json` の `eap.*` を `${config:eap.home}` で参照）を出力
- 複数 WAR プロジェクトの登録・編集・削除
- 選択した WAR のビルド（既定は `mvn -DskipTests package`）
- 選択した WAR のデプロイ / ビルド＋デプロイ（「既存の WAR を削除する」がオンなら既存 WAR をアンデプロイしてから配置）
- 共通 / プロジェクト単位の環境変数管理
- ライト / ダークテーマ

設定は `%USERPROFILE%\.eap-management\config.json` に保存されます。

## 必要環境

- Java 21+
- Maven 3.9+（ビルド時）
- 操作対象の JBoss EAP / WildFly
- WAR プロジェクト側のビルドツール（Maven など）

## 起動方法

```bat
run.bat
```

または:

```bat
mvn -DskipTests package
java -jar target\eap-manager.jar
```

開発時:

```bat
mvn -q exec:java
```

## 使い方

1. メニューの **設定 → EAP 設定** で EAP ホーム（例: `C:\opt\jboss-eap-7.4`）を指定します。テーマは **表示 → ダークテーマ** から切り替えます。
2. 左の **追加** から WAR プロジェクトを登録します。
   - プロジェクトディレクトリ
   - ビルドコマンド（既定: `mvn -DskipTests package`）
   - WAR パス（`target/*.war` のようなグロブ可）
   - デプロイ名（空なら WAR ファイル名）
3. 左リストで「デプロイする WAR」を選びます。
4. **起動** で EAP を起動し、**ビルド** / **デプロイ** / **ビルド＋デプロイ** で成果物を配置します。
   既定で **既存の WAR を削除する** がオンです。オフにすると既存のデプロイを残したまま追加配置します。
5. **tasks.json を出力** で VS Code 用タスクを書き出します。タスクの実行から **起動 / デバッグ起動 / 停止 / 再起動 / ビルド / デプロイ / アンデプロイ / ビルド＋デプロイ** を GUI と同じ手順で実行できます。デプロイ方式がディレクトリコピーなら `deployments` へコピーし、jboss-cli なら CLI デプロイします。EAP ホーム、WAR ファイル、ヒープ、ポートは `.vscode/settings.json` の `eap.*` に出し、`tasks.json` からは `${config:eap.home}` / `${config:eap.warFile}` のように参照します。

環境変数は画面右側で編集します。

- **共通**: EAP 起動とすべてのビルドに付与
- **選択プロジェクト**: 選択中 WAR のビルドと、その状態での EAP 起動に付与（同名はプロジェクト側が優先）

## デプロイ方式

- **ディレクトリコピー**（既定）: `standalone/deployments` に WAR をコピーします。EAP 停止中でも配置できます。
- **jboss-cli**: 起動中の EAP に `deploy` / `undeploy` します。
