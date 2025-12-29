package org.spark.crossfit.auth.repository;

import java.time.Instant;

public interface RefreshTokenStore {
    // 발급: sid + jti 저장 (TTL 포함)
    void save(String userId, String sessionId, String jti, Instant expiresAt);

    // 검증: sid + jti 존재 여부
    boolean exists(String sessionId, String jti);

    // 회전: 기존 jti 폐기
    void revoke(String sessionId, String jti);

    // 재사용 탐지(선택): 폐기된 jti를 짧게 기록해두기
    void markRevoked(String sessionId, String jti, Instant expiresAt);
    boolean isRevoked(String sessionId, String jti);

    // 세션 전체 로그아웃
    void revokeSession(String sessionId);

    // 전체 로그아웃(선택)
    void revokeAllForUser(String userId);
}
