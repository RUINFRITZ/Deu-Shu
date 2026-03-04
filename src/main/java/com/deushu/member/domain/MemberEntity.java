package com.deushu.member.domain;

import lombok.Data;
import java.time.LocalDateTime;

// @Data — getter / setter / toString / equals 전부 자동 생성
// (비유: 서랍마다 손잡이를 하나씩 달지 않고, "이 서랍장 전체에 손잡이 달아줘" 하는 것)
@Data
public class MemberEntity {

    private Long id;
    private String email;
    private String password;
    private String lastName;
    private String firstName;
    private String lastNameKana;
    private String firstNameKana;
    private String phone;
    private String role;
    private Integer esgPoint;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // 로그인 요청 DTO — 프론트에서 넘어오는 { email, password } JSON을 받는 그릇
    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }
}