package com.example.team3plusspring.domain.user.dto;

import com.example.team3plusspring.domain.user.entity.User;
import lombok.Getter;

@Getter
public class UserMeResponse {

    private final Long userId;
    private final String email;
    private final String name;
    private final String phone;
    private final String role;
    private final Long pointBalance;

    private UserMeResponse(User user, Long pointBalance) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.phone = user.getPhone();
        this.role = user.getRole().name();
        this.pointBalance = pointBalance;
    }

    public static UserMeResponse from(User user, Long pointBalance) {
        return new UserMeResponse(user, pointBalance);
    }
}
