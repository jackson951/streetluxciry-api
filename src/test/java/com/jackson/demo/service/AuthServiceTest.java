package com.jackson.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.jackson.demo.dto.request.AuthLoginRequest;
import com.jackson.demo.entity.AppUser;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.model.UserRole;
import com.jackson.demo.repository.AppUserRepository;
import com.jackson.demo.repository.CartRepository;
import com.jackson.demo.repository.CustomerRepository;
import com.jackson.demo.repository.RefreshTokenRepository;
import com.jackson.demo.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @SuppressWarnings("null")
@Test
    void loginForAdminWithoutCustomerGeneratesTokenWithoutCustomerClaim() {
        AuthService authService = new AuthService(
                appUserRepository,
                customerRepository,
                cartRepository,
                refreshTokenRepository,
                passwordEncoder,
                authenticationManager,
                new JwtService("bnlMdkdzbnN4Y3B2eTQ5d2VoM3dwY2VmMXVjaXJiYmxzNTRmYmhoN2x1dXFxZWY5ZA==", 30),
                14);

        AppUser admin = new AppUser();
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        admin.setEmail("admin@shop.local");
        admin.setFullName("Admin");
        admin.setPasswordHash("hashed");
        admin.getRoles().add(UserRole.ROLE_ADMIN);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("admin@shop.local", "x"));
        when(appUserRepository.findByEmailIgnoreCase("admin@shop.local")).thenReturn(Optional.of(admin));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.login(new AuthLoginRequest("ADMIN@shop.local", "Admin@12345"));

        assertEquals("Bearer", response.tokenType());
        assertFalse(response.accessToken().isBlank());
        assertEquals("admin@shop.local", response.user().email());
        assertEquals(null, response.user().customerId());
    }

    @Test
    void loginThrowsBadRequestForInvalidCredentials() {
        AuthService authService = new AuthService(
                appUserRepository,
                customerRepository,
                cartRepository,
                refreshTokenRepository,
                passwordEncoder,
                authenticationManager,
                new JwtService("bnlMdkdzbnN4Y3B2eTQ5d2VoM3dwY2VmMXVjaXJiYmxzNTRmYmhoN2x1dXFxZWY5ZA==", 30),
                14);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad creds"));

        assertThrows(BadRequestException.class, () -> authService.login(new AuthLoginRequest("x@shop.local", "bad")));
    }
}
