package com.jackson.demo.controller;

import com.jackson.demo.dto.request.AuthLoginRequest;
import com.jackson.demo.dto.request.AuthRegisterRequest;
import com.jackson.demo.dto.request.ForgotPasswordRequest;
import com.jackson.demo.dto.request.RefreshTokenRequest;
import com.jackson.demo.dto.request.ResetPasswordRequest;
import com.jackson.demo.dto.request.VerifyOtpRequest;
import com.jackson.demo.dto.response.AuthTokenResponse;
import com.jackson.demo.dto.response.AuthUserResponse;
import com.jackson.demo.dto.response.OtpResponse;
import com.jackson.demo.security.AuthenticatedUser;
import com.jackson.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a customer account")
    @PostMapping("/register")
    public AuthTokenResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Forgot password - generate OTP")
    @PostMapping("/forgot-password")
    public OtpResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @Operation(summary = "Reset password with OTP")
    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }

    @Operation(summary = "Verify OTP")
    @PostMapping("/verify-otp")
    public OtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "Get authenticated user profile")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.me(user);
    }
}
