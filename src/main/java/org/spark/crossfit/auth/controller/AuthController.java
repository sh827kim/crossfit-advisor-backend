package org.spark.crossfit.auth.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.spark.crossfit.auth.dto.RefreshToken;
import org.spark.crossfit.auth.dto.TokenPair;
import org.spark.crossfit.auth.service.AuthService;
import org.spark.crossfit.dto.CommonResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.spark.crossfit.constants.CommonConstants.REFRESH_TOKEN_COOKIE_NAME;
import static org.spark.crossfit.util.HttpServletUtil.getCookieValue;

@RestController
@RequestMapping("/auth-token")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    @PostMapping("/refresh")
    public CommonResult<TokenPair> refreshToken(@RequestBody RefreshToken refreshToken, HttpServletRequest request) {

        String refreshTokenValue = Optional.ofNullable(refreshToken)
                .map(RefreshToken::refreshToken) // DTO에서 값 추출 (메서드명에 맞게 수정 필요)
                .filter(val -> !val.isEmpty())           // 값이 비어있지 않은지 확인
                .orElseGet(() -> getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME));

        if(refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        var result = authService.refreshToken(refreshTokenValue);

        return CommonResult.success(result);
    }
}
