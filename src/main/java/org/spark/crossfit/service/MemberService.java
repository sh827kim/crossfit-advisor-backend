package org.spark.crossfit.service;

import lombok.RequiredArgsConstructor;
import org.spark.crossfit.domain.Member;
import org.spark.crossfit.dto.UnitType;
import org.spark.crossfit.repository.MemberRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    public void registerMember(String email, String password, String nickname) {
        var newMember = Member.builder()
                .nickname(nickname != null ? nickname : "User" + System.currentTimeMillis())
                .email(email)
                .uid(bCryptPasswordEncoder.encode(password))
                .unitType(UnitType.LB)
                .build();

        memberRepository.save(newMember);
    }

    public void updateMember(Member member) {
        memberRepository.save(member);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findMemberByEmail(email);
    }

}
