package lt.codeacademy.blog.integration;

import lt.codeacademy.blog.entity.BlogUser;
import lt.codeacademy.blog.repository.BlogUserRepository;
import lt.codeacademy.blog.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")  // Use the test profile, which should use an in-memory database
@Transactional  // Rollback after each test
class UserDetailsServiceImplIntegrationTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private BlogUserRepository blogUserRepository;

    private BlogUser testUser;

    @BeforeEach
    void setUp() {
        // Initialize the test user and save it in the in-memory database
        testUser = new BlogUser();
        testUser.setUserName("testUser");
        testUser.setPassword("testPassword");
        testUser.setAuthority("USER");
        blogUserRepository.save(testUser);
    }

    @Test
    void testLoadUserByUsername_Success() {
        // Act
        var userDetails = userDetailsService.loadUserByUsername(testUser.getUserName());

        // Assert
        assertNotNull(userDetails);
        assertEquals(testUser.getUserName(), userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("nonExistentUser"));
    }

    @Test
    void testLoadUserByUsername_EmptyUsername() {
        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(""));
    }

    @Test
    void testLoadUserByUsername_UserWithMultipleAuthorities() {
        // Arrange: Create a new user with multiple roles
        BlogUser multipleRoleUser = new BlogUser();
        multipleRoleUser.setUserName("multiRoleUser");
        multipleRoleUser.setPassword("testPassword");
        multipleRoleUser.setAuthority("USER,ADMIN");
        blogUserRepository.save(multipleRoleUser);

        // Act
        var userDetails = userDetailsService.loadUserByUsername("multiRoleUser");

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().toString().split(",").length > 1);  // Multiple authorities should be assigned
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER,ADMIN")));
    }

    @Test
    void testLoadUserByUsername_UserWithNoAuthority() {
        // Arrange: Create a new user with no roles (empty authority)
        BlogUser noRoleUser = new BlogUser();
        noRoleUser.setUserName("noRoleUser");
        noRoleUser.setPassword("testPassword");
        noRoleUser.setAuthority(""); // No authority
        blogUserRepository.save(noRoleUser);

        // Act
        var userDetails = userDetailsService.loadUserByUsername("noRoleUser");

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().size() == 1);  // Should have no authorities
    }
}