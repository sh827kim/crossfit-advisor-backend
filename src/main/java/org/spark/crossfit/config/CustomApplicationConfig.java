package org.spark.crossfit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "custom.application")
public class CustomApplicationConfig {
    private String defaultLoginSuccessUrl;
    private String defaultLoginFailureUrl;
    private String defaultLogoutSuccessUrl;
    private List<String> origins;
    private Jwt jwt;
    private CookieOptions cookieOptions;
    private String googleApiKey;
    private List<String> allowedUsers;

    @Getter
    @Setter
    public static class Jwt {
        private String issuer;
        private String audience;
        private String keyId;
        private String privateKey;
        private String publicKey;
        private Long accessTokenTtl;
        private Long refreshTokenTtl;
        private String refreshTokenPurpose;
        private String accessTokenPurpose;
    }

    @Getter
    @Setter
    public static class CookieOptions {
        private boolean httpOnly;
        private boolean secure;
        private String sameSite;
        private String domain;
    }

}
