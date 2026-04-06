package example.service;

import example.entity.Account;
import example.entity.MembershipLevel;
import example.entity.Role;
import example.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    public void testRegister_Success() {
        // Nhập tài khoản mới thành công
        Account account = new Account();
        account.setEmail("test@example.com");

        when(accountRepository.existsByEmail("test@example.com")).thenReturn(false);

        accountService.register(account);

        // Kiểm tra logic thiết lập dữ liệu mặc định
        assertEquals(Role.CUSTOMER, account.getRole());
        assertEquals(MembershipLevel.SILVER, account.getMembershipLevel());
        assertTrue(account.isStatus());
        assertEquals(0, account.getLoyaltyPoint());
        assertEquals(0, account.getAvatar());

        // Kiểm tra xem Repo save có được gọi không
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    public void testRegister_EmailAlreadyExists() {
        // Mô phỏng trường hợp Email đã tồn tại
        Account account = new Account();
        account.setEmail("test@example.com");

        // Giả lập repo báo là email đã tồn tại
        when(accountRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.register(account);
        });

        assertEquals("Email already exists", exception.getMessage());

        // Đảm bảo rằng hàm save KHÔNG bị gọi
        verify(accountRepository, never()).save(any(Account.class));
    }
}
