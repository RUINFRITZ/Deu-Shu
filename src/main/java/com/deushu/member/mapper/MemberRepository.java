package com.deushu.member.mapper;

import com.deushu.member.domain.MemberEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberRepository {

    // 이메일로 회원 조회 (탈퇴하지 않은 회원만)
    MemberEntity findByEmail(String email);

    // ID로 회원 조회 (마이페이지 최신 정보 로드용)
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
}