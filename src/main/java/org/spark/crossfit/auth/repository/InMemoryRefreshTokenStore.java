package org.spark.crossfit.auth.repository;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private record TokenKey(String sessionId, String jti) {}
    private record TokenValue(String userId, Instant expiresAt) {}

    // 유효 토큰(화이트리스트)
    private final ConcurrentMap<TokenKey, TokenValue> valid = new ConcurrentHashMap<>();

    // 폐기 토큰(재사용 탐지용)
    private final ConcurrentMap<TokenKey, Instant> revoked = new ConcurrentHashMap<>();

    // userId -> sessionIds
    private final ConcurrentMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    // sessionId별 동기화 (회전 경쟁 방지)
    private final ConcurrentMap<String, Object> sessionLocks = new ConcurrentHashMap<>();


    @Override
    public void save(String userId, String sessionId, String jti, Instant expiresAt) {
        TokenKey key = new TokenKey(sessionId, jti);
        valid.put(key, new TokenValue(userId, expiresAt));

        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionLocks.computeIfAbsent(sessionId, k -> new Object());
    }

    @Override
    public boolean exists(String sessionId, String jti) {
        TokenValue v = valid.get(new TokenKey(sessionId, jti));
        if (v == null) return false;
        if (v.expiresAt().isBefore(Instant.now())) {
            // 만료된 건 즉시 제거
            valid.remove(new TokenKey(sessionId, jti));
            return false;
        }
        return true;

    }

    @Override
    public void revoke(String sessionId, String jti) {
        valid.remove(new TokenKey(sessionId, jti));
    }

    @Override
    public void markRevoked(String sessionId, String jti, Instant expiresAt) {
        revoked.put(new TokenKey(sessionId, jti), expiresAt);
    }

    @Override
    public boolean isRevoked(String sessionId, String jti) {
        Instant exp = revoked.get(new TokenKey(sessionId, jti));
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) {
            revoked.remove(new TokenKey(sessionId, jti));
            return false;
        }
        return true;
    }

    @Override
    public void revokeSession(String sessionId) {
        // sessionId에 속한 모든 valid/revoked 제거
        valid.keySet().removeIf(k -> k.sessionId().equals(sessionId));
        revoked.keySet().removeIf(k -> k.sessionId().equals(sessionId));

        // userSessions에서 sessionId 제거
        userSessions.forEach((userId, sessions) -> sessions.remove(sessionId));
        sessionLocks.remove(sessionId);
    }

    @Override
    public void revokeAllForUser(String userId) {
        Set<String> sessions = userSessions.getOrDefault(userId, Set.of());
        for (String sid : sessions) {
            revokeSession(sid);
        }
        userSessions.remove(userId);
    }


}
