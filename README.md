# ドゥーシュー「Deu-Shu」 [![Presentation Link](https://img.shields.io/badge/Presentation-View_Demo-F28940?style=for-the-badge&logo=github&logoColor=white)](https://ruinfritz.github.io/Deu-Shu/presentation_DeuShu.html)
**JSL人材開発院 Cloud連携Web開発者課程26期 チームプロジェクト**

> **「どうしよう…」という店舗オーナーの悩みを、「드슈『どうぞ』」という温かい提案へ。** <br>
> 廃棄されるはずだった食品を救い、地球に仮想の木を植える生活密着型ESG「ライフ・インテグレーテッドESG」プラットフォームです。

---

## 1. プロジェクトの背景とビジョン 「Background & Vision」
「ドゥーシュー 『Deu-Shu』」は、韓国・大田「テジョン」地方の温かい方言「드슈『召し上がってください』」と、日本語の「どうしよう『もったいない』」という言葉遊びを掛け合わせたネーミングです。
閉店間際に売れ残った食品を見つめる店主の悩みを、お財布事情の厳しい若者への安価な食事提供へと変換し、世界的に深刻化する食品ロス「Food Loss」問題を持続可能なIT技術で解決します。

---

## 2. 技術スタック「Tech Stack」

### Backend「コア・フレームワーク」
![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.5.11-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/SpringSecurity-3.5.11-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring WebFlux](https://img.shields.io/badge/SpringWebFlux-3.5.11-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

### Database & ORM「データ永続化」
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![MyBatis](https://img.shields.io/badge/MyBatis_Starter-3.0.3-000000?style=for-the-badge)
![Caffeine Cache](https://img.shields.io/badge/Caffeine_Cache-Included-007396?style=for-the-badge)

### Frontend & Template「画面レンダリング」
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.5.11-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)

### Infra & External API「外部連携・最適化」
![AWS S3](https://img.shields.io/badge/Amazon_S3-2.25.66-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![JWT](https://img.shields.io/badge/JJWT-0.11.5-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Mapbox](https://img.shields.io/badge/Mapbox_Vector_Tile-3.1.0-000000?style=for-the-badge&logo=mapbox&logoColor=white)
![Protobuf](https://img.shields.io/badge/Protobuf_Java-3.21.12-000000?style=for-the-badge)

---

## 3. チーム「JSLアベンジャーズ」の開発メンバーと役割「Team Roles」
各々の得意分野「トランザクション、LBS、UI/UX、DB設計」を持つ4人のエンジニアが集結しました。

| 名前 | 役割 | 担当ドメイン・主要実装機能 |
| :--- | :--- | :--- |
| **朴潤正「パク・ユンジョン」** | **PM / Backend** | • プロジェクト総括・DBスキーマ設計・Gitブランチ戦略管理<br>• コアトランザクション制御「在庫のPessimistic Lock排他制御」<br>• PortOne API決済連動、ESGポイント付与スケジューラー |
| **高俊成「コ・ジュンソン」** | **Backend** | • LBS「位置情報」マップシミュレーション「Map UIスタイル」<br>• Caffeine Cacheを活用した周辺割引店舗リストのローディング最適化 |
| **兪仁在「ユ・インジェ」** | **Frontend / Backend** | • UI/UX全体設計「モダンなレイアウト適用」およびS3メディア処理<br>• 無限スクロールを適用したレビュー・商品リストの実装<br>• `@ControllerAdvice` を用いたグローバル例外処理の構築 |
| **裵範柱「ベ・ボムジュ」** | **Backend** | • Spring Securityを用いた認証/認可アーキテクチャ設計<br>• セッションベースのログイン・権限「Role」分離実装<br>• 事業者番号検証およびマイページ機能 |

---

## 4. コア機能とアーキテクチャの工夫「Core Features & Architecture」

| 分類 | 機能概要 | 解決した技術的課題 | 適用技術 |
| :--- | :--- | :--- | :--- |
| **LBS & Cache** | マップ上での周辺店舗動的ロード | 同一地域の繰り返し照会によるDB負荷の軽減とUX向上 | `Leaflet`, `Caffeine Cache` |
| **Transaction** | 厳格な決済と在庫制御 | タイムセール特有のオーバーブッキング防止と決済未完了時の安全なロールバック | `Pessimistic Lock`, `@Transactional`, `PortOne API` |
| **Data Integrity** | 堅牢なDB制約によるアビューズ防止 | `deleted_at`による論理削除とUNIQUE制約を用いた不正加入・虚偽レビューの物理的遮断 | `MySQL (Soft Delete, CHECK Constraint)` |
| **ESG Eco** | 仮想の木育成「ゲーミフィケーション」| 割引を通じた食品ロス削減量を炭素削減量に換算し、ユーザーの継続的な参加を誘導 | `Spring Scheduler`, カスタムドメインロジック |

---

## 5. チームのグラウンドルールとブランチ戦略「Team Rules & Branch Strategy」
私たちはドメイン駆動のパッケージ構造「Package by Feature」を採用し、Git Conflictを最小限に抑えながら開発を進めました。

- **ブランチ戦略**: 
  - `main`: 本番リリース用「直接Push禁止・罰金制度あり、PR承認必須」
  - `develop`: 開発統合用「全メンバーのコードを毎日Pullしてマージする基盤」
  - `feature/*`: 各ドメインごとの機能開発用「例: `feature/payment`, `feature/lbs-map`」
- **コミット＆共有**: 毎朝ブランチをPullし、作業後Commit & Pushを実施。トラブルシューティングは30分以内に解決できなければ即時チームに共有するルールを徹底しました。

---

## 6. 今後の拡張計画「Future Works」

### 決済ゲートウェイの抽象化および日本市場向けローカライゼーション「PayPay連動等」
現在、開発環境の物理的な制約「日本現地の電話番号やビジネスアカウント認証等」により、韓国の「PortOne」モジュールを活用して決済のコアロジック「3-Way Handshake、悲観的ロックによる同時性制御」を実装しています。

しかし、システム設計の初期段階から**「Strategy(戦略)パターン」**を導入し、決済インターフェースを抽象化(Abstraction)しています。そのため、ビジネス要件に応じて既存のコアロジック(OrderService等)を変更することなく、**PayPayやLINE Payといった日本現地の決済モジュールへ即座に拡張（差し替え）可能なアーキテクチャ**を構築しております。
