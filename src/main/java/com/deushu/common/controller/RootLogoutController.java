// =========================================================================
// [ ドゥーシュー ] 2. RootLogoutController.java (新規作成 または IndexController に追加)
// ユーザーが 'http://127.0.0.1:8888/logout' (ルートパス) を直接叩いた場合の対応
// =========================================================================

package com.deushu.common.controller;

import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class RootLogoutController {

    // ルートパスへの GET リクエストをキャッチしてリダイレクト
    @GetMapping("/logout")
    public void rootLogoutGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        SecurityContextHolder.clearContext();
        
        // メインページへリダイレクト
        response.sendRedirect("/");
    }
}