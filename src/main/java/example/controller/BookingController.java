package example.controller;

import example.entity.*;
import example.repository.BookingRepository;
import example.service.AccountService;
import example.service.BookingService;
import example.service.ComboService;
import example.service.ShowtimeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private ComboService comboService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AccountService accountService;

    // Đây là hàm xử lý khi user vào link: .../booking/seat?showtimeId=10
    @GetMapping("/seat")
    public String seatPage(@RequestParam int showtimeId, Model model) {

        // 1. Lấy thông tin suất chiếu từ ID
        Showtime showtime = showtimeService.getShowtimeById(showtimeId);

        // 2. Nếu không tìm thấy suất chiếu, quay về trang chủ hoặc báo lỗi
        if (showtime == null) {
            return "redirect:/home";
        }

        // 3. Đẩy dữ liệu sang View để Thymeleaf hiển thị (Tên phim, Rạp, Giờ...)
        model.addAttribute("showtime", showtime);

        // 4. Trả về tên file HTML giao diện (nằm trong thư mục client)
        // Đảm bảo bạn đã lưu file booking_seat.html trong thư mục view tương ứng (ví dụ: templates/client/ hoặc WEB-INF/client/)
        return "user/booking_seat";
    }

    @PostMapping("/combo")
    public String comboPage(
            @RequestParam("showtimeId") int showtimeId,
            @RequestParam("seatIds") String seatIds,
            @RequestParam("seatNames") String seatNames,
            @RequestParam("ticketPrice") Double ticketPrice,
            Model model) {

        // 1. Lấy thông tin suất chiếu để hiển thị bên Sidebar
        Showtime showtime = showtimeService.getShowtimeById(showtimeId);
        if (showtime == null) {
            return "redirect:/home"; // Báo lỗi hoặc quay về trang chủ nếu mất session
        }

        // 2. Lấy danh sách toàn bộ Combo đang bật (Active = true)
        List<Combo> activeCombos = comboService.getAll().stream()
                .filter(c -> c.getActive() != null && c.getActive())
                .collect(Collectors.toList());

        // 3. Đẩy dữ liệu ra View (booking_combo.html)
        model.addAttribute("showtime", showtime);
        model.addAttribute("combos", activeCombos);
        model.addAttribute("seatIds", seatIds);         // Chuyển tiếp ID ghế ("101,102")
        model.addAttribute("seatNames", seatNames);     // Tên ghế ("A1, A2") để in ra màn hình
        model.addAttribute("ticketTotalPrice", ticketPrice); // Tiền vé mang sang để JS cộng dồn

        // 4. Trả về giao diện Combo
        // Lưu ý: Đảm bảo file HTML bạn vừa tạo được lưu đúng ở src/main/resources/templates/user/booking_combo.html
        return "user/booking_combo";
    }

    @PostMapping("/payment")
    public String proceedToPayment(@RequestParam("showtimeId") int showtimeId,
                                   @RequestParam("seatIds") String seatIds,
                                   @RequestParam("seatNames") String seatNames,
                                   @RequestParam("comboData") String comboData,
                                   HttpSession session,
                                   Model model) {

        List<ComboBookingDTO> selectedCombos = new ArrayList<>();
        List<Seat> selectedSeats = new ArrayList<>();

        // Tính tổng tiền
        Double totalAmount = bookingService.calculateTotalAmount(
                seatIds, comboData, showtimeId, selectedCombos, selectedSeats);

        String comboString = selectedCombos.stream()
                .map(c -> c.getQuantity() + "x " + c.getComboName())
                .collect(Collectors.joining(", "));

        // Nếu không chọn combo nào thì để trống
        if (comboString.isEmpty()) comboString = "";

        // --- QUAN TRỌNG: LƯU VÀO SESSION ĐỂ LÁT NỮA LƯU DB ---
        session.setAttribute("booking_showtimeId", showtimeId);
        session.setAttribute("booking_seatIds", seatIds);
        session.setAttribute("booking_seatNames", seatNames);
        session.setAttribute("booking_comboDetails", comboString);
        session.setAttribute("booking_totalAmount", totalAmount);

        Showtime showtime = showtimeService.getShowtimeById(showtimeId);
        model.addAttribute("showtime", showtime);
        model.addAttribute("selectedSeats", selectedSeats);
        model.addAttribute("selectedCombos", selectedCombos);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("seatNames", seatNames);
        model.addAttribute("comboData", comboData);

        return "user/payment";
    }

    @GetMapping("/history")
    public String bookingHistory(Model model, Principal principal) {
        // 1. Lấy user đang đăng nhập từ Session
        if (principal != null) {
            model.addAttribute("account", accountService.findByEmail(principal.getName()));
        }
        String username = principal.getName();

        // Dùng username này tìm Account trong DB
        Account user = accountService.findByEmail(username);

        List<Booking> history = bookingRepository.findByAccountId(user.getAccountID());

        // 3. Đẩy dữ liệu ra view
        model.addAttribute("bookings", history);

        return "user/booking_history";
    }
}