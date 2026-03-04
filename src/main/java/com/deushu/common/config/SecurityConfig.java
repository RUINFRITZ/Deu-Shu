//package com.deushu.common.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//   
//	@Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        return (web) -> web.ignoring().requestMatchers("/css/**", "/js/**", "/images/**");
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//            .csrf(csrf -> csrf.disable())
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/main", "/main/**", "/store/**").permitAll()
//                .requestMatchers(HttpMethod.GET, "/api/stores/**").permitAll()
//                .requestMatchers(HttpMethod.GET, "/api/reviews", "/api/reviews/**").permitAll()
//                .anyRequest().authenticated()
//            );
//
//        return http.build();
//    }
//}