package example.service;

import example.entity.Account;
import example.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AccountEditProfileTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    public void testUpdateProfile_Success() {
        // Cấu hình dữ liệu giả
        Account account = new Account();
        account.setName("Nguyen Van New");
        account.setPhoneNum("0987654321");
        account.setAge(25);

        // Gọi phương thức cần test
        accountService.updateProfile(account);

        // Kiểm tra xem repository có được gọi để lưu không
        verify(accountRepository, times(1)).update(account);

        // Kiểm tra dữ liệu trong object sau khi xử lý (nếu service có logic biến đổi)
        assertEquals("Nguyen Van New", account.getName());
        assertEquals("0987654321", account.getPhoneNum());
        assertEquals(25, account.getAge());
    }
}
