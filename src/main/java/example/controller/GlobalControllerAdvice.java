package example.controller;

import example.entity.Account;
import example.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private AccountService accountService;

    // Hàm này sẽ tự động chạy trước TẤT CẢ các request
    // Nó đảm bảo trang nào cũng có thuộc tính "account" nếu đã đăng nhập
    @ModelAttribute
    public void addGlobalAttributes(Principal principal, Model model) {
        if (principal != null) {
            Account acc = accountService.findByEmail(principal.getName());
            model.addAttribute("account", acc);
        }
    }
}
