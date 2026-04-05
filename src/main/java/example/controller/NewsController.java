package example.controller;

import example.entity.News;
import example.service.AccountService;
import example.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/user/news")
public class NewsController {
    @Autowired
    private NewsService newsService;

    @Autowired
    private AccountService accountService;

    @GetMapping
    public String listNewsForUser(
            @RequestParam(value = "page", defaultValue = "1") int page,
            Principal principal,
            Model model) {

        if (principal != null) {
            model.addAttribute("account", accountService.findByEmail(principal.getName()));
        }

        int pageSize = 6;
        List<News> list = newsService.findVisibleForUser(page, pageSize);

        model.addAttribute("newsList", list);
        model.addAttribute("currentPage", page);

        long totalActive = newsService.countActive();
        int totalPages = (int) Math.ceil((double) totalActive / pageSize);
        model.addAttribute("totalPages", totalPages);

        return "user/news";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public News getNewsForModal(@PathVariable("id") Long id) {
        // Chỉ lấy tin tức có status = true
        return newsService.getByIdForUser(id);
    }
}
