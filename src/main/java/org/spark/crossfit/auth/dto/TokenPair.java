package org.spark.crossfit.auth.dto;

public record TokenPair(
        String accessToken,
        long accessTokenExpiresInSec,
        String refreshToken,
        long refreshTokenExpiresInSec
) {
}
