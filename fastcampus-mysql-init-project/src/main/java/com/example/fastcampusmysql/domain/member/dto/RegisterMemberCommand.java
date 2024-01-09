package com.example.fastcampusmysql.domain.member.dto;

import java.time.LocalDate;

public record RegisterMemberCommand(
        // record - getter, setter가 자동으로 생성
        String email,
        String nickname,
        LocalDate birthday
) {
}
