package com.deushu.store.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

<<<<<<< Updated upstream
@Data
public class StoreDetailResponse {

	private StoreInfo store;       // stores
    private List<String> images;   // store_images
    private RatingInfo rating;     // reviews 집계
    private List<ItemInfo> items;  // items 판매중

    @Data
    public static class StoreInfo {
        private Long id;
        private String name;
        private String category;
        private String address;
        private Double lat;
        private Double lng;
        private String thumbnailUrl;
        private String openTime;   // "10:00:00" 같이 TIME 문자열로 내려도 OK
        private String closeTime;
    }

    @Data
    public static class RatingInfo {
        private Double avg;   // 리뷰 0개면 0.0 처리
        private Integer count;
    }

    @Data
    public static class ItemInfo {
        private Long id;
        private String name;
        private Integer originalPrice;
        private Integer discountPrice;
        private Integer discountRate;
        private Integer stock;
        private LocalDateTime expireAt;
        private String thumbnailUrl;
    }
=======
/**
 * 가게 상세 페이지 전용 응답 DTO
 *
 * 📌 목적: - 가게 상세 화면에서 필요한 데이터만 View(Thymeleaf)로 전달하기 위한 DTO - 여러 테이블(stores,
 * store_images, reviews, items)을 조합한 화면 전용 모델
 *
 * 📌 특징: - 도메인(Entity)을 그대로 보내지 않고, 화면에 필요한 값만 재정의해서 전달 (응집도 ↑, 결합도 ↓)
 */
@Data
public class StoreDetailResponse {

	/**
	 * 가게 기본 정보 (stores 테이블 기반)
	 */
	private StoreInfo store;

	/**
	 * 가게 이미지 목록 (store_images 테이블 기반) - 여러 장 존재 가능
	 */
	private List<String> images;

	/**
	 * 가게 평점 집계 정보 (reviews 테이블 집계) - avg : 평균 평점 - count : 리뷰 개수
	 */
	private RatingInfo rating;

	/**
	 * 현재 판매 중인 상품 목록 (items 테이블) - 마감 시간(expireAt) 지나지 않은 것만 조회 - 재고(stock) 0 이상
	 */
	private List<ItemInfo> items;

	/**
	 * =============================== 가게 기본 정보 내부 DTO
	 * ===============================
	 */
	@Data
	public static class StoreInfo {

		/** 가게 PK */
		private Long id;

		/** 가게 이름 */
		private String name;

		/** 업종 카테고리 (ex: 베이커리, 카페 등) */
		private String category;

		/** 가게 주소 */
		private String address;

		/** 위도 (지도 표시용) */
		private Double lat;

		/** 경도 (지도 표시용) */
		private Double lng;

		/** 대표 썸네일 이미지 URL */
		private String thumbnailUrl;

		/**
		 * 영업 시작 시간 - "10:00:00" 형식의 문자열 - LocalTime 대신 String으로 내려서 프론트 처리 단순화
		 */
		private String openTime;

		/**
		 * 영업 종료 시간
		 */
		private String closeTime;
	}

	/**
	 * =============================== 평점 집계 정보 DTO ===============================
	 */
	@Data
	public static class RatingInfo {

		/**
		 * 평균 평점 - SQL: AVG(rating) - 소수점 포함 가능
		 */
		private Double avg;

		/**
		 * 전체 리뷰 개수 - SQL: COUNT(*)
		 */
		private Integer count;
	}

	/**
	 * =============================== 판매 상품 정보 DTO ===============================
	 */
	@Data
	public static class ItemInfo {

		/** 상품 PK */
		private Long id;

		/** 상품명 */
		private String name;

		/** 원가 */
		private Integer originalPrice;

		/** 할인 적용 가격 */
		private Integer discountPrice;

		/**
		 * 할인율 (예: 30%) - 계산해서 내려주면 프론트에서 연산 안해도 됨
		 */
		private Integer discountRate;

		/**
		 * 남은 재고 수량 - 0이면 품절 처리
		 */
		private Integer stock;

		/**
		 * 마감 시간 - 현재 시간과 비교해서 판매 가능 여부 판단
		 */
		private LocalDateTime expireAt;

		/** 상품 썸네일 이미지 URL */
		private String thumbnailUrl;
	}
>>>>>>> Stashed changes
}