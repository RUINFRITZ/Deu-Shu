package com.deushu.esg.dto;

import lombok.Data;

/**
 * 마이페이지 ESG 숲 섹션 표시용 DTO
 * virtual_forests 테이블 + 계산 필드 포함
 */
@Data
public class EsgForestDto {

    // virtual_forests 테이블 데이터
    private Double  cumulativeCarbon;  // 누적 탄소 절감량 (kg)
    private Long    cumulativeDays;    // 누적 소나무 일수
    private Integer treeCount;         // 완성된 소나무 수
    private Integer forestLevel;       // 숲 레벨 (1~5)

    /**
     * 숲 레벨에 맞는 gif 이미지 경로 반환
     * /static/images/esg/ 하위에 forest-level1.gif ~ forest-level5.gif 파일 필요
     */
    public String getForestGifPath() {
        int level = (forestLevel != null) ? Math.min(forestLevel, 5) : 1;
        return "/images/esg/forest-level" + level + ".gif";
    }

    /**
     * 숲 레벨에 맞는 일본어 레벨 이름 반환
     */
    public String getForestLevelName() {
        if (forestLevel == null) return "種（たね）";
        return switch (forestLevel) {
            case 1  -> "種（たね）";
            case 2  -> "苗木（なえぎ）";
            case 3  -> "小さな森";
            case 4  -> "森";
            default -> "大きな森";
        };
    }

    /**
     * 다음 레벨까지 남은 소나무 일수 계산
     * 365일 단위로 레벨업 — 현재 레벨 기준 잔여 일수 반환
     */
    public Long getDaysUntilNextLevel() {
        if (cumulativeDays == null) return 365L;
        long nextLevelDays = (long)(Math.floor((double) cumulativeDays / 365) + 1) * 365;
        return nextLevelDays - cumulativeDays;
    }
}