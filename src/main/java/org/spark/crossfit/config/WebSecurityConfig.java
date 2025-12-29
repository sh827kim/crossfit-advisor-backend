package org.spark.crossfit.config;


import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.spark.crossfit.auth.JwtTokenProvider;
import org.spark.crossfit.auth.dto.AuthDetails;
import org.spark.crossfit.auth.repository.RefreshTokenStore;
import org.spark.crossfit.auth.filter.JwtAuthenticationFilter;
import org.spark.crossfit.auth.service.CustomOAuth2UserService;
import org.spark.crossfit.util.HttpServletUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.spark.crossfit.constants.CommonConstants.REFRESH_TOKEN_COOKIE_NAME;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final CustomApplicationConfig customApplicationConfig;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 프론트(Next.js)와의 통신을 위한 CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. REST API이므로 CSRF는 일단 비활성화 (운영시에는 Next.js와 맞게 설정 필요)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/auth-token/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 3. OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // 구글에서 받은 정보로 후처리(DB저장 등) 진행
                        )
                        .successHandler(oAuth2CookieRedirectSuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(302);
                            response.setHeader("Location", customApplicationConfig.getDefaultLoginFailureUrl());
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 4. 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String refreshToken = null;
                            if (request.getCookies() != null) {
                                refreshToken = HttpServletUtil.getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
                            }
                            if(refreshToken != null) {
                                try {
                                    String userId = jwtTokenProvider.getSubject(refreshToken);
                                    // RefreshTokenStore에서 삭제
                                    refreshTokenStore.revokeAllForUser(userId);
                                } catch (Exception e) {
                                    // 토큰이 유효하지 않은 경우 무시
                                }
                            }

                            // refresh 쿠키 삭제 + 프론트 redirect
                            ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                                    .httpOnly(true)
                                    .secure(true)
                                    .sameSite("None")
                                    .path("/")
                                    .maxAge(0)
                                    .build();


                            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                            response.setStatus(302);
                            response.setHeader("Location", customApplicationConfig.getDefaultLogoutSuccessUrl());
                        })
                        .deleteCookies("JSESSIONID") // 사실 STATELESS면 거의 안 생기지만, 혹시 몰라 유지
                );

        return http.build();
    }
    @Bean
    public AuthenticationSuccessHandler oAuth2CookieRedirectSuccessHandler() {
        return (request, response, authentication) -> {
            // customOAuth2UserService에서 principal에 내부 userId 같은 걸 담아두는 걸 권장
            // 여기서는 예시로 꺼내는 부분만 표시
            AuthDetails oAuth2User = (AuthDetails) authentication.getPrincipal();
            String userId = oAuth2User.getUsername();


            // sid는 최초 로그인 시 서버에서 생성 (토큰 패밀리)
            String sid = java.util.UUID.randomUUID().toString();

            String refresh = jwtTokenProvider.issueRefreshToken(userId, sid);

            var refreshClaims = jwtTokenProvider.validateAndGetClaims(refresh);
            refreshTokenStore.save(userId, sid, refreshClaims.getId(), refreshClaims.getExpiration().toInstant());

            // refresh 쿠키 set
            ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refresh)
                    .httpOnly(true)
                    .secure(true)               // Railway + HTTPS 기준
                    .sameSite("None")           // 프론트/백 분리
                    .path("/")
                    .maxAge(Duration.ofDays(customApplicationConfig.getJwt().getRefreshTokenTtl()).toSeconds())
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // 프론트로 redirect (토큰 싣지 않음)
            response.setStatus(302);
            response.setHeader("Location", customApplicationConfig.getDefaultLoginSuccessUrl());
        };
    }


    // Next.js (Port 3000)로부터의 요청을 허용하는 CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        customApplicationConfig.getOrigins().forEach(configuration::addAllowedOrigin);
        configuration.addAllowedHeader("*"); //모든 Header 허용

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.addExposedHeader("Authorization"); // 클라이언트가 특정 헤더값에 접근 가능하도록 하기

        configuration.setAllowCredentials(true); // 쿠키/세션 주고받기 위해 필수

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
