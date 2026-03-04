package com.deushu.member.controller;

import com.deushu.member.domain.MemberEntity;
import com.deushu.member.domain.MemberEntity.LoginRequest;
import com.deushu.member.mapper.MemberRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    // 요청 DTO
    // =====================================================================

    // 회원가입 요청 데이터
    public static class SignupRequest {
        private String email;
        private String password;
        private String lastName;
        private String firstName;
        private String lastNameKana;
        private String firstNameKana;
        private String phone;

        public String getEmail()                              { return email; }
        public void   setEmail(String email)                  { this.email = email; }
        public String getPassword()                           { return password; }
        public void   setPassword(String password)            { this.password = password; }
        public String getLastName()                           { return lastName; }
        public void   setLastName(String lastName)            { this.lastName = lastName; }
        public String getFirstName()                          { return firstName; }
        public void   setFirstName(String firstName)          { this.firstName = firstName; }
        public String getLastNameKana()                       { return lastNameKana; }
        public void   setLastNameKana(String lastNameKana)    { this.lastNameKana = lastNameKana; }
        public String getFirstNameKana()                      { return firstNameKana; }
        public void   setFirstNameKana(String firstNameKana)  { this.firstNameKana = firstNameKana; }
        public String getPhone()                              { return phone; }
        public void   setPhone(String phone)                  { this.phone = phone; }
    }

    // 정보 수정 요청 데이터 (currentPw / newPw 는 비밀번호 변경 시에만 입력)
    public static class UpdateRequest {
        private String lastName;
        private String firstName;
        private String lastNameKana;
        private String firstNameKana;
        private String phone;
        private String currentPw;
        private String newPw;

        public String getLastName()                           { return lastName; }
        public void   setLastName(String lastName)            { this.lastName = lastName; }
        public String getFirstName()                          { return firstName; }
        public void   setFirstName(String firstName)          { this.firstName = firstName; }
        public String getLastNameKana()                       { return lastNameKana; }
        public void   setLastNameKana(String lastNameKana)    { this.lastNameKana = lastNameKana; }
        public String getFirstNameKana()                      { return firstNameKana; }
        public void   setFirstNameKana(String firstNameKana)  { this.firstNameKana = firstNameKana; }
        public String getPhone()                              { return phone; }
        public void   setPhone(String phone)                  { this.phone = phone; }
        public String getCurrentPw()                          { return currentPw; }
        public void   setCurrentPw(String currentPw)          { this.currentPw = currentPw; }
        public String getNewPw()                              { return newPw; }
        public void   setNewPw(String newPw)                  { this.newPw = newPw; }
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
        새회원.setPassword(passwordEncoder.encode(req.getPassword())); // 비밀번호 BCrypt 암호화
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

        // 이메일로 회원 조회
        MemberEntity 회원 = memberRepository.findByEmail(req.getEmail());

        // 회원 존재 여부 및 비밀번호 검증
        if (회원 == null || !passwordEncoder.matches(req.getPassword(), 회원.getPassword())) {
            result.put("success", false);
            result.put("message", "メールアドレスまたはパスワードが正しくありません");
            return ResponseEntity.badRequest().body(result);
        }

        // ROLE_USER 여부 확인
        if (!"ROLE_USER".equals(회원.getRole())) {
            result.put("success", false);
            result.put("message", "一般会員アカウントではありません");
            return ResponseEntity.badRequest().body(result);
        }

        // 세션에 memberId 저장
        session.setAttribute("memberId", 회원.getId());

        result.put("success", true);
        result.put("message", "ログイン成功");
        return ResponseEntity.ok(result);
    }

    // 로그아웃 — POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {

        // 세션 전체 무효화
        session.invalidate();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "ログアウト成功");
        return ResponseEntity.ok(result);
    }

    // 로그인 상태 확인 및 회원 정보 반환 — GET /api/auth/me
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 세션에서 memberId 조회
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            result.put("success", false);
            result.put("message", "未ログイン");
            return ResponseEntity.ok(result);
        }

        // DB에서 최신 회원 정보 조회
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

        // 로그인 확인
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            result.put("success", false);
            result.put("message", "未ログイン");
            return ResponseEntity.status(401).body(result);
        }

        // DB에서 회원 정보 조회
        MemberEntity 회원 = memberRepository.findById(memberId);
        if (회원 == null) {
            result.put("success", false);
            result.put("message", "会員情報が見つかりません");
            return ResponseEntity.badRequest().body(result);
        }

        // 비밀번호 변경 요청이 있는 경우 (currentPw 가 비어있으면 이 블록 건너뜀)
        if (req.getCurrentPw() != null && !req.getCurrentPw().isEmpty()) {

            // 현재 비밀번호 일치 여부 확인
            boolean 비번일치 = passwordEncoder.matches(req.getCurrentPw(), 회원.getPassword());
            if (!비번일치) {
                result.put("success", false);
                result.put("message", "現在のパスワードが正しくありません");
                return ResponseEntity.badRequest().body(result);
            }

            // 새 비밀번호 암호화 후 UPDATE
            회원.setPassword(passwordEncoder.encode(req.getNewPw()));
            memberRepository.updatePassword(회원);
        }

        // 이름 · 후리가나 · 전화번호 UPDATE
        회원.setLastName(req.getLastName());
        회원.setFirstName(req.getFirstName());
        회원.setLastNameKana(req.getLastNameKana());
        회원.setFirstNameKana(req.getFirstNameKana());
        회원.setPhone(req.getPhone());

        // 전화번호 중복 시 에러 처리
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

    // 회원 탈퇴 — POST /api/auth/withdraw
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 로그인 확인
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            result.put("success", false);
            result.put("message", "未ログイン");
            return ResponseEntity.status(401).body(result);
        }

        // 논리삭제 (deleted_at = 현재시각)
        memberRepository.withdraw(memberId);

        // 세션 무효화
        session.invalidate();

        result.put("success", true);
        result.put("message", "退会が完了しました");
        return ResponseEntity.ok(result);
    }
}