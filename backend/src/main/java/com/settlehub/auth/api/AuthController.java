package com.settlehub.auth.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(authService.me(authUser));
    }

    /**
     * MVP: 서버측 토큰 블랙리스트 없이 클라이언트 폐기 안내.
     * Redis 연동 시 무효화로 확장.
     */
    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout() {
        return ApiResponse.ok(Map.of("message", "Discard the access token on client"));
    }
}
