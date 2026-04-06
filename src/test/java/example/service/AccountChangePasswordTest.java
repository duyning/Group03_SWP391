package example.service;

import example.entity.Account;
import example.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class AccountChangePasswordTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    public void testChangePassword_Success() {
        // Đổi mật khẩu thành công
        Account account = new Account();
        account.setPassword("OldPassword123");

        accountService.changePassword(account, "OldPassword123", "NewPassword456", "NewPassword456");

        // Kiểm tra mật khẩu đã được cập nhật chưa
        assertEquals("NewPassword456", account.getPassword());

        // Kiểm tra xem hàm lưu vào CSDL (`update`) có được gọi không
        verify(accountRepository, times(1)).update(account);
    }

    @Test
    public void testChangePassword_IncorrectOldPassword() {
        // Sai mật khẩu hiện tại
        Account account = new Account();
        account.setPassword("OldPassword123");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.changePassword(account, "WrongPassword", "NewPassword456", "NewPassword456");
        });

        assertEquals("Old password is incorrect", exception.getMessage());

        // Đảm bảo không gọi update CSDL
        verify(accountRepository, never()).update(Mockito.any(Account.class));
    }

    @Test
    public void testChangePassword_NewPasswordTooShort() {
        // Mật khẩu mới sai độ dài (dưới 8 ký tự hoặc quá 20 ký tự)
        Account account = new Account();
        account.setPassword("OldPassword123");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.changePassword(account, "OldPassword123", "Short12", "Short12");
        });

        assertEquals("New password must be 8-20 characters", exception.getMessage());

        // Đảm bảo không gọi update CSDL
        verify(accountRepository, never()).update(Mockito.any(Account.class));
    }

    @Test
    public void testChangePassword_ConfirmPasswordMismatch() {
        // Xác nhận mật khẩu không khớp
        Account account = new Account();
        account.setPassword("OldPassword123");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.changePassword(account, "OldPassword123", "NewPassword456", "DifferentPassword");
        });

        assertEquals("Confirm password does not match", exception.getMessage());

        // Đảm bảo không gọi update CSDL
        verify(accountRepository, never()).update(Mockito.any(Account.class));
    }

    @Test
    public void testChangePassword_SameAsOldPassword() {
        // Mật khẩu mới trùng mật khẩu cũ
        Account account = new Account();
        account.setPassword("OldPassword123");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.changePassword(account, "OldPassword123", "OldPassword123", "OldPassword123");
        });

        assertEquals("New password must be different from old password", exception.getMessage());

        // Đảm bảo không gọi update CSDL
        verify(accountRepository, never()).update(Mockito.any(Account.class));
    }
}
