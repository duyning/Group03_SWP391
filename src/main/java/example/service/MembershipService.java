package example.service;

import example.entity.Account;
import example.entity.MembershipLevel;
import example.entity.Voucher;
import example.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MembershipService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VoucherService voucherService;

    @Transactional
    public void addPointsAndUpgrade(Account detachedAccount, Double totalAmount) {
        if (detachedAccount == null || totalAmount == null || totalAmount <= 0) return;

        // Lấy lại managed entity để tránh lỗi LazyInitializationException
        Account account = accountRepository.findById(detachedAccount.getAccountID());
        if (account == null) return;

        // 1. Tính điểm cộng thêm (10.000 vnđ = 1 điểm)
        int pointsEarned = (int) (totalAmount / 10000);
        int currentPoints = account.getLoyaltyPoint() + pointsEarned;
        account.setLoyaltyPoint(currentPoints);

        MembershipLevel currentLevel = account.getMembershipLevel();
        if (currentLevel == null) {
            currentLevel = MembershipLevel.SILVER;
        }

        // 2. Xét thăng hạng và tặng quà (ưu tiên hạng PLAT trước)
        if (currentPoints >= 2000 && currentLevel != MembershipLevel.PLAT) {
            account.setMembershipLevel(MembershipLevel.PLAT);
            
            // Lên PLAT: Tặng 2 Voucher giảm 30%, hạn 30 ngày
            generateVoucherForAccount(account, 30, 30);
            generateVoucherForAccount(account, 30, 30);
            System.out.println("Thăng hạng PLAT cho tài khoản: " + account.getEmail());
            
        } else if (currentPoints >= 500 && currentLevel == MembershipLevel.SILVER) {
            account.setMembershipLevel(MembershipLevel.GOLD);
            
            // Lên GOLD: Tặng 1 Voucher giảm 20%, hạn 30 ngày
            generateVoucherForAccount(account, 20, 30);
            System.out.println("Thăng hạng GOLD cho tài khoản: " + account.getEmail());
        }

        // 3. Cập nhật vào DB (Thực tế khi gọi set trên managed entity thì Hibernate sẽ tự flush)
        accountRepository.update(account);
        
        // Cập nhật ngược lại detachedAccount để Controller nếu dùng vẫn có data mới nhất
        detachedAccount.setLoyaltyPoint(currentPoints);
        detachedAccount.setMembershipLevel(account.getMembershipLevel());
    }

    @Transactional
    public void generateVoucherForAccount(Account account, int discountPercent, int validateDays) {
        Voucher voucher = new Voucher();
        // Sinh mã voucher ngẫu nhiên 8 ký tự, ví dụ: VIP-ASDFGHJK
        String randomCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        voucher.setCode("VIP-" + randomCode);
        voucher.setDiscountPercent(discountPercent);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(validateDays));
        voucher.setActive(true);
        // Đánh dấu là voucher cá nhân - KHÔNG hiển thị trong trang public cho user khác collect
        voucher.setPersonal(true);

        // Lưu Voucher vào bảng vouchers
        voucher = voucherService.saveVoucher(voucher);

        // Gắn quan hệ N-N: Gán Voucher thẳng vào ví của người dùng
        account.getMyVouchers().add(voucher);
    }
}
