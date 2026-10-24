# EAP Manager

JBoss / WildFly EAP の起動・停止、複数 WAR プロジェクトのビルドとデプロイ切り替えを、Java Swing + FlatLaf の GUI から操作するツールです。

## できること

- EAP の起動 / 停止（`standalone` と `jboss-cli`）
- 複数 WAR プロジェクトの登録・編集・削除
- 選択した WAR のビルド（既定は `mvn -DskipTests package`）
- デプロイ対象 WAR の切り替え（他プロジェクトの WAR をアンデプロイしてからデプロイ）
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

1. 右上の **EAP 設定** で EAP ホーム（例: `C:\opt\jboss-eap-7.4`）を指定します。
2. 左の **追加** から WAR プロジェクトを登録します。
   - プロジェクトディレクトリ
   - ビルドコマンド（既定: `mvn -DskipTests package`）
   - WAR パス（`target/*.war` のようなグロブ可）
   - デプロイ名（空なら WAR ファイル名）
3. 左リストで「デプロイする WAR」を選びます。
4. **起動** で EAP を起動し、**ビルドしてデプロイ** で成果物を切り替えます。

環境変数は画面右側で編集します。

- **共通**: EAP 起動とすべてのビルドに付与
- **選択プロジェクト**: 選択中 WAR のビルドと、その状態での EAP 起動に付与（同名はプロジェクト側が優先）

## デプロイ方式

- **ディレクトリコピー**（既定）: `standalone/deployments` に WAR をコピーします。EAP 停止中でも配置できます。
- **jboss-cli**: 起動中の EAP に `deploy` / `undeploy` します。
