package example.controller;

import example.entity.Account;
import example.entity.Voucher;
import example.service.AccountService;
import example.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherApiController {
    @Autowired
    private VoucherService voucherService;

    @Autowired
    private AccountService accountService;

    @GetMapping("/check")
    @Transactional(readOnly = true)
    public ResponseEntity<?> checkVoucher(@RequestParam("code") String code, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để sử dụng mã giảm giá"));
        }

        Voucher voucher = voucherService.findByCode(code);

        if (voucher == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Mã giảm giá không tồn tại"));
        }

        if (!voucher.isActive()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Mã giảm giá đã bị vô hiệu hóa"));
        }

        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Mã giảm giá đã hết hạn sử dụng"));
        }

        Account account = accountService.findByEmail(principal.getName());

        // Nếu là voucher cá nhân (VIP reward), kiểm tra ngay xem có thuộc về user này không
        // Điều này ngăn user nhập tay mã voucher VIP của người khác
        if (voucher.isPersonal()) {
            boolean isOwner = account.getMyVouchers().stream().anyMatch(v -> v.getId() == voucher.getId());
            if (!isOwner) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Mã giảm giá không hợp lệ"));
            }
        }

        if (account.getUsedVouchers().stream().anyMatch(v -> v.getId() == voucher.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Bạn đã sử dụng mã giảm giá này rồi"));
        }

        if (account.getMyVouchers().stream().noneMatch(v -> v.getId() == voucher.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Bạn chưa lưu mã giảm giá này vào ví Voucher"));
        }

        return ResponseEntity.ok(voucher);
    }
}
