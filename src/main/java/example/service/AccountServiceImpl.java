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

    // ===== SPRING SECURITY: LOAD USER =====
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));

        // Passing account.isStatus() as 'enabled'. If false, Spring Security
        // throws DisabledException automatically → caught by failure handler.
        return new User(
                account.getEmail(),
                account.getPassword(),
                account.isStatus(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<example.entity.Voucher> getAvailableVouchers(String email) {
        Account user = accountRepository.findByEmail(email);
        if (user == null) return java.util.Collections.emptyList();
        
        // Buộc Hibernate tải dữ liệu của collection (Initialize Proxy) vì hàm này đang nằm trong Transactional
        org.hibernate.Hibernate.initialize(user.getMyVouchers());
        org.hibernate.Hibernate.initialize(user.getUsedVouchers());

        java.util.Set<example.entity.Voucher> availableVouchers = new java.util.HashSet<>(user.getMyVouchers());
        availableVouchers.removeAll(user.getUsedVouchers());

        // Lọc bỏ Voucher hết hạn hoặc bị khoá
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<example.entity.Voucher> validVouchers = availableVouchers.stream()
                .filter(v -> v.getExpiryDate() != null && !v.getExpiryDate().isBefore(now))
                .collect(java.util.stream.Collectors.toList());

        return validVouchers;
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

    // ===== RESET PASSWORD VIA EMAIL =====
    @Override
    @Transactional
    public void initiatePasswordReset(String email, String appBaseUrl) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            // Không tiết lộ email có tồn tại không (bảo mật)
            return;
        }

        // Xóa token cũ của email này (nếu có)
        tokenRepository.deleteByEmail(email);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, email);
        tokenRepository.save(resetToken);

        // Gửi email
        String resetLink = appBaseUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(email, resetLink);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidResetToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        return resetToken != null && !resetToken.isExpired();
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);

        if (resetToken == null) {
            throw new RuntimeException("Token không hợp lệ.");
        }
        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token đã hết hạn. Vui lòng yêu cầu lại.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp.");
        }
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            throw new RuntimeException("Mật khẩu phải dài từ 8-20 ký tự.");
        }

        Account account = accountRepository.findByEmail(resetToken.getEmail());
        if (account == null) {
            throw new RuntimeException("Tài khoản không tồn tại.");
        }

        account.setPassword(newPassword);
        accountRepository.update(account);

        // Xóa token sau khi dùng xong
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
