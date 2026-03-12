package com.deushu.store.service;

import com.deushu.store.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FavoriteService — 즐겨찾기 비즈니스 로직
 *
 * toggle() 한 메서드로 추가/해제를 처리해
 * 프론트에서 단일 API 호출만으로 상태 반전 가능
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    /**
     * 즐겨찾기 토글
     * - 이미 즐겨찾기 상태면 해제
     * - 아니면 추가
     *
     * @return true  = 추가됨
     *         false = 해제됨
     */
    @Transactional
    public boolean toggle(Long memberId, Long storeId) {
        boolean alreadyFavorited = favoriteMapper.exists(memberId, storeId);
        if (alreadyFavorited) {
            favoriteMapper.delete(memberId, storeId);
            return false;
        } else {
            favoriteMapper.insert(memberId, storeId);
            return true;
        }
    }

    /**
     * 즐겨찾기 여부 조회
     * (상세 페이지 진입 시 하트 아이콘 초기 상태 결정용)
     */
    @Transactional(readOnly = true)
    public boolean isFavorited(Long memberId, Long storeId) {
        return favoriteMapper.exists(memberId, storeId);
    }

    /**
     * 회원의 즐겨찾기 storeId 목록
     * (마이페이지 즐겨찾기 탭용)
     */
    @Transactional(readOnly = true)
    public List<Long> getFavoriteStoreIds(Long memberId) {
        return favoriteMapper.findStoreIdsByMember(memberId);
    }

    /**
     * 마이페이지 즐겨찾기 탭 — 가게 상세 목록
     */
    @Transactional(readOnly = true)
    public List<com.deushu.store.dto.FavoriteStoreDto> getFavoriteStores(Long memberId) {
        return favoriteMapper.findFavoriteStoresByMember(memberId);
    }

    /**
     * 특정 가게의 즐겨찾기 수
     */
    @Transactional(readOnly = true)
    public int getFavoriteCount(Long storeId) {
        return favoriteMapper.countByStore(storeId);
    }
}