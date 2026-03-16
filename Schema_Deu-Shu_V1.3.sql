-- =========================================================================
-- [ ドゥーシュー DB Schema V1.3 ]
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
-- [ ドゥーシュー ] デモ用のダミーデータ挿入スクリプト (V1.3 スキーマ準拠)
-- （韓国語：[ 드슈 ] 데모용 더미 데이터 삽입 스크립트 (V1.3 스키마 준수)）
-- トランザクションの競合状態（Race Condition）を再現するため、在庫1個の商品を作成します。
-- （韓国語：트랜잭션의 경합 상태(Race Condition)를 재현하기 위해, 재고 1개인 상품을 생성합니다.）
-- =========================================================================

-- ステップ1: 店舗を所有するためのオーナー会員（ROLE_OWNER）を1名生成します。
-- （韓国語：스텝 1: 점포를 소유하기 위한 점주 회원(ROLE_OWNER)을 1명 생성합니다.）
INSERT INTO members (email, password, last_name, first_name, last_name_kana, first_name_kana, phone, role) 
VALUES ('owner@demo.com', '$2a$10$dummyBcryptHashValue...', '山田', '太郎', 'ヤマダ', 'タロウ', '090-1111-2222', 'ROLE_OWNER');

-- ステップ2: 生成したオーナー(id=1を想定)に紐づく店舗（寿司屋）を生成します。
-- （韓国語：스텝 2: 생성한 점주(id=1 가정)에 종속된 점포(스시집)를 생성합니다.）
INSERT INTO stores (owner_id, name, business_number, category, address, lat, lng, open_time, close_time) 
VALUES (1, 'すし屋のドゥーシュー', '123-45-67890', 'SUSHI', '東京都千代田区丸の内1-1-1', 35.681236, 139.767125, '10:00:00', '22:00:00');

-- ステップ3: デモの核心となる、在庫が「1個」しかない割引商品を登録します。
-- （韓国語：스텝 3: 데모의 핵심이 되는, 재고가 '1개'밖에 없는 할인 상품을 등록합니다.）
INSERT INTO items (store_id, name, original_price, discount_price, discount_rate, stock, expire_at) 
VALUES (1, '特上サーモン寿司セット（残り1点）', 2000, 1000, 50, 1, DATE_ADD(NOW(), INTERVAL 3 HOUR));

-- ステップ4: 複数商品決済（カート機能）のテスト用に、在庫に余裕のある商品をもう一つ登録します。
-- （韓国語：스텝 4: 다중 상품 결제(장바구니 기능) 테스트용으로, 재고에 여유가 있는 상품을 하나 더 등록합니다.）
INSERT INTO items (store_id, name, original_price, discount_price, discount_rate, stock, expire_at) 
VALUES (1, '自家製玉子焼き', 500, 300, 40, 10, DATE_ADD(NOW(), INTERVAL 3 HOUR));

commit;

-- ================================================================
-- [Deu-Shu] 관리자 계정 생성 SQL
-- 실행 순서: 이 파일만 MySQL에서 실행하면 됨
--
-- 로그인 정보
--   email   : admin@deushu.jp
--   password: Admin1234!
--
-- members 테이블이 NOT NULL 컬럼을 요구하므로
-- 관리자에게 의미없는 컬럼은 '-' 더미값으로 채움
-- ================================================================

INSERT INTO members (
    email,
    password,
    last_name,
    first_name,
    last_name_kana,
    first_name_kana,
    phone,
    role,
    esg_point
) VALUES (
    'admin@deushu.jp',
    '$2b$12$CVMceu2TWPe01ZjrB5X6FehWqEKa82vBSmwd3TRVCIfkAecEYURTS',
    '-',
    '-',
    '-',
    '-',
    '-',
    'ROLE_ADMIN',
    0
);

-- 확인용
SELECT id, email, role, created_at
FROM members
WHERE role = 'ROLE_ADMIN';

ALTER TABLE stores ADD COLUMN stop_reason VARCHAR(255) DEFAULT NULL COMMENT '정지 사유';