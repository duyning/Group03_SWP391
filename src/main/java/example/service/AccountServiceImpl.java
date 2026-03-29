package example.service;

import example.entity.Account;
import example.entity.MembershipLevel;
import example.entity.PasswordResetToken;
import example.entity.Role;
import example.repository.AccountRepository;
import example.repository.PasswordResetTokenRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public AccountServiceImpl(AccountRepository accountRepository,
                              PasswordResetTokenRepository tokenRepository,
                              EmailService emailService) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    // ===== SPRING SECURITY: TẢI THÔNG TIN NGƯỜI DÙNG & PHÂN QUYỀN =====
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Tìm tài khoản trong Database theo Email
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        // 2. Chuyển đổi Role từ Entity sang GrantedAuthority của Spring Security
        // Lưu ý: Spring Security mặc định hiểu Role có tiền tố "ROLE_" (vd: ROLE_ADMIN)
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));

        // 3. Trả về đối tượng User của Spring Security để thực hiện xác thực
        // account.isStatus() được nạp vào biến 'enabled': Nếu status=false, Spring sẽ chặn đăng nhập
        return new User(
                account.getEmail(),
                account.getPassword(),
                account.isStatus(), // enabled (Trạng thái tài khoản)
                true,               // accountNonExpired
                true,               // credentialsNonExpired
                true,               // accountNonLocked
                authorities);       // Danh sách quyền (Roles)
    }

    // ===== REGISTER =====
    @Override
    @Transactional
    public void register(Account account) {

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        account.setRole(Role.CUSTOMER);
        account.setMembershipLevel(MembershipLevel.SILVER);
        account.setStatus(true);
        account.setLoyaltyPoint(0);
        account.setAvatar("default-avatar.png");

        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void createManager(Account account) {
        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        account.setRole(Role.MANAGER);
        account.setMembershipLevel(MembershipLevel.SILVER); // Managers might not need this, but for consistency
        account.setStatus(true);
        account.setLoyaltyPoint(0);
        account.setAvatar("default-avatar.png");

        accountRepository.save(account);
    }

    // ===== LOGIN (Legacy support or internal use) =====
    @Override
    @Transactional(readOnly = true)
    public Account login(String email, String password) {
        // Using repository directly for check, though strictly Security handles this
        // now.
        // This method might be unused by Controller but kept for interface contract.
        Account account = accountRepository.findByEmail(email);
        if (account == null)
            throw new RuntimeException("Email does not exist");
        if (!account.isStatus())
            throw new RuntimeException("Account is blocked");
        if (!account.getPassword().equals(password))
            throw new RuntimeException("Invalid password");
        return account;
    }

    // ===== UPDATE PROFILE =====
    @Override
    @Transactional
    public void updateProfile(Account account) {
        accountRepository.update(account);
    }

    // ===== CHANGE PASSWORD =====
    @Override
    @Transactional
    public void changePassword(Account account, String oldPassword, String newPassword, String confirmPassword) {

        if (!account.getPassword().equals(oldPassword)) {
            throw new RuntimeException("Old password is incorrect");
        }

        if (newPassword.length() < 8 || newPassword.length() > 20) {
            throw new RuntimeException("New password must be 8-20 characters");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Confirm password does not match");
        }

        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("New password must be different from old password");
        }

        account.setPassword(newPassword);
        accountRepository.update(account);
        account.setPassword(newPassword);
        accountRepository.update(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Account findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Override
    @Transactional
    public void toggleStatus(int id) {
        accountRepository.toggleStatus(id);
    }

    // ===== TẠO ĐIỀU KIỆN ĐẶT LẠI MẬT KHẨU (GỬI MAIL) =====
    @Override
    @Transactional
    public void initiatePasswordReset(String email, String appBaseUrl) {
        // 1. Tìm tài khoản theo email
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            // Không tiết lộ email có tồn tại không (chống dò tài khoản)
            return;
        }

        // 2. Xóa các Token reset cũ (nếu có) để tránh dư thừa dữ liệu cho 1 user
        tokenRepository.deleteByEmail(email);

        // 3. Tạo Token UUID ngẫu nhiên duy nhất
        String token = UUID.randomUUID().toString();
        
        // 4. Lưu Token vào DB (Bảng PasswordResetToken có hạn sử dụng mặc định, vd: 24h)
        PasswordResetToken resetToken = new PasswordResetToken(token, email);
        tokenRepository.save(resetToken);

        // 5. Gửi email chứa link Reset kèm token này
        String resetLink = appBaseUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(email, resetLink);
    }

    // ===== KIỂM TRA TÍNH HỢP LỆ CỦA TOKEN =====
    @Override
    @Transactional(readOnly = true)
    public boolean isValidResetToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        // Token phải tồn tại trong DB và chưa bị quá hạn (isExpired)
        return resetToken != null && !resetToken.isExpired();
    }

    // ===== THỰC HIỆN ĐỔI MẬT KHẨU MỚI TỪ TOKEN =====
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        // 1. Tìm lại Token trong DB
        PasswordResetToken resetToken = tokenRepository.findByToken(token);

        if (resetToken == null) {
            throw new RuntimeException("Token không hợp lệ.");
        }
        
        // 2. Kiểm tra hạn sử dụng Token một lần nữa (Security double check)
        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token đã hết hạn. Vui lòng yêu cầu lại.");
        }
        
        // 3. Kiểm tra logic mật khẩu nhập vào
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp.");
        }
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            throw new RuntimeException("Mật khẩu phải dài từ 8-20 ký tự.");
        }

        // 4. Tìm Account gắn với email lưu trong Token
        Account account = accountRepository.findByEmail(resetToken.getEmail());
        if (account == null) {
            throw new RuntimeException("Tài khoản không tồn tại.");
        }

        // 5. Áp mật khẩu mới và lưu vào DB
        account.setPassword(newPassword);
        accountRepository.update(account);

        // 6. QUAN TRỌNG: Xóa Token ngay sau khi dùng xong (One-time-use) để tránh bị lợi dụng đổi pass nhiều lần
        tokenRepository.delete(resetToken);
    }

    @Override
    @Transactional
    public void updateAvatar(String email, String fileName) {
        Account account = accountRepository.findByEmail(email);
        if (account != null) {
            account.setAvatar(fileName);
            accountRepository.update(account); // Sử dụng phương thức update có sẵn của bạn
        } else {
            throw new UsernameNotFoundException("Account not found");
        }
    }

}
