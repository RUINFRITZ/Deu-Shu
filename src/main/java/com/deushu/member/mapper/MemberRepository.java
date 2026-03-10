package com.deushu.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deushu.member.domain.MemberEntity;
import com.deushu.order.dto.MyPageOrderResponseDto;

@Mapper
public interface MemberRepository {

    // 이메일로 회원 조회 (탈퇴하지 않은 회원만)
    MemberEntity findByEmail(String email);

    // ID로 회원 조회
    MemberEntity findById(Long id);

    // 이름 + 전화번호로 회원 조회 (이메일 찾기용)
    MemberEntity findByNameAndPhone(MemberEntity member);

    // 회원가입
    void insert(MemberEntity member);

    // 정보 수정 (이름, 후리가나, 전화번호)
    void update(MemberEntity member);

    // 비밀번호 변경
    void updatePassword(MemberEntity member);

    // 회원 탈퇴 (논리삭제 — deleted_at 설정)
    void withdraw(Long id);
    
    // =====================================================================
    // マイページ：本日のピックアップ対象注文を照会
    // =====================================================================
    List<MyPageOrderResponseDto> findTodayPickupOrders(@Param("memberId") Long memberId);
    
    // =====================================================================
    // マイページ：ユーザーの全注文履歴を照会 (ステータス・日付問わず)
    /* 過去の履歴も含まれるため、年月日(YYYY-MM-DD)までフォーマットします */
    // =====================================================================
    List<MyPageOrderResponseDto> findAllOrderHistory(@Param("memberId") Long memberId);
}