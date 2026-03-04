package com.deushu.common.config;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FR-M02  Caffeine 캐시 설정
 *
 * 캐시 종류:
 *   - todayStorePins : 오늘 기준 마감 할인 가게 핀 리스트 (TTL 1시간)
 *                     FR-M05 주문/재고 변경 시 @CacheEvict 로 전체 무효화
 *
 * build.gradle 의존성:
 *   implementation 'org.springframework.boot:spring-boot-starter-cache'
 *   implementation 'com.github.ben-manes.caffeine:caffeine'
 */
@EnableCaching
@Configuration
public class CacheConfig {

    /**
     * todayStorePins 캐시
     * - TTL(expireAfterWrite): 1시간
     *   → 재고가 자주 변하는 서비스 특성상 1시간이 적절한 기본값
     *   → FR-M05(@CacheEvict) 로 즉시 무효화 가능하므로 TTL이 길어도 안전
     * - 최대 항목 수: 200
     *   → 지역/날짜 조합 수 고려 (지역 ~25개 * 날짜 1일 = 25개 이내)
     *   → 여유 있게 200으로 설정
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCache todayStorePins = new CaffeineCache(
            "todayStorePins",
            Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(200)
                .recordStats()           // 캐시 히트율 통계 (운영 모니터링용)
                .build()
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(todayStorePins));
        return manager;
    }
}