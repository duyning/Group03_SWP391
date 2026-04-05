package example.controller;

import example.entity.Voucher;
import example.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/manager_vouchers")
public class ManagerVoucherController {
    @Autowired
    private VoucherService voucherService;

    @GetMapping
    public String listVouchers(@RequestParam(defaultValue = "1") int page, Model model) {
        int pageSize = 10;
        List<Voucher> vouchers = voucherService.findAllPaged(page, pageSize);
        long totalVouchers = voucherService.countTotal();
        int totalPages = (int) Math.ceil((double) totalVouchers / pageSize);

        // 1. Đồng bộ tên biến danh sách với HTML
        model.addAttribute("voucherList", vouchers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        // 2. Truyền Object cho Modal Form (để tránh lỗi th:object)
        model.addAttribute("voucherForm", new Voucher());

        // 3. Tính toán các thông số thống kê để HTML hiển thị được
        // Note: Statistics still based on all records for better overview
        List<Voucher> allVouchers = voucherService.findAll();
        long active = allVouchers.stream()
                .filter(v -> v.getExpiryDate() != null && v.getExpiryDate().isAfter(java.time.LocalDateTime.now()))
                .count();
        int maxPercent = allVouchers.stream()
                .mapToInt(Voucher::getDiscountPercent)
                .max().orElse(0);

        model.addAttribute("totalVouchers", totalVouchers);
        model.addAttribute("activeVouchers", active);
        model.addAttribute("expiredVouchers", totalVouchers - active);
        model.addAttribute("maxDiscount", maxPercent);

        return "admin/manager_vouchers";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("voucher", new Voucher());
        return "admin/manager_vouchers"; // Trang tạo mới
    }

    @PostMapping("/save")
    public String saveVoucher(@Valid @ModelAttribute("voucherForm") Voucher voucher,
                              BindingResult result,
                              RedirectAttributes ra,
                              Model model) {
        // 1. Kiểm tra lỗi Validation cơ bản (trống trường...)
        if (result.hasErrors()) {
            loadListData(model, 1); // Quay về trang 1 nếu có lỗi
            return "admin/manager_vouchers";
        }

        // 2. LOGIC QUAN TRỌNG: Kiểm tra ngày hết hạn so với hiện tại
        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            ra.addFlashAttribute("error", "Hạn sử dụng không được nhỏ hơn thời gian hiện tại!");
            return "redirect:/admin/manager_vouchers";
        }

        try {
            voucherService.saveVoucher(voucher);
            ra.addFlashAttribute("success", "Thêm Voucher thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/manager_vouchers";
    }

    // Hàm bổ trợ để tránh lặp code khi có lỗi validation
    private void loadListData(Model model, int page) {
        int pageSize = 10;
        List<Voucher> vouchers = voucherService.findAllPaged(page, pageSize);
        long totalVouchers = voucherService.countTotal();
        int totalPages = (int) Math.ceil((double) totalVouchers / pageSize);

        model.addAttribute("voucherList", vouchers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalVouchers", totalVouchers);

        List<Voucher> allVouchers = voucherService.findAll();
        long active = allVouchers.stream()
                .filter(v -> v.getExpiryDate() != null && v.getExpiryDate().isAfter(java.time.LocalDateTime.now()))
                .count();
        int maxPercent = allVouchers.stream()
                .mapToInt(Voucher::getDiscountPercent)
                .max().orElse(0);

        model.addAttribute("activeVouchers", active);
        model.addAttribute("expiredVouchers", totalVouchers - active);
        model.addAttribute("maxDiscount", maxPercent);
    }

    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable("id") int id, RedirectAttributes ra) {
        try {
            voucherService.deleteVoucher(id);
            ra.addFlashAttribute("success", "Đã xóa voucher thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi xóa voucher: " + e.getMessage());
        }
        return "redirect:/admin/manager_vouchers";
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public Voucher getVoucherForEdit(@PathVariable("id") int id) {
        Voucher v = voucherService.findById(id);
        System.out.println("Đang lấy voucher: " + v.getCode()); // Log để kiểm tra server có chạy vào đây không
        return v;
    }
}