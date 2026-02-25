-- =========================================================================
-- [ ドゥーシュー DB Schema V1.2]
-- =========================================================================

-- 1. 会員テーブル (Members) - べさん担当領域
CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'ログインID (Email)',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt暗号化パスワード',
    name VARCHAR(50) NOT NULL COMMENT '実名（実名認証用）',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '電話番号',
    role ENUM('ROLE_USER', 'ROLE_OWNER', 'ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER' COMMENT '権限階層',
    esg_point INT NOT NULL DEFAULT 0 COMMENT '仮想の木を育てるESGエコポイント',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登録日時',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '論理削除フラグ（NULL以外は退会・停止アカウント）'
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
-- 画像並び替え検索のパフォーマンスを跳ね上げる複合インデックス
CREATE INDEX idx_store_images_store_sort ON store_images(store_id, sort_order);

-- 4. お気に入りテーブル (Favorites) - ユさん担当領域
CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_store UNIQUE (member_id, store_id) COMMENT '同一店舗への重複登録を弾き返す制約',
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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '論理削除（日々のUPSERT時の非活性化用）',
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

-- 6. 注文・決済テーブル (Orders) - パクさん領域
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    order_status ENUM('PAYMENT_PENDING', 'PAYMENT_COMPLETED', 'PICKUP_COMPLETED', 'CANCELED', 'EXPIRED') NOT NULL,
    quantity INT NOT NULL,
    total_price INT NOT NULL,
    pickup_code VARCHAR(36) NOT NULL UNIQUE COMMENT '受け取り用UUID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- 7. 仮想政府事業者認証テーブル (Mock_Business_Registry) - べさん領域
CREATE TABLE mock_business_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_number VARCHAR(20) NOT NULL UNIQUE,
    owner_name VARCHAR(50) NOT NULL,
    store_name VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE', 'CLOSED', 'SUSPENDED') DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);