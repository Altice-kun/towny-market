# TownyMarket

Paper 1.21.11向けのTowny連携型経済プラグインです。プレイヤー企業、上場株式、Towny国家の外資、独自暗号資産をSQLiteに保存し、Vault経済を決済基盤として利用します。Paperの公式開発案内に合わせ、Gradle Kotlin DSLとJava 21で構成しています。[1]

## 前提

サーバーにはPaper 1.21.11、Vault、およびVault対応経済プラグイン（EssentialsX Economy等）を導入してください。外資を使う場合はTowny Advancedも導入してください。Townyは現行リリースで1.19以降をサポートし、APIを提供しています。[2] [3]

## ビルド

```bash
./gradlew build
```

生成された`build/libs/TownyMarket-1.0.0.jar`を`plugins`へコピーし、依存プラグインを起動後にサーバーを再起動してください。Gradle Wrapperが無い環境では、Gradle 8.8以上でビルドしてください。

## コマンド

| コマンド | 内容 | 手数料 |
|---|---|---:|
| `/tm gui` | 独自市場GUIを開く | なし |
| `/tm company <名前> <株数> <株価> [説明]` | プレイヤー企業を作成 | なし |
| `/tm list <企業名>` | 自分の企業を株式上場 | 50,000 |
| `/tm foreign <記号> <供給量> <価格>` | 自分のTowny国家の外資を発行 | 100,000 |
| `/tm crypto <名前> <記号> <供給量> <開始値>` | 暗号資産を作成 | 10,000 |
| `/tm order BUY <企業名> LIMIT <数量> <価格>` | 指値買い注文 | 約定時に支払い |
| `/tm order SELL <企業名> LIMIT <数量> <価格>` | 指値売り注文 | 株式を保有している必要あり |
| `/tm order BUY <企業名> MARKET <数量>` | 成行買い注文 | 約定時に支払い |
| `/tm order SELL <企業名> MARKET <数量>` | 成行売り注文 | 株式を保有している必要あり |
| `/tm mine <記号>` | 暗号資産をマイニング。1分クールダウン | なし |
| `/tm stake <記号> <数量>` | 暗号資産をステーキング | 暗号資産残高 |
| `/tm unstake <記号>` | ステーキング解除と報酬受取 | なし |
| `/tm balance <記号>` | 暗号資産残高を確認 | なし |
| `/tm dividend <企業名> <総額>` | 自社上場株式の保有者へ配当配布 | 企業主のVault残高 |
| `/tm tax <foreign|stock|crypto> <税率%>` | 自国の外資・株式・暗号資産税率を変更 | Towny国王のみ |
| `/tm taxinfo` | 自国の税率と国庫を表示 | なし |
| `/tm market` | 上場株式・現在値・買い板／売り板数量の一覧 | なし |

企業の上場と資産作成にはVault経済プロバイダーが必要です。暗号資産の開始値と外資価格は1以上の整数で指定します。外資の発行者は、Towny上で対象国家の国王でなければなりません。

## 配当金と国別税率

上場企業の所有者は`/tm dividend <企業名> <総額>`で配当原資をVaultから支払い、保有株数に比例して全株主へ配布できます。1株配当は`総額 ÷ 発行済み保有株式数`で計算し、株主の所属国に設定された株式税率を差し引いて支払います。配当総額、1株配当、株主ごとの保有株数、総額、税額、手取り額、配布時刻はSQLiteへ保存されます。

Towny国王は`/tm tax foreign <税率>`、`/tm tax stock <税率>`、`/tm tax crypto <税率>`で自国の税率を0〜100%の範囲で設定できます。税率は国別に保存され、株式約定時の買い手側税および配当受取時の株主税へ適用されます。徴収税は自国の国庫へ加算され、税台帳へ記録されます。外資・暗号資産の税率項目も同じ国別税制テーブルで管理され、対応する資産移転処理を拡張する際に利用できます。

## TownyGUI

`/tm gui`で統合メニューを開きます。国王がTowny国に所属している場合のみ「国別税率設定」画面を開けます。税率画面では、外資・株式・暗号資産の各項目を左クリックで+1%、右クリックで-1%変更できます。税率は0〜100%に制限されます。企業経営画面には「配当を配布」ボタンがあり、クリックすると`/tm dividend <企業名> <総額>`の入力形式が表示されます。メイン画面から「株式市場」「企業経営」「暗号資産」「外資」へ移動できます。株式市場画面では現在値、買い板数量、売り板数量、注文コマンドを確認できます。企業経営画面では企業作成・上場・経営情報へアクセスできます。暗号資産画面では登録済み資産を選択し、左クリックでマイニング、右クリックで1枚ステーキング、Shift＋右クリックでステーキング解除を実行できます。暗号資産の名称・記号・供給量・開始値を入力する作成処理は、チャット入力を安全に受け取るため`/tm crypto`コマンドを使用します。

## 重要な仕様

株式については、SQLiteの`orders`テーブルで注文ID、資産ID、所有者UUID、売買区分、指値／成行、注文価格、注文数量、残数量、状態、受付時刻を数値・文字列として永続管理します。約定は価格優先・時間優先でマッチングし、`trades`テーブルに約定価格、約定数量、実行時刻を保存します。成行注文は反対側の最良気配から約定します。約定価格と数量から需給圧力を計算し、価格は次の式で更新します。`pressure = min(0.10, 約定数量 / 10000.0)`、買い約定時は`新価格 = round(約定価格 × (1 + pressure))`、売り約定時は`新価格 = max(1, round(約定価格 × (1 - pressure)))`です。これにより価格は常に1以上で、単一約定による変動幅は最大10%に制限されます。マイニングは60秒を1エポックとする決定論的スコア方式です。難易度は`D = clamp(供給量 / 100, 1, 1,000,000)`、スコアは`hash(UUID + 記号 + エポック) mod 1,000,000`で計算し、`score mod D == 0`のとき採掘成功とします。成功報酬は`R = clamp(供給量 / 1000, 1, 100)`です。失敗時も採掘時刻は更新せず、同一エポック内で再試行できます。ステーキングはAPR12%を採用し、報酬を`floor(元本 × 0.12 × 経過秒 / 31,536,000)`で計算します。解除時に元本と報酬を暗号資産残高へ戻します。暗号資産残高、ステーキング元本、開始時刻、採掘時刻、累計採掘量はSQLiteへ保存されます。

## References

[1]: https://docs.papermc.io/paper/dev/project-setup/ "PaperMC Project setup"
[2]: https://github.com/TownyAdvanced/Towny/releases "Towny Advanced releases"
[3]: https://townyadvanced.github.io/ "Towny Advanced Portal"

[4]: https://www.investopedia.com/terms/o/order-book.asp "Investopedia: Order Book"

## TM-WebConsole 0.1.0（内蔵Webコンソール）

TM-WebConsoleは別の公開Webサイトではなく、TownyMarket JAR内にHTML/CSS/JavaScriptを同梱した内蔵管理画面です。プラグイン起動時にHTTPサーバーを起動し、Minecraftサーバーのポート2026で`http://<サーバーIP>:2026`からアクセスできます。

```yaml
webconsole:
  enabled: true
  port: 2026
  password: "必ず変更する管理者パスワード"
```

初回導入時は`plugins/TownyMarket/config.yml`の`webconsole.password`を変更してください。ログイン後はHttpOnlyおよびSameSite=StrictのセッションCookieを使い、未認証の概要APIを拒否します。WebConsoleは停止時にHTTPサーバーを安全に停止します。ルーターやホスティング環境で利用する場合は、TCP 2026をMinecraftサーバーへ転送し、管理者だけがアクセスできるようファイアウォールまたはVPNで制限してください。

内蔵WebConsoleの現行APIは`POST /api/login`、認証済みの`GET /api/health`、`GET /api/summary`です。`/api/summary`では企業数、注文数、暗号資産数、約定数を取得できます。

## Project identity

The main class is `jp.alticeworks.townymarket.TownyMarket`, and the plugin author is `アルティス(TUPB_Altice) & Manus AI`.

## TownyMarket 0.7.1 security foundation

Version 0.7.1 adds the persistent foundation for audit events, frozen accounts, refund requests, asset reservations, reconciliation reports, company charters, share classes, shareholder record dates, corporate loans, national bonds, policy rates, exchange rates, and backup metadata. Audit events are purged after 30 days by the scheduled maintenance task.

Daily snapshots are written under the plugin data directory's `backups` folder. The AltimeceEncryptAlgorizmLite-compatible implementation derives five independent AES-256-GCM keys from a server IP-derived key material and applies five encryption layers. The plaintext server IP is not stored in the backup metadata. Backups include a SHA-256 integrity digest and an audit event. Change the WebConsole password and protect port 2026 with a firewall or VPN in production.

## Finance automation and add-on API

管理者は`/tm meeting <企業名> <終了まで秒> <議案名> <説明>`で株主総会議案を作成し、`/tm autodividend <企業名> <総額> <間隔秒>`で自動配当を設定できます。自動配当は60秒以上の間隔で、Paperの定期タスクが期限到来時に企業所有者がオンラインであれば配当を発行します。

融資金利は国家の政策金利と信用格付けスプレッドを合算します。格付け別スプレッドはAAA 1.0%、AA 1.5%、A 2.0%、BBB 3.0%、BB 5.0%、B 8.0%、その他12.0%です。`/tm rate <国名>`で有効金利の説明を確認できます。履歴は`loan_rate_history`へ保存できます。

有志のアドオンはPaperのサービスマネージャから`jp.alticeworks.townymarket.api.TownyMarketApi`を取得できます。公開ファサードには株式注文、配当発行、融資有効金利、金利説明が含まれ、内部SQLite実装へ直接依存しない構成です。

## TownyMarket 0.7.1 Corporate Operations

`/tm passcode <6〜12桁>`で取引パスコードをPBKDF2WithHmacSHA256のソルト付きハッシュとして保存します。`/tm approval <取引参照>`で二重承認を申請し、別のOPが`/tm approve <承認ID>`で承認できます。

企業信用は`/tm credit <企業名>`でスコアリングします。現在は上場状態などの基本要素からスコアと格付けを算出する基礎版であり、信用スコアは企業の評価値としてのみ利用します。

管理者または対象企業の社長以上の役職者は`/tm relation <企業A> <企業B> <良|普|嫌|同|提|子|親> [メモ]`で企業関係を設定できます。雇用契約は`/tm employment add <企業> <MCID> <役割> <給与> <契約日数>`、企業間請求書は`/tm invoice <発行企業> <受取企業> <金額> <支払期限までの残り日数>`で登録します。福利厚生は`/tm benefit add|remove <企業> <項目名> <内容>`で独立管理します。


## Employment and benefits command update

プレイヤー指定はUUIDではなく、オンライン中のMCID（Minecraftプレイヤー名）を使用します。契約期間は秒ではなくリアル日数で保存し、終了時刻は`日数 × 86,400,000ミリ秒`で計算します。

```text
/tm employment add <企業> <MCID> <役割> <給与> <契約日数>
/tm employment hire <企業> <MCID>
/tm employment fire <企業> <MCID>
/tm benefit add <企業> <項目名> <内容>
/tm benefit remove <企業> <項目名> <内容>
```

`employment add`は招待状態（`OFFERED`）を作成し、`hire`で採用状態（`HIRED`）、`fire`で終了状態（`TERMINATED`）へ変更します。福利厚生は雇用契約から分離し、`benefit_plans`で企業単位に管理します。企業管理コマンドは対象企業の社長以上相当の役職を要求し、対象MCIDはオンラインプレイヤー名としてサーバー側で解決します。

## Invoice and company lifecycle update

企業間請求書の期限はUnix時刻ではなく、発行時点からの残りリアル日数で指定します。たとえば`/tm invoice Alpha Beta 10000 14`は、発行時刻から14日後を支払期限として保存します。

```text
/tm invoice <発行企業> <受取企業> <金額> <支払期限までの残り日数>
/tm companyinfo <企業名>
/tm closecompany <企業名> [閉業理由]
```

`closecompany`は対象企業の所有者、社長、会長、CEO、取締役、または`company.manage`／`*`権限を持つ役職者だけが実行できます。未決済の企業間請求書が残っている場合は閉業を拒否し、企業状態・理由・時刻を`company_status`へ記録します。

## Funds ledger

資金運用の共通基盤として、個人・企業・国家で利用できる口座台帳、利用可能残高、予約残高、複式の送金ジャーナル、冪等取引キーを追加しました。金額は小数の経済額として保存し、送金元の利用可能残高を減らしてから送金先を増加させ、同一取引キーの再実行を拒否します。

```text
/tm balance funds <通貨>
/tm funds reserve <通貨> <金額> <参照ID>
/tm funds transfer <MCID> <金額> <通貨> [メモ]
```

送金先MCIDはオンラインプレイヤー名で指定します。予約は資金を利用可能残高から予約残高へ移し、注文・契約などの参照IDを一意キーとして二重予約を防止します。SQLiteの`fund_accounts`、`fund_journal`、`fund_reservations`に残高と資金移動を永続化します。

## Towny国別NISA

国王は`/n set nisa <限度額>`で自国のNISA年間利用限度額を設定できます。実行者の所属国と国王権限はサーバー側で確認し、他国の設定は変更できません。限度額`0`はNISA無効を意味します。

株主配当では、対象株主の所属国に設定されたNISA残り枠を先に非課税として消費し、超過額だけに既存の株式税率を適用します。利用額と課税額は`nisa_ledger`へ記録されます。

```text
/n set nisa <限度額>
```

## Release version

TownyMarketのリリースバージョンは`0.8.1`です。Paper 1.21.11、Java 21、Gradle Wrapperを使用してビルドします。
