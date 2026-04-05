package example.controller;

import example.entity.Cinema;
import example.service.CinemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class CinemaController {

    @Autowired
    private CinemaService cinemaService;

    // Luôn cung cấp form object để tránh lỗi BindingResult
    @ModelAttribute("cinemaForm")
    public Cinema setupCinemaForm() {
        return new Cinema();
    }

    /**
     * Hàm dùng chung để nạp dữ liệu phân trang cho Model
     */
    private void populatePaginationModel(Model model, int page, int pageSize) {
        List<Cinema> cinemas = cinemaService.getAllCinemasPaged(page, pageSize);
        long totalCinemas = cinemaService.getTotalCount();
        int totalPages = (int) Math.ceil((double) totalCinemas / pageSize);

        model.addAttribute("cinemas", cinemas);
        model.addAttribute("totalCinemas", totalCinemas);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages > 0 ? totalPages : 1);
    }

    @GetMapping("/manager_cinema")
    public String listCinemas(@RequestParam(defaultValue = "1") int page, Model model) {
        populatePaginationModel(model, page, 10);
        return "admin/manager_cinema";
    }

    @PostMapping("/cinema/add")
    public String addCinema(@ModelAttribute("cinemaForm") Cinema cinema, RedirectAttributes ra) {
        cinemaService.saveCinema(cinema);
        ra.addFlashAttribute("message", "Thêm rạp mới thành công!");
        return "redirect:/admin/manager_cinema";
    }

    @GetMapping("/cinema/edit/{id}")
    public String editCinemaForm(@PathVariable("id") int id,
                                 @RequestParam(defaultValue = "1") int page,
                                 Model model) {
        Cinema cinema = cinemaService.getCinemaById(id);
        model.addAttribute("cinemaForm", cinema);
        model.addAttribute("isEdit", true);

        // Nạp lại danh sách và phân trang để UI bên dưới Modal không bị trống
        populatePaginationModel(model, page, 10);
        return "admin/manager_cinema";
    }

    @PostMapping("/cinema/update")
    public String updateCinema(@ModelAttribute("cinemaForm") Cinema cinema, RedirectAttributes ra) {
        cinemaService.saveCinema(cinema);
        ra.addFlashAttribute("message", "Cập nhật thông tin rạp thành công!");
        return "redirect:/admin/manager_cinema";
    }

    @GetMapping("/cinema/delete")
    public String deleteCinema(@RequestParam("id") int id, RedirectAttributes ra) {
        try {
            boolean success = cinemaService.deleteCinema(id);
            if (success) {
                ra.addFlashAttribute("message", "Đã xóa rạp thành công!");
            } else {
                ra.addFlashAttribute("error", "Không thể xóa rạp này do hệ thống vẫn còn phòng chiếu trực thuộc.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Đã có lỗi xảy ra khi xóa rạp chiếu.");
        }
        return "redirect:/admin/manager_cinema";
    }

    @GetMapping("/cinema/search")
    public String searchCinemas(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Integer minRooms,
            @RequestParam(required = false) String phone,
            Model model) {

        // Gọi Service với đầy đủ 6 tham số search
        List<Cinema> results = cinemaService.searchCinemas(name, city, status, address, minRooms, phone);

        model.addAttribute("cinemas", results);
        model.addAttribute("totalCinemas", results.size());

        // Giữ giá trị input để người dùng thấy họ đã search gì
        model.addAttribute("searchName", name);
        model.addAttribute("searchCity", city);
        model.addAttribute("searchStatus", status);
        model.addAttribute("searchAddress", address);
        model.addAttribute("searchMinRooms", minRooms);
        model.addAttribute("searchPhone", phone);

        // Reset phân trang về 1 khi search (đơn giản hóa)
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 1);

        return "admin/manager_cinema";
    }
}