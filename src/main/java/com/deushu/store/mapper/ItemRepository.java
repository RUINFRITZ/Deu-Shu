package com.deushu.store.mapper;


import org.apache.ibatis.annotations.Mapper;

import com.deushu.order.domain.ItemEntity;

import java.util.List;

/**
 * items 테이블 MyBatis 매퍼 인터페이스
 * ItemMapper.xml 의 SQL 과 1:1 대응
 */
@Mapper
public interface ItemRepository {

    /** 가게 ID로 활성 상품 목록 조회 (논리삭제 제외) */
    List<ItemEntity> findByStoreId(Long storeId);

    /** 상품 ID로 단건 조회 */
    ItemEntity findById(Long itemId);

    /** 신규 상품 등록 */
    void insert(ItemEntity item);

    /** 상품 정보 수정 */
    void update(ItemEntity item);

    /** 상품 논리삭제 (deleted_at 설정) */
    void delete(Long itemId);
}
