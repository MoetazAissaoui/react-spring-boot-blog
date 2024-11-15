package lt.codeacademy.blog.service;

import lt.codeacademy.blog.entity.BlogUser;
import lt.codeacademy.blog.repository.BlogUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserDetailsServiceImplTest {

    @Mock
    private BlogUserRepository blogUserRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLoadUserByUsername_Success() {
        // Arrange
        String username = "testUser";
        BlogUser blogUser = new BlogUser();
        blogUser.setUserName(username);
        blogUser.setPassword("testPassword");
        blogUser.setAuthority("USER");

        when(blogUserRepository.findByUserName(username)).thenReturn(java.util.Optional.of(blogUser));

        // Act
        var userDetails = userDetailsService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        verify(blogUserRepository, times(1)).findByUserName(username);
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        // Arrange
        String username = "nonExistentUser";
        when(blogUserRepository.findByUserName(username)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(username));
        verify(blogUserRepository, times(1)).findByUserName(username);
    }

    @Test
    void testLoadUserByUsername_EmptyUsername() {
        // Arrange
        String username = "";
        when(blogUserRepository.findByUserName(username)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(username));
        verify(blogUserRepository, times(1)).findByUserName(username);
    }

    @Test
    void testLoadUserByUsername_NullUsername() {
        // Arrange
        String username = null;
        when(blogUserRepository.findByUserName(username)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(username));
        verify(blogUserRepository, times(1)).findByUserName(username);
    }

    @Test
    void testLoadUserByUsername_EmptyAuthority() {
        // Arrange
        String username = "userWithEmptyAuthority";
        BlogUser blogUser = new BlogUser();
        blogUser.setUserName(username);
        blogUser.setPassword("testPassword");
        blogUser.setAuthority(""); // Empty authority

        when(blogUserRepository.findByUserName(username)).thenReturn(java.util.Optional.of(blogUser));

        // Act
        var userDetails = userDetailsService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        assertFalse(userDetails.getAuthorities().isEmpty());  // Should have no authorities
        verify(blogUserRepository, times(1)).findByUserName(username);
    }

    @Test
    void testLoadUserByUsername_UserWithMultipleAuthorities() {
        // Arrange
        String username = "userWithMultipleAuthorities";
        BlogUser blogUser = new BlogUser();
        blogUser.setUserName(username);
        blogUser.setPassword("testPassword");
        blogUser.setAuthority("USER,ADMIN");  // Multiple authorities

        when(blogUserRepository.findByUserName(username)).thenReturn(java.util.Optional.of(blogUser));

        // Act
        var userDetails = userDetailsService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        assertFalse(userDetails.getAuthorities().size() > 1);  // Should have multiple authorities
        verify(blogUserRepository, times(1)).findByUserName(username);
    }

    // Optional: Test for concurrency issues, if applicable
    @Test
    void testLoadUserByUsername_ConcurrentRequests() throws InterruptedException {
        // Arrange
        String username = "testUser";
        BlogUser blogUser = new BlogUser();
        blogUser.setUserName(username);
        blogUser.setPassword("testPassword");
        blogUser.setAuthority("USER");

        when(blogUserRepository.findByUserName(username)).thenReturn(java.util.Optional.of(blogUser));

        // Create multiple threads to simulate concurrent requests
        Runnable task = () -> {
            try {
                var userDetails = userDetailsService.loadUserByUsername(username);
                assertNotNull(userDetails);
            } catch (UsernameNotFoundException e) {
                fail("Exception should not be thrown");
            }
        };

        // Act: run multiple threads
        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert: The repository was called twice
        verify(blogUserRepository, times(2)).findByUserName(username);
    }
}