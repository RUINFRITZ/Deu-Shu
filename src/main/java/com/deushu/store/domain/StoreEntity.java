package com.deushu.store.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;

@Data
public class StoreEntity {
   /**
    * 店舗カテゴリ種別 (stores.category と 1:1 対応)
    */
   public enum StoreCategory {
       BAKERY,
       SUSHI,
       LUNCHBOX,
       CAFE,
       SIDEDISH
   }
   
    /**
     * 店舗ID (PK)
     * DB: stores.id
     */
    private Long id;

    /**
     * 店舗オーナーの会員ID (FK)
     * DB: stores.owner_id → members.id
     */
    private Long ownerId;

    /**
     * 店舗名
     * DB: stores.name
     */
    private String name;

    /**
     * 事業者登録番号
     * DB: stores.business_number
     */
    private String businessNumber;

    /**
     * 業種カテゴリENUM
     * DB: stores.category (BAKERY, SUSHI, LUNCHBOX, CAFE, SIDEDISH)
     */
    private StoreCategory category;

    /**
     * 物理住所
     * DB: stores.address
     */
    private String address;

    /**
     * 緯度 (LBS用)
     * DB: stores.lat (DECIMAL(10,7))
     */
    private BigDecimal lat;

    /**
     * 経度 (LBS用)
     * DB: stores.lng (DECIMAL(10,7))
     */
    private BigDecimal lng;

    /**
     * 営業開始時間（割引販売の開始基準）
     * DB: stores.open_time (TIME)
     */
    private LocalTime openTime;

    /**
     * 営業終了時間（マップ上の販売中判定用）
     * DB: stores.close_time (TIME)
     */
    private LocalTime closeTime;

    /**
     * リスト・ポップアップ用の代表画像1枚
     * DB: stores.thumbnail_url
     */
    private String thumbnailUrl;

    /**
     * 登録日時
     * DB: stores.created_at
     */
    private LocalDateTime createdAt;

    /**
     * 更新日時
     * DB: stores.updated_at
     */
    private LocalDateTime updatedAt;

    /**
     * 論理削除フラグ（NULL以外は閉店）
     * DB: stores.deleted_at
     */
    private LocalDateTime deletedAt;
    
}
