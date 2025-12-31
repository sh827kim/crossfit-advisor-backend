package org.spark.crossfit.auth.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.spark.crossfit.auth.JwtTokenProvider;
import org.spark.crossfit.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.info("JwtAuthenticationFilter called for URI: {}", request.getRequestURI());
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring("Bearer ".length()).trim();

        try {
            Claims claims = jwtTokenProvider.validateAndGetClaims(token);

            String userId = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            var authorities = (roles == null ? List.<SimpleGrantedAuthority>of()
                    : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());

            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 1) 인증 정보는 확실히 제거
            SecurityContextHolder.clearContext();

            // 2) 이미 응답이 커밋된 경우면 더 건드리면 또 터짐 -> 로그만 남기고 종료
            if (response.isCommitted()) {
                log.warn("JWT auth failed but response already committed. uri={}", request.getRequestURI(), e);
                return;
            }

            // 3) Security 표준 라인(EntryPoint)로 401 응답 생성 위임
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid or expired JWT", e)
            );// 토큰 에러 응답 전략은 프로젝트 표준 에러 포맷에 맞춰서 처리 권장
            throw new UnauthorizedException("Invalid or expired JWT token");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // 필터를 타지 않게 할 경로들을 등록
        return path.startsWith("/auth-token/") ||
                path.startsWith("/login/") ||
                path.startsWith("/oauth2/");
    }
}
