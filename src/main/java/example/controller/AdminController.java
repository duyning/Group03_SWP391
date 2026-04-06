package example.controller;

import example.entity.Account;
import example.service.AccountService;
import jakarta.validation.Valid; // Đã sửa sang jakarta cho chuẩn Spring 6
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // Đã sửa lại đường dẫn package cho chuẩn
    @Autowired
    private AccountService accountService;

    @Autowired
    private example.service.BlogService blogService;

    @GetMapping("/blogs")
    public String listBlogs(Model model) {
        model.addAttribute("posts", blogService.getAllPosts());
        return "admin/blog_list";
    }

    @GetMapping("/create-manager")
    public String createManagerForm(Model model) {
        model.addAttribute("account", new Account());
        return "admin/create_manager";
    }

    @PostMapping("/create-manager")
    public String createManager(
            @Valid @ModelAttribute("account") Account account, // Đã sửa @Valid
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/create_manager";
        }

        try {
            accountService.createManager(account);
            redirectAttributes.addFlashAttribute("successMessage", "Manager created successfully!");
            return "redirect:/admin/accounts";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/create-manager";
        }
    }

    @GetMapping("/accounts")
    public String listAccounts(Model model) {
        model.addAttribute("accounts", accountService.findAll());
        return "admin/account_list";
    }

    @PostMapping("/accounts/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            accountService.toggleStatus(id);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/accounts";
    }
}