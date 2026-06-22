package com.example.team3plusspring.domain.auth.service;

import com.example.team3plusspring.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PointService pointService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone()
        );

        User savedUser = userRepository.save(user);

        pointService.grantSignupBonus(savedUser);

        cartRepository.save(new Cart(savedUser));

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getPhone(),
                savedUser.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(this::invalidLoginCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidLoginCredentials();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        return new LoginResponse(
                "Bearer",
                accessToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds(),
                new LoginResponse.UserSummary(
                        user.getId(),
                        user.getEmail(),
                        user.getName()
                )
        );
    }

    public LogoutResponse logout() {
        return new LogoutResponse(true);
    }

    private BusinessException invalidLoginCredentials() {
        return new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }
}
