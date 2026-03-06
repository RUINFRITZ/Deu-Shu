package com.deushu.review.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

<<<<<<< Updated upstream
@Data
public class ReviewListResponse {
    private List<ReviewItem> items;
    private Long nextCursor;

    @Data
    public static class ReviewItem {
        private Long id;
        private String memberName;   // members.name join해서 내려줄 값
        private String title;
        private String content;
        private Double rating;
        private String photoUrl;
        private LocalDateTime createdAt;
    }
}
=======
/**
 * 리뷰 목록 조회 응답 DTO
 *
 * 📌 목적: - 특정 가게의 리뷰 리스트를 화면에 전달하기 위한 전용 DTO - 무한스크롤(커서 기반 페이징)을 고려한 구조
 *
 * 📌 특징: - 단순 List가 아니라 nextCursor를 포함해 다음 페이지 조회가 가능하도록 설계
 */
@Data
public class ReviewListResponse {

	/**
	 * 현재 페이지에 내려줄 리뷰 목록
	 */
	private List<ReviewItem> items;

	/**
	 * 다음 페이지 조회용 커서 값
	 *
	 * 예: - 마지막 리뷰 ID를 cursor로 사용 - 다음 요청에서 ?cursor=xx 로 전달
	 *
	 * null이면 더 이상 데이터 없음
	 */
	private Long nextCursor;

	/**
	 * =============================== 개별 리뷰 정보 DTO ===============================
	 */
	@Data
	public static class ReviewItem {

		/** 리뷰 PK */
		private Long id;

		/**
		 * 작성자 이름 - members 테이블 join 해서 가져옴 - FK(member_id) → members.name
		 */
		private String memberName;

		/** 리뷰 제목 */
		private String title;

		/** 리뷰 본문 내용 */
		private String content;

		/**
		 * 평점 (1~5) - 평균 계산용
		 */
		private Integer rating;

		/**
		 * 리뷰 첨부 이미지 URL - 없으면 null 가능
		 */
		private String photoUrl;

		/**
		 * 작성 시간 - 최신순 정렬 기준
		 */
		private LocalDateTime createdAt;
	}
}
>>>>>>> Stashed changes
