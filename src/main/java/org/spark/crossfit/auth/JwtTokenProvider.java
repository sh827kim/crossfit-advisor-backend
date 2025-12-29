package org.spark.crossfit.auth;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.spark.crossfit.auth.dto.TokenPair;
import org.spark.crossfit.config.CustomApplicationConfig;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final CustomApplicationConfig config;
    private final PrivateKey signingKey;  // private
    private final PublicKey verifyKey;    // public

    public JwtTokenProvider(CustomApplicationConfig config) {
        this.config = config;

        this.signingKey = loadRsaPrivateKey(config.getJwt().getPrivateKey());
        this.verifyKey  = loadRsaPublicKey(config.getJwt().getPublicKey());
    }

    public TokenPair issueTokenPair(String userId) {
        Instant now = Instant.now();

        String access = issueAccessToken(userId, now);
        String refresh = issueRefreshToken(userId, now, UUID.randomUUID().toString());

        return new TokenPair(
                access,
                Duration.ofSeconds(config.getJwt().getAccessTokenTtl()).toSeconds(),
                refresh,
                Duration.ofDays(config.getJwt().getRefreshTokenTtl()).toSeconds()
        );
    }


    public String issueAccessToken(String userId) {
        return issueAccessToken(userId, Instant.now());
    }

    public String issueAccessToken(String userId, Instant now) {
        Instant exp = now.plus(Duration.ofSeconds(config.getJwt().getAccessTokenTtl()));

        return Jwts.builder()
                .header().keyId(config.getJwt().getKeyId()).and()
                .issuer(config.getJwt().getIssuer())
                .audience().add(config.getJwt().getAudience()).and()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("purpose", config.getJwt().getAccessTokenPurpose())
                .claim("roles", List.of("USER"))
                .signWith(signingKey, Jwts.SIG.RS256)
                .compact();
    }

    public String issueRefreshToken(String userId, String sid) {
        return issueRefreshToken(userId, Instant.now(), sid);
    }

    private String issueRefreshToken(String userId, Instant now, String sid) {
        Instant exp = now.plus(Duration.ofDays(config.getJwt().getRefreshTokenTtl()));
        String refreshJti = UUID.randomUUID().toString();

        return Jwts.builder()
                .header().keyId(config.getJwt().getKeyId()).and()
                .issuer(config.getJwt().getIssuer())
                .audience().add(config.getJwt().getAudience()).and()
                .subject(userId)
                .id(refreshJti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("purpose", config.getJwt().getRefreshTokenPurpose())
                .claim("sid", sid)
                .signWith(signingKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims validateAndGetClaims(String token) {
        return parseAndValidate(token).getPayload();
    }

    public String getSubject(String token) {
        return parseAndValidate(token).getPayload().getSubject();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(verifyKey)
                .requireIssuer(config.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token);
    }

    public boolean isRefreshToken(Claims claims) {
        Object p = claims.get("purpose");
        return config.getJwt().getRefreshTokenPurpose().equals(p);
    }

    public boolean isAccessToken(Claims claims) {
        Object p = claims.get("purpose");
        return config.getJwt().getAccessTokenPurpose().equals(p);
    }

    private static PrivateKey loadRsaPrivateKey(String pem) {
        try {
            String normalized = normalizePem(pem);
            String b64 = stripPem(normalized,
                    "-----BEGIN PRIVATE KEY-----",
                    "-----END PRIVATE KEY-----");

            byte[] der = Base64.getDecoder().decode(b64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key from PEM string", e);
        }
    }


    public static PublicKey loadRsaPublicKey(String pem) {
        try {
            String normalized = normalizePem(pem);
            String b64 = stripPem(normalized,
                    "-----BEGIN PUBLIC KEY-----",
                    "-----END PUBLIC KEY-----");

            byte[] der = Base64.getDecoder().decode(b64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key from PEM string", e);
        }
    }

    private static String normalizePem(String pem) {
        // Railway ENV에 \n 형태로 들어오는 케이스 대비
        String s = pem.replace("\\n", "\n");

        // 혹시 CRLF가 섞였을 때 제거
        s = s.replace("\r\n", "\n");

        // 앞뒤 공백 제거
        return s.trim();
    }

    private static String stripPem(String pem, String begin, String end) {
        String s = pem.replace(begin, "")
                .replace(end, "")
                .replaceAll("\\s", ""); // 모든 공백/개행 제거
        return s.getBytes(StandardCharsets.UTF_8).length == 0 ? "" : s;
    }

}
