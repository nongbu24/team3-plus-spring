package com.example.team3plusspring.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignupRequest {

    @NotBlank(message = "이메일 입력은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
    private String email;

    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다."
    )
    private String password;

    @NotBlank(message = "이름 입력은 필수입니다.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "전화번호 입력은 필수입니다.")
    @Pattern(
            regexp = "^01[016789]-\\d{3,4}-\\d{4}$",
            message = "전화번호 형식이 올바르지 않습니다."
    )
    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
    private String phone;
}
