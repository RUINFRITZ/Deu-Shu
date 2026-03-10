// =========================================================================
// [ ドゥーシュー ] ブランチ名: feature/security-integration
// 認証モダールが全ページのヘッダーに埋め込まれているSPA構造に最適化したルーティング。
// Bootstrapや画像などの静的リソースを完全開放し、APIのGET/POSTを厳密に分離します。
// =========================================================================

package com.deushu.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /*
     * AuthControllerで使用するパスワード暗号化エンコーダー
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            
            // エンドポイント別のアクセス権限制御
            .authorizeHttpRequests(auth -> auth
                
                // -----------------------------------------------------------------
                // [1] 静的リソース (CSS, JS, Images, Bootstrap 等) の完全開放
                // -----------------------------------------------------------------
                // ユさんのBootstrapデザインや、AWS S3のリンク等がブロックされないようにする
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()

                // -----------------------------------------------------------------
                // [2] フロントエンドのHTML画面遷移 (誰でもアクセス可能)
                // -----------------------------------------------------------------
                // IndexController: メイン画面 (/), リスト画面 (/list)
                .requestMatchers("/", "/list").permitAll()
                // StoreController: 店舗詳細画面 (/store/{id})
                .requestMatchers("/store/**").permitAll()

                // -----------------------------------------------------------------
                // [3] Auth(認証)ドメイン - モダールからAjaxで叩かれるAPI
                // -----------------------------------------------------------------
                // ベさんのログイン、会員登録、パスワード探しAPIは常に開けておく
                .requestMatchers("/api/auth/**").permitAll()

                // -----------------------------------------------------------------
                // [4] Store(店舗)ドメイン
                // -----------------------------------------------------------------
                // 管理者専用のキャッシュ無効化API
                .requestMatchers(HttpMethod.POST, "/api/stores/admin/cache/evict").hasRole("ADMIN")
                // お気に入りトグルはログイン済みのユーザーまたはオーナーのみ
                .requestMatchers(HttpMethod.POST, "/api/stores/*/favorite/toggle").hasAnyRole("USER", "OWNER")
                // その他のマップ用ピン取得、リスト取得などの照会(GET)は全て開放
                .requestMatchers("/api/stores/**").permitAll()

                // -----------------------------------------------------------------
                // [5] Review(レビュー)ドメイン
                // -----------------------------------------------------------------
                // レビューの照会(GET)は非ログインでも可能
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                // レビューの作成(POST)はログイン済みのユーザー(ROLE_USER)のみ
                .requestMatchers(HttpMethod.POST, "/api/reviews/**").hasRole("USER")

                // -----------------------------------------------------------------
                // [6] マイページおよび保護領域 (ログイン必須)
                // -----------------------------------------------------------------
                // マイページ画面 (IndexControllerの @GetMapping("/mypage") と対応)
                .requestMatchers("/mypage").authenticated()
                
                // [ パクさん領域 ] 今後実装する注文・決済API
                .requestMatchers("/api/v1/orders/**").hasRole("USER")
                .requestMatchers("/api/v1/owner/**").hasAnyRole("OWNER", "ADMIN")

                // 上記以外のすべてのアプローチは認証(ログイン)を要求して安全に遮断
                .anyRequest().authenticated()
            );

        return http.build();
    }
}