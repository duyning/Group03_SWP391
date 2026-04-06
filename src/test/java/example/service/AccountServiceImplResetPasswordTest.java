package example.service;

import example.entity.Account;
import example.entity.PasswordResetToken;
import example.repository.AccountRepository;
import example.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplResetPasswordTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockAccount = new Account();
        mockAccount.setAccountID(1);
        mockAccount.setEmail("user@example.com");
        mockAccount.setPassword("oldpassword");
    }

    // ============================================================
    //   initiatePasswordReset()
    // ============================================================

    @Test
    @DisplayName("UTCID01: Khởi tạo reset password thành công với email hợp lệ (Normal)")
    void initiateReset_WithValidEmail_ShouldSaveTokenAndSendEmail() {
        // Arrange
        when(accountRepository.findByEmail("user@example.com")).thenReturn(mockAccount);
        doNothing().when(tokenRepository).deleteByEmail(anyString());
        doNothing().when(tokenRepository).save(any(PasswordResetToken.class));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // Act
        assertDoesNotThrow(() ->
            accountService.initiatePasswordReset("user@example.com", "http://localhost:8080")
        );

        // Assert: token được lưu và email được gửi
        verify(tokenRepository, times(1)).deleteByEmail("user@example.com");
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("user@example.com"), anyString());
    }

    @Test
    @DisplayName("UTCID02: Email không tồn tại thì im lặng, không gửi email (Security - không tiết lộ)")
    void initiateReset_WithNonExistentEmail_ShouldDoNothing() {
        // Arrange
        when(accountRepository.findByEmail("notfound@example.com")).thenReturn(null);

        // Act
        assertDoesNotThrow(() ->
            accountService.initiatePasswordReset("notfound@example.com", "http://localhost:8080")
        );

        // Assert: không gọi gì cả
        verify(tokenRepository, never()).deleteByEmail(anyString());
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("UTCID03: Reset link được tạo đúng format URL (Normal)")
    void initiateReset_ShouldGenerateCorrectResetLink() {
        // Arrange
        when(accountRepository.findByEmail("user@example.com")).thenReturn(mockAccount);
        doNothing().when(tokenRepository).deleteByEmail(anyString());
        doNothing().when(tokenRepository).save(any());

        // Dùng ArgumentCaptor để bắt link được gửi
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), linkCaptor.capture());

        // Act
        accountService.initiatePasswordReset("user@example.com", "http://localhost:8080/myapp");

        // Assert: link phải bắt đầu đúng base URL và có token
        String capturedLink = linkCaptor.getValue();
        assertTrue(capturedLink.startsWith("http://localhost:8080/myapp/reset-password?token="));
        // Token phải là UUID (36 ký tự)
        String token = capturedLink.substring(capturedLink.indexOf("token=") + 6);
        assertEquals(36, token.length());
    }

    // ============================================================
    //   isValidResetToken()
    // ============================================================

    @Test
    @DisplayName("UTCID04: Token hợp lệ và chưa hết hạn trả về true (Normal)")
    void isValidToken_WithValidToken_ShouldReturnTrue() {
        // Arrange: token còn hạn (hết hạn trong 1 giờ từ bây giờ)
        PasswordResetToken validToken = new PasswordResetToken("valid-uuid-token", "user@example.com");
        when(tokenRepository.findByToken("valid-uuid-token")).thenReturn(validToken);

        // Act + Assert
        assertTrue(accountService.isValidResetToken("valid-uuid-token"));
    }

    @Test
    @DisplayName("UTCID05: Token không tồn tại trong DB trả về false (Abnormal)")
    void isValidToken_WithNullToken_ShouldReturnFalse() {
        // Arrange
        when(tokenRepository.findByToken("nonexistent-token")).thenReturn(null);

        // Act + Assert
        assertFalse(accountService.isValidResetToken("nonexistent-token"));
    }

    @Test
    @DisplayName("UTCID06: Token đã hết hạn trả về false (Abnormal)")
    void isValidToken_WithExpiredToken_ShouldReturnFalse() {
        // Arrange: Tạo token với thời hạn đã qua
        PasswordResetToken expiredToken = new PasswordResetToken("expired-token", "user@example.com");
        expiredToken.setExpiryDate(LocalDateTime.now().minusHours(2)); // hết hạn 2 tiếng trước
        when(tokenRepository.findByToken("expired-token")).thenReturn(expiredToken);

        // Act + Assert
        assertFalse(accountService.isValidResetToken("expired-token"));
    }

    // ============================================================
    //   resetPassword()
    // ============================================================

    @Test
    @DisplayName("UTCID07: Reset password thành công với đầy đủ dữ liệu hợp lệ (Normal)")
    void resetPassword_WithValidData_ShouldUpdatePasswordAndDeleteToken() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken("valid-token", "user@example.com");
        when(tokenRepository.findByToken("valid-token")).thenReturn(validToken);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(mockAccount);
        doNothing().when(accountRepository).update(any(Account.class));
        doNothing().when(tokenRepository).delete(any(PasswordResetToken.class));

        // Act
        assertDoesNotThrow(() ->
            accountService.resetPassword("valid-token", "newpass123", "newpass123")
        );

        // Assert: password được cập nhật
        assertEquals("newpass123", mockAccount.getPassword());
        verify(accountRepository, times(1)).update(mockAccount);
        // Token bị xóa sau khi dùng
        verify(tokenRepository, times(1)).delete(validToken);
    }

    @Test
    @DisplayName("UTCID08: Token không tồn tại ném ra RuntimeException (Abnormal)")
    void resetPassword_WithInvalidToken_ShouldThrowException() {
        // Arrange
        when(tokenRepository.findByToken("fake-token")).thenReturn(null);

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            accountService.resetPassword("fake-token", "newpass123", "newpass123")
        );
        assertEquals("Token không hợp lệ.", ex.getMessage());

        // Không cập nhật gì cả
        verify(accountRepository, never()).update(any());
    }

    @Test
    @DisplayName("UTCID09: Token hết hạn ném ra RuntimeException và xóa token (Abnormal)")
    void resetPassword_WithExpiredToken_ShouldThrowAndDeleteToken() {
        // Arrange
        PasswordResetToken expiredToken = new PasswordResetToken("expired-token", "user@example.com");
        expiredToken.setExpiryDate(LocalDateTime.now().minusHours(3));
        when(tokenRepository.findByToken("expired-token")).thenReturn(expiredToken);
        doNothing().when(tokenRepository).delete(expiredToken);

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            accountService.resetPassword("expired-token", "newpass123", "newpass123")
        );
        assertTrue(ex.getMessage().contains("hết hạn"));

        // Token hết hạn phải bị xóa khỏi DB
        verify(tokenRepository, times(1)).delete(expiredToken);
        verify(accountRepository, never()).update(any());
    }

    @Test
    @DisplayName("UTCID10: Mật khẩu xác nhận không khớp ném ra RuntimeException (Abnormal)")
    void resetPassword_WhenPasswordsDoNotMatch_ShouldThrowException() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken("valid-token", "user@example.com");
        when(tokenRepository.findByToken("valid-token")).thenReturn(validToken);

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            accountService.resetPassword("valid-token", "newpass123", "differentpass")
        );
        assertEquals("Mật khẩu xác nhận không khớp.", ex.getMessage());

        verify(accountRepository, never()).update(any());
    }

    @Test
    @DisplayName("UTCID11: Mật khẩu mới quá ngắn (dưới 8 ký tự) ném ra RuntimeException (Abnormal)")
    void resetPassword_WithTooShortPassword_ShouldThrowException() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken("valid-token", "user@example.com");
        when(tokenRepository.findByToken("valid-token")).thenReturn(validToken);

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            accountService.resetPassword("valid-token", "abc", "abc")
        );
        assertTrue(ex.getMessage().contains("8-20 ký tự"));
    }

    @Test
    @DisplayName("UTCID12: Mật khẩu mới quá dài (trên 20 ký tự) ném ra RuntimeException (Abnormal)")
    void resetPassword_WithTooLongPassword_ShouldThrowException() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken("valid-token", "user@example.com");
        when(tokenRepository.findByToken("valid-token")).thenReturn(validToken);

        String longPassword = "a".repeat(21);

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            accountService.resetPassword("valid-token", longPassword, longPassword)
        );
        assertTrue(ex.getMessage().contains("8-20 ký tự"));
    }

    @Test
    @DisplayName("UTCID13: Token hợp lệ nhưng account không còn trong DB (Edge Case)")
    void resetPassword_WhenAccountNotFound_ShouldThrowException() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken("valid-token", "deleted@example.com");
        when(tokenRepository.findByToken("valid-token")).thenReturn(validToken);
        when(accountRepository.findByEmail("deleted@example.com")).thenReturn(null);

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            accountService.resetPassword("valid-token", "newpass123", "newpass123")
        );
        assertEquals("Tài khoản không tồn tại.", ex.getMessage());
        verify(accountRepository, never()).update(any());
    }

    @Test
    @DisplayName("UTCID14: Mật khẩu đúng 8 ký tự (biên dưới) được chấp nhận (Boundary)")
    void resetPassword_WithMinLengthPassword_ShouldSucceed() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken("valid-token", "user@example.com");
        when(tokenRepository.findByToken("valid-token")).thenReturn(validToken);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(mockAccount);
        doNothing().when(accountRepository).update(any());
        doNothing().when(tokenRepository).delete(any());

        // Act + Assert: không ném exception với mật khẩu 8 ký tự
        assertDoesNotThrow(() ->
            accountService.resetPassword("valid-token", "12345678", "12345678")
        );
        assertEquals("12345678", mockAccount.getPassword());
    }

    @Test
    @DisplayName("UTCID15: Mật khẩu đúng 20 ký tự (biên trên) được chấp nhận (Boundary)")
    void resetPassword_WithMaxLengthPassword_ShouldSucceed() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken("valid-token", "user@example.com");
        when(tokenRepository.findByToken("valid-token")).thenReturn(validToken);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(mockAccount);
        doNothing().when(accountRepository).update(any());
        doNothing().when(tokenRepository).delete(any());

        String maxPass = "a".repeat(20);

        // Act + Assert
        assertDoesNotThrow(() ->
            accountService.resetPassword("valid-token", maxPass, maxPass)
        );
        assertEquals(maxPass, mockAccount.getPassword());
    }
}
