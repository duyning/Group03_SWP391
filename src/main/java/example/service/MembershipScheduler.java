package example.service;

import example.entity.Account;
import example.entity.MembershipLevel;
import example.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class MembershipScheduler {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MembershipService membershipService;

    // Lập lịch tự chạy lúc 00:00:00 mỗi ngày
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void generateDailyPlatVoucher() {
        System.out.println("--- BẮT ĐẦU CHẠY SCHEDULER: Tặng Voucher cho thành viên PLAT Hàng Ngày ---");
        
        List<Account> platAccounts = accountRepository.findByMembershipLevel(MembershipLevel.PLAT);
        
        int count = 0;
        for (Account account : platAccounts) {
            // Tặng 1 voucher 5% cho tài khoản PLAT, hạn dùng trong 1 ngày
            membershipService.generateVoucherForAccount(account, 5, 1);
            
            // Update db để lưu relation ManyToMany
            accountRepository.update(account); 
            count++;
        }
        
        System.out.println("--- ĐÃ HOÀN TẤT PHÁT VOUCHER 5% CHO " + count + " THÀNH VIEN PLAT ---");
    }
}
