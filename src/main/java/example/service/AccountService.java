package example.service;


import example.entity.Account;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.List;

public interface AccountService extends UserDetailsService {
    void register(Account account);

    Account login(String email, String password);

    void updateProfile(Account account);

    void changePassword(Account account, String oldPassword, String newPassword, String confirmPassword);

    Account findByEmail(String email);

    List<example.entity.Voucher> getAvailableVouchers(String email);

    void createManager(Account account);

    List<Account> findAll();

    void toggleStatus(int id);

    // --- RESET PASSWORD VIA EMAIL ---
    void initiatePasswordReset(String email, String appBaseUrl);

    void resetPassword(String token, String newPassword, String confirmPassword);

    boolean isValidResetToken(String token);

    void updateAvatar(String email, String fileName);
}
