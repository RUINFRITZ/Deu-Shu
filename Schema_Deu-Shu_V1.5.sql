-- =========================================================================
-- [ ドゥーシュー DB Schema V1.5 ]
-- =========================================================================

-- 1. 会員テーブル (Members) - べさん担当領域
CREATE TABLE members (
	id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
	email           VARCHAR(100) NOT NULL UNIQUE  COMMENT 'ログインID (Email)',
	password        VARCHAR(255) NOT NULL         COMMENT 'BCrypt暗号化パスワード',
	last_name       VARCHAR(50)  NOT NULL         COMMENT '姓（実名認証用）',
	first_name      VARCHAR(50)  NOT NULL         COMMENT '名（実名認証用）',
	last_name_kana  VARCHAR(50)  NOT NULL         COMMENT '姓フリガナ（カタカナ全角）',
	first_name_kana VARCHAR(50)  NOT NULL         COMMENT '名フリガナ（カタカナ全角）',
	phone           VARCHAR(20)  NOT NULL 	      COMMENT '電話番号',
	role            ENUM('ROLE_USER', 'ROLE_OWNER', 'ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER' COMMENT '権限階層',
	esg_point       INT          NOT NULL DEFAULT 0 COMMENT '仮想の木を育てるESGエコポイント',
	created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '登録日時',
	updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	deleted_at      DATETIME     DEFAULT NULL     COMMENT '論理削除フラグ（NULL以外は退会・停止アカウント）'
);

-- 2. 店舗テーブル (Stores) - ゴさん・べさん連携領域
CREATE TABLE stores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL COMMENT '店舗オーナー(FK)',
    name VARCHAR(100) NOT NULL COMMENT '店舗名',
    business_number VARCHAR(20) NOT NULL UNIQUE COMMENT '事業者登録番号',
    category ENUM('BAKERY', 'SUSHI', 'LUNCHBOX', 'CAFE', 'SIDEDISH') NOT NULL COMMENT '業種カテゴリ',
    address VARCHAR(255) NOT NULL COMMENT '物理住所',
    lat DECIMAL(10, 7) NOT NULL COMMENT '緯度 (LBS用)',
    lng DECIMAL(10, 7) NOT NULL COMMENT '経度 (LBS用)',
    open_time TIME NOT NULL COMMENT '営業開始時間（割引販売の開始基準）',
    close_time TIME NOT NULL COMMENT '営業終了時間（マップ上の販売中判定用）',
    thumbnail_url VARCHAR(500) COMMENT 'リスト・ポップアップ用の代表画像1枚',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '論理削除フラグ（店舗閉鎖時）',
    info TEXT DEFAULT NULL COMMENT '店舗からのお知らせ・説明文',
    FOREIGN KEY (owner_id) REFERENCES members(id) ON DELETE CASCADE
);

-- 3. 店舗複数画像テーブル (Store_Images) - ユさん担当領域
CREATE TABLE store_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL COMMENT 'S3バケットの画像URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '表示順序（UI描画用）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_images_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);
CREATE INDEX idx_store_images_store_sort ON store_images(store_id, sort_order);

-- 4. お気に入りテーブル (Favorites) - ユさん担当領域
CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_store UNIQUE (member_id, store_id),
    CONSTRAINT fk_favorites_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

-- 5. 商品テーブル (Items / Foods) - パクさん・ゴさん領域
CREATE TABLE items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '商品名',
    original_price INT NOT NULL COMMENT '定価',
    discount_price INT NOT NULL COMMENT '割引価格',
    discount_rate INT NOT NULL COMMENT '割引率（非正規化カラム：ソート用）',
    stock INT NOT NULL COMMENT '残在庫数（トランザクション制御の核心）',
    expire_at DATETIME NOT NULL COMMENT '販売期限（廃棄時間）',
    thumbnail_url VARCHAR(500) COMMENT '商品リスト用の代表画像1枚（JOIN回避用）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '論理削除（日々のUPSERT時の非活性化用）',
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

-- 6. 商品複数画像テーブル (Item_Images) - パクさん・ユさん連携領域
-- 商品（パン、弁当など）ごとの複数アングル画像を管理する新規テーブル
CREATE TABLE item_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL COMMENT 'S3バケットの画像URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '表示順序（UI描画用）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_images_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);
CREATE INDEX idx_item_images_item_sort ON item_images(item_id, sort_order);

-- 7. 注文・決済テーブル (Orders) - パクさん領域
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL COMMENT '注文者(FK) / (한국어: 주문자(FK))',
    store_id BIGINT NOT NULL COMMENT '対象店舗(FK): 1回の注文は1つの店舗に限定する設計 / (한국어: 대상 점포(FK): 1회의 주문은 1개의 점포로 한정하는 설계)',
    order_status ENUM('PAYMENT_PENDING', 'PAYMENT_COMPLETED', 'PICKUP_COMPLETED', 'CANCELED', 'EXPIRED') NOT NULL,
    total_price INT NOT NULL COMMENT '注文全体の合計金額 / (한국어: 주문 전체의 총합 금액)',
    pickup_code VARCHAR(36) NOT NULL UNIQUE COMMENT '受け取り用UUID / (한국어: 픽업용 UUID)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

-- 8. 注文詳細テーブル (Order_Items) - カート内の個別商品たち (1:N)
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '所属する注文(FK) / (한국어: 소속된 주문(FK))',
    item_id BIGINT NOT NULL COMMENT '購入した商品(FK) / (한국어: 구매한 상품(FK))',
    quantity INT NOT NULL COMMENT '購入数量 / (한국어: 구매 수량)',
    order_price INT NOT NULL COMMENT '購入当時の単価（スナップショット）/ (한국어: 구매 당시의 단가 (스냅샷))',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- 9. 仮想政府事業者認証テーブル (Mock_Business_Registry) - べさん領域
CREATE TABLE mock_business_registry (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    business_number VARCHAR(20)  NOT NULL UNIQUE,
    last_name       VARCHAR(50)  NOT NULL         COMMENT '代表者姓',
    first_name      VARCHAR(50)  NOT NULL         COMMENT '代表者名',
    last_name_kana  VARCHAR(50)  NOT NULL         COMMENT '代表者姓フリガナ（カタカナ全角）',
    first_name_kana VARCHAR(50)  NOT NULL         COMMENT '代表者名フリガナ（カタカナ全角）',
    store_name      VARCHAR(100) NOT NULL,
    address         VARCHAR(255) NOT NULL         COMMENT '店舗住所（stores.addressへコピー）',
    status          ENUM('ACTIVE', 'CLOSED', 'SUSPENDED') DEFAULT 'ACTIVE',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- 10. レビューテーブル (Reviews) - ユさん担当領域
-- 実際に商品を購入・受け取り完了したユーザーのみが店舗を評価できるセキュアな構造
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL COMMENT 'レビュー作成者(FK)',
    store_id BIGINT NOT NULL COMMENT '対象店舗(FK)',
    order_id BIGINT NOT NULL UNIQUE COMMENT '1注文につき1レビューを強制する注文ID(FK)',
    title VARCHAR(100) NOT NULL COMMENT 'レビューの件名',
    content TEXT NOT NULL COMMENT 'レビューの本文',
    photo_url VARCHAR(500) COMMENT '添付画像(S3バケットURL)',
    rating DECIMAL(2,1) NOT NULL CHECK(rating >= 1 AND rating <= 5) COMMENT '星評価(1~5)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '論理削除フラグ',
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- 11. クレーム・問い合わせテーブル (Complaints) - 運営(Admin)領域
-- ユーザーと店舗、および運営(開発者)間のトラブルを中央集権的に管理するテーブル
CREATE TABLE complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL COMMENT '申告者(ユーザーまたはオーナー)のID(FK)',
    target_store_id BIGINT COMMENT '申告対象が店舗の場合の店舗ID(FK: User->Store用。それ以外はNULL)',
    complaint_type ENUM('USER_TO_STORE', 'USER_TO_ADMIN', 'OWNER_TO_ADMIN') NOT NULL COMMENT 'クレームの方向性',
    title VARCHAR(100) NOT NULL COMMENT 'クレームの件名',
    content TEXT NOT NULL COMMENT 'クレームの詳細内容',
    status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'PENDING' COMMENT '対応ステータス',
    admin_reply TEXT COMMENT '運営(開発者)からの返答・処理結果',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (reporter_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (target_store_id) REFERENCES stores(id) ON DELETE CASCADE
);

-- =========================================================================
-- [ 드슈 ESG/탄소 절감 확장 Schema ]
-- =========================================================================

-- 12. 탄소 배출 계수 마스터 (Carbon_Emission_Factors)
-- 카테고리별로 1개 수량당 절감되는 탄소량(kg)을 정의합니다.
CREATE TABLE carbon_emission_factors (
    category      ENUM('BAKERY', 'SUSHI', 'LUNCHBOX', 'CAFE', 'SIDEDISH') PRIMARY KEY,
    carbon_kg     DECIMAL(5, 2) NOT NULL COMMENT '품목 1개당 평균 탄소 절감량 (kg CO2e)',
    tree_days     INT NOT NULL COMMENT '소나무 1그루가 며칠 동안 흡수해야 하는 양인지 계산된 값',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 13. 주문 탄소 절감 이력 (Order_Carbon_Savings)
-- 각 주문이 완료(PICKUP_COMPLETED)될 때 계산된 탄소량을 기록합니다.
CREATE TABLE order_carbon_savings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE COMMENT '대상 주문 ID (FK)',
    member_id       BIGINT NOT NULL COMMENT '사용자 ID (FK)',
    total_carbon_kg DECIMAL(10, 2) NOT NULL COMMENT '해당 주문으로 절감한 총 탄소량',
    total_tree_days INT NOT NULL COMMENT '해당 주문으로 절감한 총 소나무 시간(일)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);

-- 14. 가상의 숲 대시보드 (Virtual_Forests)
-- 사용자의 누적 기여도를 관리하고 '가상의 숲' 상태를 결정합니다.
CREATE TABLE virtual_forests (
    member_id          BIGINT PRIMARY KEY COMMENT '사용자 ID (FK)',
    cumulative_carbon  DECIMAL(15, 2) DEFAULT 0.00 COMMENT '누적 탄소 절감량 (kg)',
    cumulative_days    BIGINT DEFAULT 0 COMMENT '누적 소나무 시간 (일)',
    tree_count         INT DEFAULT 0 COMMENT '완성된 소나무 수 (누적 시간 / 365일)',
    forest_level       INT DEFAULT 1 COMMENT '숲 레벨 (1: 씨앗, 2: 묘목, 3: 나무 한그루, 4: 작은 숲 등)',
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);

-- =========================================================================
-- [ ドゥーシュー ] デモ用のダミーデータ挿入スクリプト (V1.3 スキーマ準拠)
-- （韓国語：[ 드슈 ] 데모용 더미 데이터 삽입 스크립트 (V1.3 스키마 준수)）
-- トランザクションの競合状態（Race Condition）を再現するため、在庫1個の商品を作成します。
-- （韓国語：트랜잭션의 경합 상태(Race Condition)를 재현하기 위해, 재고 1개인 상품을 생성합니다.）
-- =========================================================================

-- admin 계정 생성 비밀번호 12341234

insert into members (email,password,last_name,first_name,last_name_kana,first_name_kana,phone,role) values
('admin@deushu.com','$2a$10$MtEFBVxfOcgTQAwuJLguI.fpjNGdXNvoBn24TlMKzqovfHde8FrfG'
,'admin','admin','admin','admin',01012345678,'ROLE_ADMIN');

-- 탄소절감량 계산을 위한 기본 데이터.
INSERT INTO carbon_emission_factors (category, carbon_kg, tree_days) VALUES
('BAKERY',   0.60, 33),  -- 소나무 1개월분
('SUSHI',    1.20, 66),  -- 소나무 2개월분
('LUNCHBOX', 2.00, 110), -- 소나무 4개월분
('CAFE',     0.40, 22),  -- 소나무 3주분
('SIDEDISH', 0.80, 44);  -- 소나무 1.5개월분	

-- 사업자정보 더미데이터
-- =====================================================
-- mock_business_registry 더미 데이터 (30건)
-- 도쿄 都心 (千代田区・中央区・港区)
-- 카테고리: BAKERY(6) / SUSHI(6) / LUNCHBOX(6) / CAFE(6) / SIDEDISH(6)
-- =====================================================

INSERT INTO mock_business_registry
    (business_number, last_name, first_name, last_name_kana, first_name_kana, store_name, address, status)
VALUES

-- ■ BAKERY (6)
    ('101-11-00001', '木村', '健太', 'キムラ', 'ケンタ',
     '木村ベーカリー', '東京都千代田区丸の内1-2-1', 'ACTIVE'),

    ('101-11-00002', '佐藤', '美咲', 'サトウ', 'ミサキ',
     'パン工房さくら', '東京都千代田区大手町2-1-3', 'ACTIVE'),

    ('101-11-00003', '高橋', '誠', 'タカハシ', 'マコト',
     'ブーランジェリー高橋', '東京都中央区銀座3-4-5', 'ACTIVE'),

    ('101-11-00004', '渡辺', '陽子', 'ワタナベ', 'ヨウコ',
     '渡辺のクロワッサン', '東京都中央区銀座6-7-8', 'ACTIVE'),

    ('101-11-00005', '伊藤', '龍一', 'イトウ', 'リュウイチ',
     '麻布ブレッド工房', '東京都港区麻布十番2-3-1', 'ACTIVE'),

    ('101-11-00006', '中村', '奈々', 'ナカムラ', 'ナナ',
     'ナカムラ丸の内パン店', '東京都千代田区有楽町1-5-2', 'ACTIVE'),

-- ■ SUSHI (6)
    ('202-22-00001', '山本', '隆', 'ヤマモト', 'タカシ',
     '寿司 匠 銀座', '東京都中央区銀座4-6-1', 'ACTIVE'),

    ('202-22-00002', '小林', '一郎', 'コバヤシ', 'イチロウ',
     '銀座すし権', '東京都中央区銀座7-2-3', 'ACTIVE'),

    ('202-22-00003', '加藤', '浩二', 'カトウ', 'コウジ',
     '築地すし三昧', '東京都中央区築地4-1-2', 'ACTIVE'),

    ('202-22-00004', '吉田', '剛', 'ヨシダ', 'ゴウ',
     '江戸前寿司 浜田', '東京都港区新橋2-8-5', 'ACTIVE'),

    ('202-22-00005', '山口', '恵', 'ヤマグチ', 'メグミ',
     'すし処 丸の内', '東京都千代田区丸の内3-1-1', 'ACTIVE'),

    ('202-22-00006', '松本', '大輔', 'マツモト', 'ダイスケ',
     '芝寿司', '東京都港区芝1-4-7', 'ACTIVE'),

-- ■ LUNCHBOX (6)
    ('303-33-00001', '井上', '久美子', 'イノウエ', 'クミコ',
     'お弁当の里 大手町店', '東京都千代田区大手町1-3-2', 'ACTIVE'),

    ('303-33-00002', '木下', '信二', 'キノシタ', 'シンジ',
     '虎ノ門弁当', '東京都港区虎ノ門1-2-9', 'ACTIVE'),

    ('303-33-00003', '清水', '真理', 'シミズ', 'マリ',
     '銀座デリ弁当', '東京都中央区銀座5-3-4', 'ACTIVE'),

    ('303-33-00004', '池田', '幸雄', 'イケダ', 'ユキオ',
     '丸の内ランチボックス', '東京都千代田区丸の内2-4-1', 'ACTIVE'),

    ('303-33-00005', '橋本', '彩', 'ハシモト', 'アヤ',
     'お弁当処 霞が関', '東京都千代田区霞が関3-2-5', 'ACTIVE'),

    ('303-33-00006', '前田', '健', 'マエダ', 'ケン',
     '港区手作り弁当', '東京都港区芝公園2-6-3', 'ACTIVE'),

-- ■ CAFE (6)
    ('404-44-00001', '藤田', '恭子', 'フジタ', 'キョウコ',
     'カフェ 丸の内', '東京都千代田区丸の内1-8-3', 'ACTIVE'),

    ('404-44-00002', '岡田', '武', 'オカダ', 'タケシ',
     '銀座コーヒーハウス', '東京都中央区銀座2-9-6', 'ACTIVE'),

    ('404-44-00003', '後藤', '由美', 'ゴトウ', 'ユミ',
     'カフェ日比谷 後藤', '東京都千代田区有楽町1-1-2', 'ACTIVE'),

    ('404-44-00004', '長谷川', '修', 'ハセガワ', 'オサム',
     '芝パークカフェ', '東京都港区芝公園4-2-8', 'ACTIVE'),

    ('404-44-00005', '村田', 'さくら', 'ムラタ', 'サクラ',
     '霞が関珈琲 村田', '東京都千代田区霞が関1-1-1', 'ACTIVE'),

    ('404-44-00006', '斉藤', '雄大', 'サイトウ', 'ユウダイ',
     '新橋スタンドカフェ', '東京都港区新橋1-3-6', 'ACTIVE'),

-- ■ SIDEDISH (6)
    ('505-55-00001', '田村', '博', 'タムラ', 'ヒロシ',
     '惣菜の田村 大手町', '東京都千代田区大手町2-2-4', 'ACTIVE'),

    ('505-55-00002', '石川', '幸子', 'イシカワ', 'サチコ',
     '銀座デリカ 石川', '東京都中央区銀座1-5-9', 'ACTIVE'),

    ('505-55-00003', '林', '次郎', 'ハヤシ', 'ジロウ',
     'お惣菜 丸の内 林', '東京都千代田区丸の内3-5-2', 'ACTIVE'),

    ('505-55-00004', '小川', '典子', 'オガワ', 'ノリコ',
     '港の惣菜屋', '東京都港区浜松町2-1-4', 'ACTIVE'),

    ('505-55-00005', '近藤', '正樹', 'コンドウ', 'マサキ',
     '築地惣菜 近藤', '東京都中央区築地5-2-1', 'ACTIVE'),

    ('505-55-00006', '坂本', '真紀', 'サカモト', 'マキ',
     '千代田総菜店', '東京都千代田区神田神保町1-3-7', 'ACTIVE');
COMMIT;