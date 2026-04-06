package example.controller;

import example.entity.Showtime;
import example.service.ShowtimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/booking")
public class BookingRestController {

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private example.repository.SeatRepository seatRepository;

    @Autowired
    private example.repository.TicketRepository ticketRepository;

    // API: /api/booking/showtimes?movieId=1&cinemaId=2&date=2024-02-14
    @GetMapping("/showtimes")
    public ResponseEntity<?> getShowtimes(
            @RequestParam int movieId,
            @RequestParam int cinemaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // Phải có dòng này để lấy dữ liệu bỏ vào biến 'list'
        List<Showtime> list = showtimeService.findShowtimesForBooking(movieId, cinemaId, date);

        // Lọc các suất chiếu đã qua trong ngày hôm nay
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            list = Collections.emptyList();
        } else if (date.isEqual(today)) {
            LocalTime now = LocalTime.now();
            list = list.stream()
                    .filter(s -> s.getStartTime().isAfter(now))
                    .collect(Collectors.toList());
        }

        // Chuyển đổi sang JSON dùng HashMap (Cách an toàn nhất)
        List<Map<String, Object>> result = list.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            // Cắt chuỗi HH:mm:ss lấy HH:mm
            map.put("time", s.getStartTime().toString().substring(0, 5));
            map.put("roomName", s.getRoom().getRoomName());
            map.put("roomType", s.getRoom().getRoomType());
            
            // Tính số ghế trống = Tổng ghế phòng - Số vé đã đặt cho suất chiếu
            int totalSeats = seatRepository.findByRoomId(s.getRoom().getId()).size();
            int bookedSeats = ticketRepository.getBookedSeatIds(s.getId()).size();
            map.put("seatsLeft", totalSeats > bookedSeats ? (totalSeats - bookedSeats) : 0);
            
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // API 2: Lấy sơ đồ ghế cho một suất chiếu cụ thể
    // URL ví dụ: /api/booking/seat-map/123
    @GetMapping("/seat-map/{showtimeId}")
    public ResponseEntity<?> getSeatMap(@PathVariable int showtimeId) {
        try {
            // Gọi hàm getSeatMap bên Service (hàm mà chúng ta vừa viết ở bước trước)
            List<Map<String, Object>> seatMap = showtimeService.getSeatMap(showtimeId);
            return ResponseEntity.ok(seatMap);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi lấy dữ liệu ghế: " + e.getMessage());
        }
    }

    // API 3: Lấy danh sách phim kèm suất chiếu theo rạp và ngày
    @GetMapping("/movies-with-showtimes")
    public ResponseEntity<?> getMoviesWithShowtimes(
            @RequestParam int cinemaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Map<String, Object>> result = showtimeService.getMoviesWithShowtimes(cinemaId, date);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi lấy lịch chiếu: " + e.getMessage());
        }
    }
}