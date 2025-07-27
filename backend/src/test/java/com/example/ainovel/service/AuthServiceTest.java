package com.example.ainovel.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.ainovel.dto.RegisterRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.utils.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void testRegisterUser_Success() {
        // Given
        when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = authService.register(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals(registerRequest.getUsername(), result.getUsername());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameAlreadyExists() {
        // Given
        when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.of(new User()));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(registerRequest);
        });
        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        // Given
        when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(new User()));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(registerRequest);
        });
        assertEquals("Email is already in use", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAuthenticate_Success() {
        // Given
        String username = "testuser";
        String password = "password123";
        String expectedToken = "jwt-token";
        
        Authentication mockAuth = mock(Authentication.class);
        UserDetails mockUserDetails = mock(UserDetails.class);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(mockUserDetails)).thenReturn(expectedToken);

        // When
        String result = authService.login(username, password);

        // Then
        assertEquals(expectedToken, result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(mockUserDetails);
    }

    @Test
    void testAuthenticate_InvalidCredentials() {
        // Given
        String username = "testuser";
        String password = "wrongpassword";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Authentication failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.login(username, password);
        });
        verify(jwtUtil, never()).generateToken(any(UserDetails.class));
    }
}
