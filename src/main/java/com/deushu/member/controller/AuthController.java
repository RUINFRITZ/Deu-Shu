package com.deushu.member.controller;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deushu.member.domain.MemberEntity;
import com.deushu.member.domain.MemberEntity.LoginRequest;
import com.deushu.member.mapper.MemberRepository;

import jakarta.servlet.http.HttpSession;
import lombok.Data;

// 인증 관련 API 컨트롤러 (회원가입 / 로그인 / 로그아웃 / 정보수정 / 탈퇴)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 생성자 주입
    public AuthController(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================================
    // 요청 DTO — @Data 가 getter / setter 자동 생성
    // =====================================================================

    @Data
    public static class SignupRequest {
        private String email;
        private String password;
        private String lastName;
        private String firstName;
        private String lastNameKana;
        private String firstNameKana;
        private String phone;
    }

    @Data
    public static class UpdateRequest {
        private String lastName;
        private String firstName;
        private String lastNameKana;
        private String firstNameKana;
        private String phone;
        private String currentPw;
        private String newPw;
    }

    @Data
    public static class ResetPasswordRequest {
        private String email;
        private String lastName;
        private String firstName;
        private String phone;
        private String newPw;
    }

    @Data
    public static class FindEmailRequest {
        private String lastName;
        private String firstName;
        private String phone;
    }

    // =====================================================================
    // API
    // =====================================================================

    // 일반 회원가입 — POST /api/auth/signup
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody SignupRequest req) {
        Map<String, Object> result = new HashMap<>();

        // 이메일 중복 확인
        MemberEntity 기존회원 = memberRepository.findByEmail(req.getEmail());
        if (기존회원 != null) {
            result.put("success", false);
            result.put("message", "すでに登録されているメールアドレスです");
            return ResponseEntity.badRequest().body(result);
        }

        // 회원 정보 세팅 후 INSERT
        MemberEntity 새회원 = new MemberEntity();
        새회원.setEmail(req.getEmail());
        새회원.setPassword(passwordEncoder.encode(req.getPassword())); // BCrypt 암호화
        새회원.setLastName(req.getLastName());
        새회원.setFirstName(req.getFirstName());
        새회원.setLastNameKana(req.getLastNameKana());
        새회원.setFirstNameKana(req.getFirstNameKana());
        새회원.setPhone(req.getPhone());
        memberRepository.insert(새회원);

        result.put("success", true);
        result.put("message", "会員登録が完了しました");
        return ResponseEntity.ok(result);
    }

    // 일반 회원 로그인 — POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req,
                                                     HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        MemberEntity 회원 = memberRepository.findByEmail(req.getEmail());

        if (회원 == null || !passwordEncoder.matches(req.getPassword(), 회원.getPassword())) {
            result.put("success", false);
            result.put("message", "メールアドレスまたはパスワードが正しくありません");
            return ResponseEntity.badRequest().body(result);
        }

        if (!"ROLE_USER".equals(회원.getRole())) {
            result.put("success", false);
            result.put("message", "一般会員アカウントではありません");
            return ResponseEntity.badRequest().body(result);
        }

        session.setAttribute("memberId", 회원.getId());
        
        // =====================================================================
        // Spring Security コンテキストとの強制同期ブリッジコード
        // =====================================================================
        // ユーザーの権限(ROLE_USERなど)をSecurityが理解できる形(GrantedAuthority)に変換
        List<GrantedAuthority> authorities = 
                Collections.singletonList(new SimpleGrantedAuthority(회원.getRole()));
        
        // パスワードは既に検証済みなのでnullを渡し、プリンシパルとしてPK(会員ID)を登録
        Authentication authentication = 
                new UsernamePasswordAuthenticationToken(회원.getId(), null, authorities);
        
        // Securityのグローバルコンテキストに認証完了オブジェクトをセット
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 以降のAPIリクエストでも認証状態を維持できるよう、セッションにContextを明示的に保存
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                SecurityContextHolder.getContext());
        // =====================================================================

        result.put("success", true);
        result.put("message", "ログイン成功");
        return ResponseEntity.ok(result);
    }

    // 로그아웃 — POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "ログアウト成功");
        return ResponseEntity.ok(result);
    }

    // 로그인 상태 확인 — GET /api/auth/me
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            result.put("success", false);
            result.put("message", "未ログイン");
            return ResponseEntity.ok(result);
        }

        MemberEntity 회원 = memberRepository.findById(memberId);
        if (회원 == null) {
            result.put("success", false);
            result.put("message", "会員情報が見つかりません");
            return ResponseEntity.ok(result);
        }

        result.put("success",       true);
        result.put("memberId",      회원.getId());
        result.put("email",         회원.getEmail());
        result.put("role",          회원.getRole());
        result.put("lastName",      회원.getLastName());
        result.put("firstName",     회원.getFirstName());
        result.put("lastNameKana",  회원.getLastNameKana());
        result.put("firstNameKana", 회원.getFirstNameKana());
        result.put("phone",         회원.getPhone());
        result.put("esgPoint",      회원.getEsgPoint());
        return ResponseEntity.ok(result);
    }

    // 회원 정보 수정 — PUT /api/auth/update
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@RequestBody UpdateRequest req,
                                                      HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            result.put("success", false);
            result.put("message", "未ログイン");
            return ResponseEntity.status(401).body(result);
        }

        MemberEntity 회원 = memberRepository.findById(memberId);
        if (회원 == null) {
            result.put("success", false);
            result.put("message", "会員情報が見つかりません");
            return ResponseEntity.badRequest().body(result);
        }

        // 비밀번호 변경 요청이 있는 경우 (currentPw 가 비어있으면 건너뜀)
        if (req.getCurrentPw() != null && !req.getCurrentPw().isEmpty()) {
            boolean 비번일치 = passwordEncoder.matches(req.getCurrentPw(), 회원.getPassword());
            if (!비번일치) {
                result.put("success", false);
                result.put("message", "現在のパスワードが正しくありません");
                return ResponseEntity.badRequest().body(result);
            }
            회원.setPassword(passwordEncoder.encode(req.getNewPw()));
            memberRepository.updatePassword(회원);
        }

        // 이름 · 후리가나 · 전화번호 UPDATE
        회원.setLastName(req.getLastName());
        회원.setFirstName(req.getFirstName());
        회원.setLastNameKana(req.getLastNameKana());
        회원.setFirstNameKana(req.getFirstNameKana());
        회원.setPhone(req.getPhone());

        try {
            memberRepository.update(회원);
        } catch (DuplicateKeyException e) {
            result.put("success", false);
            result.put("message", "この電話番号はすでに使用されています");
            return ResponseEntity.badRequest().body(result);
        }

        result.put("success", true);
        result.put("message", "情報が更新されました");
        return ResponseEntity.ok(result);
    }

    // 비밀번호 재설정 본인확인 — POST /api/auth/verify-identity
    @PostMapping("/verify-identity")
    public ResponseEntity<Map<String, Object>> verifyIdentity(@RequestBody ResetPasswordRequest req) {
        Map<String, Object> result = new HashMap<>();

        MemberEntity 회원 = memberRepository.findByEmail(req.getEmail());

        if (회원 == null
                || !회원.getLastName().equals(req.getLastName())
                || !회원.getFirstName().equals(req.getFirstName())
                || !회원.getPhone().equals(req.getPhone())) {
            result.put("success", false);
            result.put("message", "入力情報が一致しません");
            return ResponseEntity.ok(result);
        }

        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    // 비밀번호 재설정 — POST /api/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest req) {
        Map<String, Object> result = new HashMap<>();

        MemberEntity 회원 = memberRepository.findByEmail(req.getEmail());

        if (회원 == null
                || !회원.getLastName().equals(req.getLastName())
                || !회원.getFirstName().equals(req.getFirstName())
                || !회원.getPhone().equals(req.getPhone())) {
            result.put("success", false);
            result.put("message", "入力情報が一致しません");
            return ResponseEntity.ok(result);
        }

        회원.setPassword(passwordEncoder.encode(req.getNewPw()));
        memberRepository.updatePassword(회원);

        result.put("success", true);
        result.put("message", "パスワードが変更されました");
        return ResponseEntity.ok(result);
    }

    // 이메일 찾기 — POST /api/auth/find-email
    @PostMapping("/find-email")
    public ResponseEntity<Map<String, Object>> findEmail(@RequestBody FindEmailRequest req) {
        Map<String, Object> result = new HashMap<>();

        MemberEntity 검색용 = new MemberEntity();
        검색용.setLastName(req.getLastName());
        검색용.setFirstName(req.getFirstName());
        검색용.setPhone(req.getPhone());

        MemberEntity 회원 = memberRepository.findByNameAndPhone(검색용);

        if (회원 == null) {
            result.put("success", false);
            result.put("message", "一致する会員情報が見つかりません");
            return ResponseEntity.ok(result);
        }

        result.put("success", true);
        result.put("email", 회원.getEmail());
        return ResponseEntity.ok(result);
    }

    // 회원 탈퇴 — POST /api/auth/withdraw
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            result.put("success", false);
            result.put("message", "未ログイン");
            return ResponseEntity.status(401).body(result);
        }

        memberRepository.withdraw(memberId);
        session.invalidate();

        result.put("success", true);
        result.put("message", "退会が完了しました");
        return ResponseEntity.ok(result);
    }
}