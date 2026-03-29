package example.controller;

import example.entity.News;
import example.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/admin/manager_news")
public class ManagerNewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping
    public String listNews(Model model) {
        List<News> list = newsService.findAllForAdmin(1, 10);
        model.addAttribute("newsList", list);

        // Đổi tên biến ở đây để khớp với HTML bạn đã viết
        model.addAttribute("totalNews", newsService.countTotal());
        model.addAttribute("activeCount", newsService.countActive());
        // HTML sẽ tự tính: ${totalNews - activeCount}

        return "admin/manager_news";
    }

    // Endpoint tạo dữ liệu mẫu
    @GetMapping("/init")
    public String initNewsData() {
        if (newsService.countTotal() == 0) {
            News n1 = new News();
            n1.setTitle("Khuyến mãi thứ 3 vui vẻ - Vé chỉ 45k");
            n1.setImageUrl("https://www.betacinemas.vn/Assets/Common/logo/logo.png"); // using default logo as fallback if real img expires
            n1.setStatus(true);
            n1.setContent("<p>Thứ 3 hàng tuần, rạp mang đến chương trình ĐỒNG GIÁ 45K cho toàn bộ phim 2D tại rạp. Đừng bỏ lỡ những siêu phẩm điện ảnh với mức giá hạt dẻ nhất nhé!</p>");
            newsService.saveOrUpdate(n1);

            News n2 = new News();
            n2.setTitle("Ra mắt Combo Bắp Nước Mới: Phô Mai Trân Châu");
            n2.setImageUrl("https://www.betacinemas.vn/Assets/Common/logo/logo.png");
            n2.setStatus(true);
            n2.setContent("<p>Combo bắp phô mai trân châu mới cực đỉnh đã chính thức cập bến. Sự kết hợp lạ miệng đảm bảo gây nghiện.</p>");
            newsService.saveOrUpdate(n2);

            News n3 = new News();
            n3.setTitle("Tuyển dụng Nhân viên rạp (Part-time & Full-time)");
            n3.setImageUrl("https://www.betacinemas.vn/Assets/Common/logo/logo.png");
            n3.setStatus(true);
            n3.setContent("<p>Chúng tôi đang tìm kiếm những mảnh ghép để hoàn thiện đội ngũ cực cháy tại hệ thống rạp trên toàn quốc. Gửi CV ngay!</p>");
            newsService.saveOrUpdate(n3);
        }
        return "redirect:/admin/manager_news";
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public News editNews(@PathVariable Long id) {
        return newsService.getById(id);
    }

    // Lưu tin tức mới hoặc cập nhật
    @PostMapping("/save")
    public String saveNews(@ModelAttribute News news, @RequestParam("file") MultipartFile file) {
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
        newsService.saveOrUpdate(news);

        return "redirect:/admin/manager_news";
    }

    // Thay đổi trạng thái nhanh (Ẩn/Hiện)
    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id) {
        // Gọi trực tiếp hàm toggle logic đã viết ở Service
        newsService.toggleStatus(id);
        return "redirect:/admin/manager_news";
    }

    // Xóa tin tức
    // Trong AdminNewsController.java
    @GetMapping("/delete/{id}")
    public String deleteNews(@PathVariable Long id) {
        try {
            newsService.delete(id);
            // Có thể thêm redirect kèm tham số để báo thành công nếu muốn
            return "redirect:/admin/manager_news?deleteSuccess=true";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/manager_news?error=true";
        }
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