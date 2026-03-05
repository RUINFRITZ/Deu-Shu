package com.deushu.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 무한 스크롤용 페이징 응답 래퍼
 * GET /api/stores?page=N&size=10 의 응답 구조
 * JS 측에서 { content:[...], last: boolean } 으로 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyStoreResponse {

    /** 현재 페이지 가게 목록 */
    private List<StoreMapDto> content;

    /** 현재 페이지 번호 (0-based) */
    private int page;

    /** 페이지 크기 */
    private int size;

    /** 마지막 페이지 여부 (true면 JS가 observer 해제) */
    private boolean last;

    /** 전체 건수 */
    private long totalElements;

    /* ── 정적 팩토리 ── */
    public static NearbyStoreResponse of(List<StoreMapDto> content,
                                         int page, int size,
                                         long totalElements) {
        boolean isLast = (long) (page + 1) * size >= totalElements;
        return NearbyStoreResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .last(isLast)
                .totalElements(totalElements)
                .build();
    }
}