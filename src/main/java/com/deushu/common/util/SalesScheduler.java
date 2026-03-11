package com.deushu.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 매출 정산 스케줄러
 *
 * 매일 정오(12:00)에 자동으로 새로고침 이벤트를 발생시킨다.
 * 실제 집계는 SalesRepository 가 담당하며, 스케줄러는 캐시 무효화·알림 역할만 수행한다.
 *
 * 사용 방법:
 *  - Application 메인 클래스에 @EnableScheduling 을 추가하거나
 *  - 이 클래스에 @EnableScheduling 을 추가하면 동작한다.
 *
 * 주의:
 *  - 현재는 로그 출력 + lastRefreshedAt 갱신만 수행한다.
 *  - 향후 WebSocket 알림이나 Spring Cache(@CacheEvict) 연동으로 확장 가능하다.
 */
@Slf4j
@Component
@EnableScheduling
public class SalesScheduler {

    // 스케줄러가 마지막으로 실행된 시각 (프론트에서 /api/owner/sales/last-refresh 로 조회 가능)
    private volatile LocalDateTime lastRefreshedAt = LocalDateTime.now();

    // =====================================================================
    // 자동 스케줄 — 매일 정오 12:00 실행
    // =====================================================================

    /**
     * cron 표현식: "0 0 12 * * ?" → 매일 12시 00분 00초
     * 시간대는 서버 JVM 기본 시간대를 따른다 (application.yml에서 JST 설정 권장).
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void dailySalesRefresh() {
        lastRefreshedAt = LocalDateTime.now();
        log.info("[SalesScheduler] 매출 정산 자동 새로고침 실행 — {}",
                lastRefreshedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // ※ 실제 구현 예시 (Spring Cache 연동 시):
        // cacheManager.getCache("salesSummary").clear();

        // ※ WebSocket 알림 연동 시:
        // messagingTemplate.convertAndSend("/topic/sales", "refresh");
    }

    // =====================================================================
    // 마지막 새로고침 시각 조회 (OwnerController 에서 호출 가능)
    // =====================================================================

    /** 마지막 자동 새로고침 시각 반환 */
    public LocalDateTime getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    /** 수동 새로고침 시 외부에서 호출하여 시각을 갱신 */
    public void markRefreshed() {
        lastRefreshedAt = LocalDateTime.now();
        log.info("[SalesScheduler] 매출 정산 수동 새로고침 실행 — {}",
                lastRefreshedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}