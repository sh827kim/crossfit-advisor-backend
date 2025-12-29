package org.spark.crossfit.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.spark.crossfit.auth.JwtTokenProvider;
import org.spark.crossfit.auth.repository.RefreshTokenStore;
import org.spark.crossfit.auth.dto.TokenPair;
import org.spark.crossfit.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore store;

    public TokenPair refreshToken(String refreshToken) {
        Claims claims = jwtTokenProvider.validateAndGetClaims(refreshToken);


        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new UnauthorizedException("Refresh token required");
        }

        String userId = claims.getSubject();
        String jti = claims.getId();
        String sid = claims.get("sid", String.class);

        if (store.isRevoked(sid, jti)) {
            store.revokeSession(sid);
            throw new UnauthorizedException("Refresh token replay detected");
        }

        // 2) 화이트리스트 확인
        if (!store.exists(sid, jti)) {
            throw new UnauthorizedException("Refresh token not recognized");
        }

        store.revoke(sid, jti);
        store.markRevoked(sid, jti, jwtTokenProvider.validateAndGetClaims(refreshToken).getExpiration().toInstant());


        TokenPair tokenPair = jwtTokenProvider.issueTokenPair(userId);

        Claims newRefreshClaims = jwtTokenProvider.validateAndGetClaims(tokenPair.refreshToken());
        store.save(userId, sid, newRefreshClaims.getId(), newRefreshClaims.getExpiration().toInstant());

        return tokenPair;
    }
}
