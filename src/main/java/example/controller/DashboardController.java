package example.controller;

import example.repository.BookingRepository;
import example.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class DashboardController {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private MovieService movieService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private example.service.CinemaService cinemaService;

    @GetMapping("/dashboard")
    public String showDashboard(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "cinemaId", required = false) Integer cinemaId,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model,
            jakarta.servlet.http.HttpSession session) {

        // --- Hiển thị thông báo lỗi phân quyền nếu có ---
        String accessDeniedMessage = (String) session.getAttribute("accessDeniedMessage");
        if (accessDeniedMessage != null) {
            model.addAttribute("accessDeniedMessage", accessDeniedMessage);
            session.removeAttribute("accessDeniedMessage");
        }

        if (page < 1) page = 1;

        // 1. Tổng doanh thu hôm nay (Từ 00:00:00 đến 23:59:59)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        model.addAttribute("todayRevenue", bookingRepository.getTotalRevenueByDate(startOfDay, endOfDay));

        // 2. Tổng số vé đã bán
        model.addAttribute("totalTickets", bookingRepository.getTotalTicketsSold());

        // 3. Dữ liệu biểu đồ 7 ngày gần đây
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            labels.add(date.toString());
            data.add(bookingRepository.getTotalRevenueByDate(date.atStartOfDay(), date.atTime(LocalTime.MAX)));
        }
        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartData", data);

        // 3.5 Dữ liệu biểu đồ doanh thu theo tháng (12 tháng của năm nay)
        List<String> monthLabels = new ArrayList<>();
        List<Double> monthData = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = 1; i <= 12; i++) {
            monthLabels.add("Tháng " + i);
            LocalDateTime startMonth = LocalDateTime.of(currentYear, i, 1, 0, 0);
            LocalDateTime endMonth = startMonth.plusMonths(1).minusSeconds(1); // Ngày cuối cùng của tháng
            monthData.add(bookingRepository.getTotalRevenueByDate(startMonth, endMonth));
        }
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("monthData", monthData);

        // 3.5 Lấy danh sách rạp đổ ra dropdown lọc dữ liệu
        model.addAttribute("cinemas", cinemaService.getAllCinemas());

        // 4. Danh sách đặt vé có PHÂN TRANG (lấy TẤT CẢ, không giới hạn 10)
        boolean hasFilter = (search != null && !search.trim().isEmpty())
                || (cinemaId != null)
                || (minPrice != null)
                || (maxPrice != null);

        long totalItems;
        if (hasFilter) {
            String safeSearch = search != null ? search.trim() : null;
            totalItems = bookingRepository.countSearchBookings(safeSearch, cinemaId, minPrice, maxPrice);
            model.addAttribute("recentBookings",
                    bookingRepository.searchBookingsPaged(safeSearch, cinemaId, minPrice, maxPrice, page, PAGE_SIZE));
            model.addAttribute("searchKeyword", safeSearch);
            model.addAttribute("selectedCinemaId", cinemaId);
            model.addAttribute("minPrice", minPrice);
            model.addAttribute("maxPrice", maxPrice);
        } else {
            totalItems = bookingRepository.countAll();
            model.addAttribute("recentBookings", bookingRepository.findAllPaged(page, PAGE_SIZE));
        }

        // 5. Tính toán thông tin phân trang
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;
        if (page > totalPages) page = totalPages;

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);

        return "admin/dashboard";
    }
}