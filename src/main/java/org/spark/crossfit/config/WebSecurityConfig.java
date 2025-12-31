package org.spark.crossfit.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.spark.crossfit.auth.JwtTokenProvider;
import org.spark.crossfit.auth.dto.AuthDetails;
import org.spark.crossfit.auth.filter.RestAccessDeniedHandler;
import org.spark.crossfit.auth.filter.RestAuthenticationEntryPoint;
import org.spark.crossfit.auth.repository.RefreshTokenStore;
import org.spark.crossfit.auth.filter.JwtAuthenticationFilter;
import org.spark.crossfit.auth.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final CustomApplicationConfig customApplicationConfig;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            AuthenticationEntryPoint restAuthenticationEntryPoint
    ) {
        return new JwtAuthenticationFilter(jwtTokenProvider, restAuthenticationEntryPoint);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           AuthenticationEntryPoint restAuthenticationEntryPoint,
                                           AccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {
        http
                // 1. 프론트(Next.js)와의 통신을 위한 CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. REST API이므로 CSRF는 일단 비활성화 (운영시에는 Next.js와 맞게 설정 필요)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
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
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 4. 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {

                            String userId = authentication.getPrincipal().toString();
                            refreshTokenStore.revokeAllForUser(userId);

                            response.setStatus(302);
                            response.setHeader("Location", customApplicationConfig.getDefaultLogoutSuccessUrl());
                        })
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }
    @Bean
    public AuthenticationSuccessHandler oAuth2CookieRedirectSuccessHandler() {
        return (request, response, authentication) -> {
            // 1. 유저 정보 추출
            AuthDetails oAuth2User = (AuthDetails) authentication.getPrincipal();
            String userId = oAuth2User.getUsername();

            // 2. RefreshToken 및 SID 생성
            String sid = java.util.UUID.randomUUID().toString();
            String refresh = jwtTokenProvider.issueRefreshToken(userId, sid);

            var refreshClaims = jwtTokenProvider.validateAndGetClaims(refresh);
            refreshTokenStore.save(userId, sid, refreshClaims.getId(), refreshClaims.getExpiration().toInstant());

            String targetUrl = UriComponentsBuilder.fromUriString(customApplicationConfig.getDefaultLoginSuccessUrl())
                    .queryParam("refreshToken", refresh)
                    .build().toUriString();

            // 5. 리다이렉트 실행
            response.setStatus(302);
            response.setHeader("Location", targetUrl);
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
        configuration.addExposedHeader("Set-Cookie");
        configuration.addExposedHeader("Location");

        configuration.setAllowCredentials(true); // 쿠키/세션 주고받기 위해 필수

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
