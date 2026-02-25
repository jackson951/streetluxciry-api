package com.jackson.demo.service;

import com.jackson.demo.dto.request.AuthLoginRequest;
import com.jackson.demo.dto.request.AuthRegisterRequest;
import com.jackson.demo.dto.request.RefreshTokenRequest;
import com.jackson.demo.dto.response.AuthTokenResponse;
import com.jackson.demo.dto.response.AuthUserResponse;
import com.jackson.demo.entity.AppUser;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.RefreshToken;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.model.UserRole;
import com.jackson.demo.repository.AppUserRepository;
import com.jackson.demo.repository.CartRepository;
import com.jackson.demo.repository.CustomerRepository;
import com.jackson.demo.repository.RefreshTokenRepository;
import com.jackson.demo.security.AuthenticatedUser;
import com.jackson.demo.security.JwtService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long refreshTokenExpirationDays;

    public AuthService(
            AppUserRepository appUserRepository,
            CustomerRepository customerRepository,
            CartRepository cartRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${app.jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays) {
        this.appUserRepository = appUserRepository;
        this.customerRepository = customerRepository;
        this.cartRepository = cartRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public AuthTokenResponse register(AuthRegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (appUserRepository.findByEmailIgnoreCase(email).isPresent()
                || customerRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BadRequestException("Email is already registered");
        }

        Customer customer = new Customer();
        customer.setFullName(request.fullName().trim());
        customer.setEmail(email);
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        customer = customerRepository.save(customer);

        Cart cart = new Cart();
        cart.setCustomer(customer);
        cartRepository.save(cart);

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.getRoles().add(UserRole.ROLE_CUSTOMER);
        user.setCustomer(customer);
        user = appUserRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Invalid email or password");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Refresh token is invalid"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token is expired or revoked");
        }

        AppUser user = refreshToken.getUser();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return issueTokens(user);
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public AuthUserResponse me(AuthenticatedUser principal) {
        if (principal == null) {
            throw new BadRequestException("Not authenticated");
        }
        AppUser user = appUserRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    private AuthTokenResponse issueTokens(AppUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("roles", user.getRoles().stream().map(Enum::name).toList());
        if (user.getCustomer() != null) {
            claims.put("customerId", user.getCustomer().getId());
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getCustomer() != null ? user.getCustomer().getId() : null,
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> (org.springframework.security.core.GrantedAuthority) () -> role.name())
                        .toList());

        String accessToken = jwtService.generateAccessToken(authenticatedUser, claims);
        String refreshTokenValue = UUID.randomUUID().toString() + UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpirationDays * 24 * 3600));
        refreshTokenRepository.save(refreshToken);

        return new AuthTokenResponse(
                "Bearer",
                accessToken,
                jwtService.getAccessTokenExpirationSeconds(),
                refreshTokenValue,
                toUserResponse(user));
    }

    private AuthUserResponse toUserResponse(AppUser user) {
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        Long customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getFullName(), roles, customerId);
    }
}
