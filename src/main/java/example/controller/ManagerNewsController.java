package example.controller;

import example.entity.News;
import example.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/manager_news")
public class ManagerNewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping
    public String listNews(@RequestParam(defaultValue = "1") int page, Model model) {
        int pageSize = 10;
        List<News> list = newsService.findAllForAdmin(page, pageSize);
        long totalNews = newsService.countTotal();
        int totalPages = (int) Math.ceil((double) totalNews / pageSize);

        model.addAttribute("newsList", list);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        model.addAttribute("totalNews", totalNews);
        model.addAttribute("activeCount", newsService.countActive());

        return "admin/manager_news";
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public News editNews(@PathVariable Long id) {
        return newsService.getById(id);
    }

    // Lưu tin tức mới hoặc cập nhật
    @PostMapping("/save")
    public String saveNews(@ModelAttribute News news, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        // 1. Xử lý ID cho trường hợp INSERT
        if (news.getId() != null && news.getId() == 0) {
            news.setId(null);
        }

        // 2. Xử lý hình ảnh
        if (!file.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String uploadDir = "C:/Users/USER/Downloads/uploads/news/";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                file.transferTo(new java.io.File(uploadDir + fileName));
                news.setImageUrl("/uploads/" + fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (news.getId() != null) {
            // Nếu không upload ảnh mới, giữ lại ảnh cũ
            News oldNews = newsService.getById(news.getId());
            if (oldNews != null) {
                news.setImageUrl(oldNews.getImageUrl());
            }
        }

        // 3. Lưu dữ liệu (Trường 'content' đã được tự động map vào object 'news')
        try {
            boolean isNew = (news.getId() == null);
            newsService.saveOrUpdate(news);
            if (isNew) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã đăng bài viết mới thành công!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật bài viết thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu bài viết: " + e.getMessage());
        }

        return "redirect:/admin/manager_news";
    }

    // Thay đổi trạng thái nhanh (Ẩn/Hiện)
    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Gọi trực tiếp hàm toggle logic đã viết ở Service
        newsService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái hiển thị!");
        return "redirect:/admin/manager_news";
    }

    // Xóa tin tức
    // Trong AdminNewsController.java
    @GetMapping("/delete/{id}")
    public String deleteNews(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            newsService.delete(id);
            // Có thể thêm redirect kèm tham số để báo thành công nếu muốn
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bài viết khỏi hệ thống!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa bài viết: " + e.getMessage());
        }
        return "redirect:/admin/manager_news";
    }

    @GetMapping("/search")
    public String searchNews(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "status", required = false) String statusStr,
            Model model) {

        // Chuyển đổi String status từ form sang Boolean cho DB
        Boolean status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            status = Boolean.parseBoolean(statusStr);
        }

        // Gọi repository để lấy dữ liệu đã lọc
        List<News> list = newsService.search(title, status);

        // Gửi dữ liệu lại cho giao diện
        model.addAttribute("newsList", list);
        model.addAttribute("searchTitle", title);
        model.addAttribute("searchStatus", statusStr);

        // Giữ nguyên các thông số thống kê ở trên đầu
        model.addAttribute("totalNews", newsService.countTotal());
        model.addAttribute("activeCount", newsService.countActive());

        return "admin/manager_news";
    }
}