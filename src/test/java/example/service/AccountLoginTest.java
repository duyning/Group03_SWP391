package example.service;

import example.entity.Account;
import example.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountLoginTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    public void testLogin_Success() {
        // Đăng nhập thành công
        String email = "test@example.com";
        String password = "Password123";

        Account mockAccount = new Account();
        mockAccount.setEmail(email);
        mockAccount.setPassword(password);
        mockAccount.setStatus(true);

        // Giả lập Repository trả về Account hợp lệ
        when(accountRepository.findByEmail(email)).thenReturn(mockAccount);

        Account result = accountService.login(email, password);

        // Đảm bảo trả về đúng Account
        assertEquals(email, result.getEmail());
        assertEquals(password, result.getPassword());
    }

    @Test
    public void testLogin_IncorrectPassword() {
        // Mật khẩu không đúng
        String email = "test@example.com";
        String wrongPassword = "WrongPassword";

        Account mockAccount = new Account();
        mockAccount.setEmail(email);
        mockAccount.setPassword("Password123");
        mockAccount.setStatus(true);

        when(accountRepository.findByEmail(email)).thenReturn(mockAccount);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.login(email, wrongPassword);
        });

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    public void testLogin_EmailNotFound() {
        // Email không được lưu (không tồn tại trong hệ thống)
        String email = "notfound@example.com";
        String password = "Password123";

        // Giả lập Repository trả về null (không tìm thấy)
        when(accountRepository.findByEmail(email)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.login(email, password);
        });

        assertEquals("Email does not exist", exception.getMessage());
    }

    @Test
    public void testLogin_EmailNull() {
        // Địa chỉ email là null
        String email = null;
        String password = "Password123";

        // Tùy thuộc vào backend, thường findByEmail(null) sẽ trả về null
        when(accountRepository.findByEmail(email)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.login(email, password);
        });

        assertEquals("Email does not exist", exception.getMessage());
    }

    @Test
    public void testLogin_InvalidEmailFormat() {
        // Địa chỉ email không đúng định dạng
        String email = "invalid-email-format";
        String password = "Password123";

        when(accountRepository.findByEmail(email)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.login(email, password);
        });

        assertEquals("Email does not exist", exception.getMessage());
    }

    @Test
    public void testLogin_AccountBlocked() {
        // Thêm một trường hợp bổ sung: Tài khoản bị khóa (status = false)
        String email = "test@example.com";
        String password = "Password123";

        Account mockAccount = new Account();
        mockAccount.setEmail(email);
        mockAccount.setPassword(password);
        mockAccount.setStatus(false); // Bị khóa

        when(accountRepository.findByEmail(email)).thenReturn(mockAccount);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.login(email, password);
        });

        assertEquals("Account is blocked", exception.getMessage());
    }
}
