# ドゥーシュー (Deu-Shu) / JSL26期-チームプロジェクト

> [cite_start]**「どうしよう…」というオーナーの悩みを「드슈(どうぞ)」という温かい提案へ。** [cite: 1]
> [cite_start]廃棄される食品を救い、地球に仮想の木を植える生活密着型ESGプラットフォームです。 [cite: 1]

## [cite_start]1. チーム「JSLアベンジャーズ」の開発メンバー (Team Roles) [cite: 1]
- [cite_start]**パク・ユンジョン (PM)**: コアトランザクション制御、決済(PortOne)連動、ESGスケジューラー [cite: 1]
- [cite_start]**ゴ・ジュンソン**: LBS(位置情報)マップシミュレーション、データキャッシュ(Caffeine)最適化 [cite: 1]
- [cite_start]**ベ・ボムジュ**: 認証/認可(Spring Security)、セッション管理、ユーザー/オーナーの権限分離 [cite: 1]
- [cite_start]**ユ・インジェ**: UI/UX設計、S3メディア処理、無限スクロールレビュー、グローバル例外処理 [cite: 1]

## [cite_start]2. 技術スタック (Tech Stack) [cite: 1]
- [cite_start]**Backend**: Java 17, Spring Boot 3.x, MyBatis, Spring Security [cite: 1]
- [cite_start]**Database**: MySQL 8.0 [cite: 1]
- [cite_start]**Frontend**: HTML5, Thymeleaf, JavaScript, Kakao/Leaflet Map API [cite: 1]
- [cite_start]**Infra/Others**: GitHub, AWS S3, PortOne API [cite: 1]

## [cite_start]3. コア機能 (Core Features) [cite: 1]
1. [cite_start]**LBSマップ割引店舗検索**: ユーザーの現在地に基づき、現在割引中の商品がある店舗のみをピンで表示します。 [cite: 1]
2. [cite_start]**トランザクション＆決済制御**: 在庫の仮押さえと決済時の悲観的ロック(Pessimistic Lock)により、オーバーブッキングを防ぎます。 [cite: 1]
3. [cite_start]**ESGエコシステム**: 商品受け取り完了時、サーバー上に仮想の木を育てるESGエコポイントを付与します。 [cite: 1]
4. [cite_start]**セキュアなレビューシステム**: 実際の購入者(UUID認証完了者)のみがレビューを作成できるようDBレベルで強制します。 [cite: 1]

## [cite_start]4. ブランチ戦略 (Branch Strategy) [cite: 1]
- [cite_start]`main`: 本番リリース用（直接Push不可、PR承認必須） [cite: 1]
- [cite_start]`develop`: 開発統合用（全メンバーのコードをマージ・テストする基盤） [cite: 1]
- [cite_start]`feature/*`: 各ドメインごとの機能開発用 (例: `feature/login`, `feature/mvt-map`) [cite: 1]
