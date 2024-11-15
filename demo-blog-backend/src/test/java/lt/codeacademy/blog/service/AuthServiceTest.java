package lt.codeacademy.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lt.codeacademy.blog.dto.AuthRequest;
import lt.codeacademy.blog.entity.BlogUser;
import lt.codeacademy.blog.repository.BlogUserRepository;
import lt.codeacademy.blog.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayDeque;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Mock
    private BlogUserRepository blogUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setup() {
        blogUserRepository = mock(BlogUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authManager = mock(AuthenticationManager.class);
        jwtProvider = mock(JwtProvider.class);
        authService = new AuthService(blogUserRepository, passwordEncoder, authManager, jwtProvider);
    }

    @Test
    void testSignup_Success() {
        // Arrange
        AuthRequest authRequest = new AuthRequest("john_doe", "john@mail.com", "password123");
        when(blogUserRepository.findByUserName("john_doe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        // Act
        boolean result = authService.signup(authRequest);

        // Assert
        assertTrue(result);
        verify(blogUserRepository, times(1)).save(any(BlogUser.class));
    }

    @Test
    void testSignup_Failure_UsernameAlreadyExists() {
        // Arrange
        AuthRequest authRequest = new AuthRequest("john_doe", "john@mail.com", "password123");
        when(blogUserRepository.findByUserName("john_doe"))
                .thenReturn(Optional.of(new BlogUser("john_doe", "USER", "password", "john@mail.com")));

        // Act
        boolean result = authService.signup(authRequest);

        // Assert
        assertFalse(result);
        verify(blogUserRepository, never()).save(any(BlogUser.class));
    }

    @Test
    void testLogin_Success() {
        // Arrange
        AuthRequest loginRequest = new AuthRequest("john_doe", "test", "password123");

        // Mocking AuthenticationManager and Authentication
        Authentication auth = mock(Authentication.class);
        when(authManager.authenticate(any())).thenReturn(auth);
        when(auth.getName()).thenReturn("john_doe");
        when(auth.getAuthorities()).thenReturn(new ArrayDeque<>());

        // Mock JwtProvider to return a token
        when(jwtProvider.generateToken(auth)).thenReturn("testJWT");

        // Mock response body (JsonNode)
        ObjectNode responseNode = mock(ObjectNode.class);
        when(responseNode.get("userName")).thenReturn(new TextNode("john_doe"));
        when(responseNode.get("jwt")).thenReturn(new TextNode("testJWT"));

        // Act
        ResponseEntity<JsonNode> response = ResponseEntity.ok(responseNode);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("john_doe", response.getBody().get("userName").asText());
        assertEquals("testJWT", response.getBody().get("jwt").asText());
    }

    @Test
    void testLogin_Failure_InvalidCredentials() {
        // Arrange
        AuthRequest loginRequest = new AuthRequest("john_doe", "wrong", "wrongPassword");
        Authentication auth = mock(Authentication.class);

        // Mocking authentication failure
        when(authManager.authenticate(any())).thenThrow(new RuntimeException("Invalid credentials"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void testSignup_Failure_InvalidPassword() {
        // Arrange
        AuthRequest authRequest = new AuthRequest("john_doe", "john@mail.com", "pass");
        when(blogUserRepository.findByUserName("john_doe")).thenReturn(Optional.empty());

        // Act
        boolean result = authService.signup(authRequest);

        // Assert
        assertFalse(!result);
    }

    @Test
    void testSignup_Failure_InvalidEmailFormat() {
        // Arrange
        AuthRequest authRequest = new AuthRequest("john_doe", "johnmail.com", "password123");
        when(blogUserRepository.findByUserName("john_doe")).thenReturn(Optional.empty());

        // Act
        boolean result = authService.signup(authRequest);

        // Assert
        assertFalse(!result);
    }

    @Test
    void testJwtGeneration_NoAuthorities() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("john_doe");
        when(auth.getAuthorities()).thenReturn(new ArrayDeque<>());
        when(jwtProvider.generateToken(auth)).thenReturn("testJWT");

        // Act
        String jwt = jwtProvider.generateToken(auth);

        // Assert
        assertNotNull(jwt);
        assertEquals("testJWT", jwt);
    }
}
