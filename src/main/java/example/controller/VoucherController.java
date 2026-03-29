package example.controller;

import example.entity.Account;
import example.service.AccountService;
import example.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/user/vouchers")
public class VoucherController {
    @Autowired
    private VoucherService voucherService;

    @Autowired
    private AccountService accountService;

    // 1. Hiển thị danh sách Voucher còn hạn cho khách hàng
    @GetMapping("")
    public String showAvailableVouchers(Model model, Principal principal) {
        if (principal != null) {
            Account account = accountService.findByEmail(principal.getName());
            model.addAttribute("account", account);

            // Thêm danh sách ID đã lưu để trang HTML biết nút nào cần hiển thị "Đã lưu"
            List<Integer> savedIds = voucherService.getSavedVoucherIds(account.getAccountID());
            model.addAttribute("savedVoucherIds", savedIds);
        }

        model.addAttribute("vouchers", voucherService.getActiveVouchers());
        return "user/vouchers";
    }

    // 2. Xử lý khi khách hàng nhấn "Lưu Voucher"
    @PostMapping("/save-to-library")
    @ResponseBody
    public String saveToLibrary(@RequestParam("voucherId") int voucherId, Principal principal) {
        if (principal == null) return "error_auth";

        try {
            Account account = accountService.findByEmail(principal.getName());
            // Gọi service đã sửa đổi
            boolean success = voucherService.collectVoucher(account.getAccountID(), voucherId);
            return success ? "success" : "exists";
        } catch (Exception e) {
            return "error";
        }
    }

    @GetMapping("/my_vouchers")
    public String showMyVouchers(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        Account account = accountService.findByEmail(principal.getName());
        model.addAttribute("account", account);

        // Lấy danh sách Voucher thực thể và LỌC NHỮNG VOUCHER CÒN HẠN
        List<example.entity.Voucher> validVouchers = voucherService.getVouchersByAccountId(account.getAccountID())
                .stream()
                .filter(v -> v.getExpiryDate() != null && !v.getExpiryDate().isBefore(java.time.LocalDateTime.now()))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("myVouchers", validVouchers);

        return "user/my_vouchers";
    }
}
