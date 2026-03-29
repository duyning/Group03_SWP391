package example.controller;

import example.entity.Account;
import example.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;

@Controller
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("account", new Account());
        // Trỏ vào thư mục user (Ví dụ: WEB-INF/user/register.html)
        return "user/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("account") Account account,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            return "user/register";
        }

        try {
            accountService.register(account);
            model.addAttribute("success", "Register successfully! Please login.");
            return "redirect:/login"; // Redirect giữ nguyên vì nó gọi đường dẫn (URL), không phải file HTML

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "user/register";
        }
    }

    // ===== LOGIN (Handled by Spring Security) =====
    @GetMapping("/login")
    public String showLoginForm() {
        return "user/login";
    }

    // ===== VIEW PROFILE =====
    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        String email = principal.getName();
        Account acc = accountService.findByEmail(email);
        model.addAttribute("account", acc);
        return "user/profile";
    }

    // ===== EDIT PROFILE =====
    @GetMapping("/edit-profile")
    public String showEditProfileForm(Principal principal, Model model) {
        String email = principal.getName();
        Account acc = accountService.findByEmail(email);
        model.addAttribute("account", acc);
        return "user/edit-profile";
    }

    @PostMapping("/edit-profile")
    public String editProfile(
            @Valid @ModelAttribute("account") Account updatedAccount,
            BindingResult result,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = principal.getName();
        Account currentAccount = accountService.findByEmail(email);

        // Manually check only the fields that are present in the Edit Profile form
        if (result.hasFieldErrors("name") || result.hasFieldErrors("phoneNum") || 
            result.hasFieldErrors("age") || result.hasFieldErrors("gender")) {
            return "user/edit-profile";
        }

        try {
            // Keep fields that shouldn't be changed
            updatedAccount.setAccountID(currentAccount.getAccountID());
            updatedAccount.setEmail(currentAccount.getEmail());
            updatedAccount.setPassword(currentAccount.getPassword());
            updatedAccount.setRole(currentAccount.getRole());
            updatedAccount.setMembershipLevel(currentAccount.getMembershipLevel());
            updatedAccount.setLoyaltyPoint(currentAccount.getLoyaltyPoint());
            updatedAccount.setAvatar(currentAccount.getAvatar());
            updatedAccount.setStatus(currentAccount.isStatus());

            accountService.updateProfile(updatedAccount);

            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");

            return "redirect:/profile";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", currentAccount);
            return "user/edit-profile";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Principal principal, Model model) {
        // Moved to user folder
        return "user/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Principal principal,
            Model model) {

        String email = principal.getName();
        Account acc = accountService.findByEmail(email);

        try {
            accountService.changePassword(acc, oldPassword, newPassword, confirmPassword);
            model.addAttribute("success", "Password changed successfully!");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "user/change-password";
    }

    // ===== 1. GIAI ĐOẠN 1: GỬI YÊU CẦU QUÊN MẬT KHẨU =====
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "user/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String email,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        // Xây dựng base URL động (ví dụ: http://localhost:8080/app) để tạo link reset trong mail
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();

        // Gọi Service để kiểm tra email, tạo token và bắn mail
        accountService.initiatePasswordReset(email, baseUrl);

        // Bảo mật: Không tiết lộ email có tồn tại hay không để tránh bị dò quét tài khoản
        redirectAttributes.addFlashAttribute("success",
                "Nếu email tồn tại trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu trong vài phút.");
        return "redirect:/forgot-password";
    }

    // ===== 2. GIAI ĐOẠN 2: XÁC THỰC LINK VÀ ĐẶT LẠI MẬT KHẨU =====
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        // Kiểm tra Token có hợp lệ và còn hạn (thường là 24h hoặc ít hơn) không
        if (!accountService.isValidResetToken(token)) {
            model.addAttribute("error", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "user/reset-password";
        }
        // Truyền token xuống form Reset để dùng lúc Submit
        model.addAttribute("token", token);
        return "user/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Thực hiện đổi mật khẩu mới và xóa token sau khi dùng xong
            accountService.resetPassword(token, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("success", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            // Nếu có lỗi (mật khẩu không khớp, token hết hạn đột ngột...), hiện thông báo lại trang reset
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "user/reset-password";
        }
    }

    @PostMapping("/update-avatar")
    public String updateAvatar(@RequestParam("avatarFile") MultipartFile file,
                               Principal principal,
                               RedirectAttributes ra) {
        if (file.isEmpty()) return "redirect:/profile";

        try {
            String email = principal.getName();
            // 1. Cấu hình đường dẫn tuyệt đối trên ổ D
            String uploadDir = "D:/uploads/avatars/";

            String fileName = email.split("@")[0] + "_" + System.currentTimeMillis() + ".jpg";

            Path uploadPath = Paths.get(uploadDir);
            // Tạo thư mục nếu chưa tồn tại
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. Lưu file vào ổ D
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            // 3. Cập nhật tên file vào DB
            accountService.updateAvatar(email, fileName);
            ra.addFlashAttribute("success", "Cập nhật ảnh thành công!");

        } catch (IOException e) {
            ra.addFlashAttribute("error", "Lỗi lưu file: " + e.getMessage());
        }
        return "redirect:/profile";
    }
}
