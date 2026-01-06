package org.spark.crossfit.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spark.crossfit.auth.dto.AuthDetails;
import org.spark.crossfit.config.CustomApplicationConfig;
import org.spark.crossfit.exception.UnauthorizedException;
import org.spark.crossfit.service.MemberService;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberService memberService;
    private final Environment env;
    private final CustomApplicationConfig customApplicationConfig;

    private boolean isTest() {
        return Arrays.stream(env.getActiveProfiles()).noneMatch(s -> s.equals("prd"));
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String uid = attributes.get("sub") != null ? attributes.get("sub").toString() :
                attributes.get("id") != null ? attributes.get("id").toString() :
                        null;

        String email = attributes.get("email") != null ? attributes.get("email").toString() : null;

        String nickname = attributes.get("name") != null ? attributes.get("name").toString() : null;

        if ( isTest() && !customApplicationConfig.getAllowedUsers().contains(email)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user"), "허용되지 않은 테스트 계정입니다");
        }

        if (email != null) {
            var memberOpt = memberService.findByEmail(email);
            if (memberOpt.isEmpty()) {
                // 신규 회원 가입 처리
                memberService.registerMember(email, uid, nickname);
                log.info("New member registered: {}", uid);
            } else {
                log.info("Existing member logged in: {}", uid);
            }

        } else {
            log.warn("Email not found in OAuth2 user attributes.");
        }
        var member = memberService.findByEmail(email).orElseThrow(() -> new OAuth2AuthenticationException("Member not found after registration"));

        return new AuthDetails(member, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));

    }
}
