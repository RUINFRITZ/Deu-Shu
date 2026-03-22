#  ドゥーシュー (Deu-Shu)
JSL人材開発院 Cloud連携Web開発者課程26期 チームプロジェクト

以下のリンクから、プロジェクトのシステムアーキテクチャ、機能デモンストレーション、およびチームメンバーの回顧録を含むプレゼンテーションをご覧いただけます。

[![Presentation Link](https://img.shields.io/badge/Presentation-View_Demo-F28940?style=for-the-badge&logo=github&logoColor=white)](https://github.com/RUINFRITZ/Deu-Shu/presentation_DeuShu.html)

> **「どうしよう…」という店舗オーナーの悩みを、「드슈(どうぞ)」という温かい提案へ。** > 廃棄されるはずだった食品を救い、地球に仮想の木を植える生活密着型ESG（ライフ・インテグレーテッドESG）プラットフォームです。


##  1. プロジェクトの背景とビジョン (Background & Vision)
「ドゥーシュー (Deu-Shu)」は、韓国・大田（テジョン）地方の温かい方言「드슈(召し上がってください)」と、日本語の「どうしよう（もったいない）」という言葉遊びを掛け合わせたネーミングです。
閉店間際に売れ残った食品を見つめる店主の悩みを、お財布事情の厳しい若者への安価な食事提供へと変換し、世界的に深刻化する食品ロス（Food Loss）問題を持続可能なIT技術で解決します。


##  2. 技術スタック (Tech Stack)

### Backend (コア・フレームワーク)
<p>
  <img src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/SpringBoot-3.5.11-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/SpringSecurity-3.5.11-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security">
  <img src="https://img.shields.io/badge/SpringWebFlux-3.5.11-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring WebFlux">
</p>

### Database & ORM (データ永続化)
<p>
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/MyBatis_Starter-3.0.3-000000?style=for-the-badge" alt="MyBatis 3.0.3">
  <img src="https://img.shields.io/badge/Caffeine_Cache-Included-007396?style=for-the-badge" alt="Caffeine Cache">
</p>

### Frontend & Template (画面レンダリング)
<p>
  <img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white" alt="HTML5">
  <img src="https://img.shields.io/badge/Thymeleaf-3.5.11-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white" alt="Thymeleaf">
  <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript">
  <img src="https://img.shields.io/badge/Bootstrap-5.3-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white" alt="Bootstrap 5.3">
</p>

### Infra & External API (外部連携・最適化)
<p>
  <img src="https://img.shields.io/badge/Amazon_S3-2.25.66-569A31?style=for-the-badge&logo=amazons3&logoColor=white" alt="AWS S3 2.25.66">
  <img src="https://img.shields.io/badge/JJWT-0.11.5-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT 0.11.5">
  <img src="https://img.shields.io/badge/Mapbox_Vector_Tile-3.1.0-000000?style=for-the-badge&logo=mapbox&logoColor=white" alt="Mapbox Vector Tile 3.1.0">
  <img src="https://img.shields.io/badge/Protobuf_Java-3.21.12-000000?style=for-the-badge" alt="Protobuf 3.21.12">
  <img src="https://img.shields.io/badge/Gson-Included-000000?style=for-the-badge" alt="Gson">
</p>


##  3. チーム「JSLアベンジャーズ」の開発メンバーと役割 (Team Roles)
各々の得意分野（トランザクション、LBS、UI/UX、DB設計）を持つ4人のエンジニアが集結しました。

- **パク・ユンジョン (PM)**: 
  - プロジェクト総括・DBスキーマ設計・Gitブランチ戦略管理
  - コアトランザクション制御（在庫のPessimistic Lock排他制御）
  - PortOne APIを用いた決済連動、ESGポイント付与スケジューラー
- **ゴ・ジュンソン**: 
  - LBS(位置情報)マップシミュレーション（Map UIスタイル）
  - Caffeine Cacheを活用した周辺割引店舗リストのローディング最適化
- **ベ・ボムジュ**: 
  - Spring Securityを用いた認証/認可アーキテクチャ設計
  - セッションベースのログイン処理およびユーザー/オーナーの権限（Role）分離実装
  - 事業者番号検証およびマイページ機能
- **ユ・インジェ**: 
  - UI/UX全体設計（モダンなレイアウト適用）およびS3メディア処理
  - 無限スクロールを適用したレビュー・商品リストの実装
  - @ControllerAdviceを用いたグローバル例外処理の構築


##  4. コア機能とアーキテクチャの工夫 (Core Features & Architecture)

### 1) LBSマップと動的キャッシング
ユーザーの現在地（GPS）を基準に、現在割引中の商品がある店舗のみをピンで表示します。同一地域の繰り返し照会に対するトラフィック負荷を下げるため、**Caffeine Cache**を適用しローディング速度を最適化しています。

### 2) トランザクション＆決済の厳密な制御
タイムセール環境でのオーバーブッキングを防ぐため、注文生成時の在庫仮確保（先占）と決済承認時の**悲観的ロック(Pessimistic Lock)**を実装。未決済時の自動キャンセル等、例外状況のロールバック(Rollback)を厳密に制御しています。

### 3) 堅牢なDB設計によるデータ整合性担保
- **Soft Delete制約**: 退会したユーザーのメールアドレス再加入を防ぐため、`deleted_at`による論理削除とUNIQUE制約を併用し悪意のあるアビューズを遮断。
- **セキュアなレビューシステム**: 実際の購入者のみがレビューを作成できるよう、レビューテーブルに `order_id` (UNIQUE) を必須外部キーとして設定。DBレベル（CHECK制約）で不正な星評価や허위レビューを強制的に防ぎます。
- **画像レイテンシの最適化**: マップ上のピンやリスト用の代表画像1枚は店舗テーブルに非正規化（Denormalization）して配置し、JOIN負荷を最小化。詳細な複数画像は別テーブルで管理する二元化アーキテクチャを採用しました。

### 4) ESGエコシステム（仮想の木育成）
単なる割引プラットフォームにとどまらず、商品の受け取りが完了（食品ロス削減）するたびに、サーバー上にユーザー個人の「仮想の木」を育てるESGエコポイント（炭素削減量比例）が付与されるゲーミフィケーションを導入しています。


##  5. チームのグラウンドルールとブランチ戦略 (Team Rules & Branch Strategy)
私たちはドメイン駆動のパッケージ構造（Package by Feature）を採用し、Git Conflictを最小限に抑えながら開発を進めています。

- **ブランチ戦略**: 
  - `main`: 本番リリース用（直接Push禁止・罰金制度あり、PR承認必須）
  - `develop`: 開発統合用（全メンバーのコードを毎日Pullしてマージする基盤）
  - `feature/*`: 各ドメインごとの機能開発用 (例: `feature/payment`, `feature/lbs-map`)
- **コミット＆共有**: 毎朝ブランチをPullし、作業後Commit & Pushを実施。トラブルシューティングは30分以内に解決できなければ即時チームに共有します。

## 6. 今後の拡張計画 (Future Works)

### 1. 決済ゲートウェイの抽象化および日本市場向けローカライゼーション (PayPay連動等)
現在、開発環境の物理的な制約（日本現地の電話番号やビジネスアカウント認証等）により、韓国の「PortOne」モジュールを活用して決済のコアロジック（3-Way Handshake、悲観的ロックによる同時性制御）を実装しています。

しかし、システム設計の初期段階から**「Strategy(戦略)パターン」**を導入し、決済インターフェースを抽象化(Abstraction)しています。そのため、ビジネス要件に応じて既存のコアロジック(OrderService等)を変更することなく、**PayPayやLINE Payといった日本現地の決済モジュールへ即座に拡張（差し替え）可能なアーキテクチャ**を構築しております。
