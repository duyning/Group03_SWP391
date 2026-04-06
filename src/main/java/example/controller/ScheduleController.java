package example.controller;

import example.entity.*;
import example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/manager_schedule")
public class ScheduleController {

    @Autowired private ShowtimeService showtimeService;
    @Autowired private CinemaService cinemaService;
    @Autowired private MovieService movieService;
    @Autowired private CinemaRoomService roomService;

    /**
     * 1. Hiển thị trang quản lý lịch chiếu
     */
    @GetMapping
    public String index(@RequestParam(required = false) Integer cinemaId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {

        // Lấy danh sách tất cả rạp
        List<Cinema> cinemas = cinemaService.getAllCinemas();

        // Xử lý tham số đầu vào
        LocalDate selectedDate = (date != null) ? date : LocalDate.now();
        int selectedCinemaId = (cinemaId != null) ? cinemaId : (cinemas.isEmpty() ? 0 : cinemas.get(0).getId());

        String selectedCinemaName = "Chưa chọn rạp";
        List<CinemaRoom> rooms = List.of();

        if (selectedCinemaId > 0) {
            Cinema c = cinemaService.getCinemaById(selectedCinemaId);
            if (c != null) {
                selectedCinemaName = c.getCinemaName();
                rooms = roomService.getRoomsByCinemaId(selectedCinemaId);
            }
        }

        // Lấy danh sách phim theo từng trạng thái để làm UI OptGroup (nhóm)
        List<Movie> allMovies = movieService.getAllMovies();
        List<Movie> moviesDangChieu = allMovies.stream().filter(m -> "Đang chiếu".equals(m.getStatus())).collect(java.util.stream.Collectors.toList());
        List<Movie> moviesSapChieu = allMovies.stream().filter(m -> "Sắp chiếu".equals(m.getStatus())).collect(java.util.stream.Collectors.toList());
        List<Movie> moviesDacBiet = allMovies.stream().filter(m -> "Suất chiếu đặc biệt".equals(m.getStatus())).collect(java.util.stream.Collectors.toList());

        model.addAttribute("cinemas", cinemas);
        model.addAttribute("moviesDangChieu", moviesDangChieu);
        model.addAttribute("moviesSapChieu", moviesSapChieu);
        model.addAttribute("moviesDacBiet", moviesDacBiet);
        model.addAttribute("rooms", rooms);
        model.addAttribute("showtimes", showtimeService.getSchedule(selectedCinemaId, selectedDate));

        model.addAttribute("selectedCinemaId", selectedCinemaId);
        model.addAttribute("selectedCinemaName", selectedCinemaName);
        model.addAttribute("selectedDate", selectedDate);

        // QUAN TRỌNG: Tên file HTML phải chính xác
        return "admin/manager_showtime";
    }

    /**
     * 2. API thêm suất chiếu mới
     */
    @PostMapping("/api/add")
    @ResponseBody
    public ResponseEntity<?> add(@RequestBody Map<String, Object> payload) {
        try {
            // Debug log
            System.out.println("Add Payload: " + payload);

            Showtime s = new Showtime();

            // 1. Kiểm tra và set Phòng
            if (payload.get("roomId") == null || payload.get("roomId").toString().isEmpty()) {
                throw new Exception("Vui lòng chọn phòng chiếu!");
            }
            int roomId = Integer.parseInt(payload.get("roomId").toString());
            CinemaRoom room = roomService.getRoomById(roomId);
            if (room == null) throw new Exception("Phòng chiếu không tồn tại!");
            s.setRoom(room);

            // 2. Kiểm tra và set Phim
            if (payload.get("movieId") == null || payload.get("movieId").toString().isEmpty()) {
                throw new Exception("Vui lòng chọn phim!");
            }
            int movieId = Integer.parseInt(payload.get("movieId").toString());
            Movie movie = movieService.getMovieById(movieId);
            if (movie == null) throw new Exception("Phim không tồn tại!");
            s.setMovie(movie);

            // 3. Date & Time
            if (payload.get("date") == null) throw new Exception("Chưa chọn ngày!");
            if (payload.get("time") == null) throw new Exception("Chưa chọn giờ!");

            s.setStartDate(LocalDate.parse(payload.get("date").toString()));
            s.setStartTime(LocalTime.parse(payload.get("time").toString()));

            // Lưu (Service sẽ tự check trùng lịch và ném Exception nếu trùng)
            showtimeService.saveShowtime(s);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            // Trả về đúng message mà Service ném ra (Vd: "Không thể tạo suất chiếu trong quá khứ!")
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 3. API Cập nhật suất chiếu
     */
    @PostMapping("/api/update")
    @ResponseBody
    public ResponseEntity<?> update(@RequestBody Map<String, Object> payload) {
        try {
            // Debug log
            System.out.println("Update Payload: " + payload);

            if (payload.get("id") == null) throw new Exception("Không tìm thấy ID suất chiếu!");
            int id = Integer.parseInt(payload.get("id").toString());

            Showtime s = showtimeService.getShowtimeById(id);
            if (s == null) throw new Exception("Suất chiếu không tồn tại!");

            // Cập nhật thông tin mới
            if (payload.get("roomId") != null) {
                int roomId = Integer.parseInt(payload.get("roomId").toString());
                s.setRoom(roomService.getRoomById(roomId));
            }

            if (payload.get("movieId") != null) {
                int movieId = Integer.parseInt(payload.get("movieId").toString());
                s.setMovie(movieService.getMovieById(movieId));
            }

            if (payload.get("date") != null) {
                s.setStartDate(LocalDate.parse(payload.get("date").toString()));
            }

            if (payload.get("time") != null) {
                s.setStartTime(LocalTime.parse(payload.get("time").toString()));
            }

            // Lưu cập nhật (Service sẽ check trùng lịch, trừ ID hiện tại ra)
            showtimeService.updateShowtime(s);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            // Trả về đúng message mà Service ném ra (Vd: "Không thể tạo suất chiếu trong quá khứ!")
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 4. API Xóa suất chiếu
     */
    @PostMapping("/api/delete")
    @ResponseBody
    public ResponseEntity<?> delete(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("id") == null) throw new Exception("ID không hợp lệ!");
            int id = Integer.parseInt(payload.get("id").toString());

            showtimeService.deleteShowtime(id);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 5. API Tạo xuất chiếu tự động hàng loạt
     */
    @PostMapping("/api/auto-add-bulk")
    @ResponseBody
    public ResponseEntity<?> autoAddBulk(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("Auto Add Bulk Payload: " + payload);

            if (payload.get("roomId") == null || payload.get("movieId") == null) {
                throw new Exception("Thiếu thông tin phòng hoặc phim!");
            }

            int roomId = Integer.parseInt(payload.get("roomId").toString());
            int movieId = Integer.parseInt(payload.get("movieId").toString());

            CinemaRoom room = roomService.getRoomById(roomId);
            Movie movie = movieService.getMovieById(movieId);

            if (room == null || movie == null) {
                throw new Exception("Phòng chiếu hoặc Phim không hợp lệ!");
            }

            LocalDate startDate = LocalDate.parse(payload.get("startDate").toString());
            LocalDate endDate = LocalDate.parse(payload.get("endDate").toString());
            LocalTime startTime = LocalTime.parse(payload.get("startTime").toString());
            LocalTime endTimeLimit = LocalTime.parse(payload.get("endTimeLimit").toString());
            int gapMinutes = Integer.parseInt(payload.get("gapMinutes").toString());

            if (endDate.isBefore(startDate)) {
                throw new Exception("Ngày kết thúc không được nhỏ hơn ngày bắt đầu!");
            }

            int addedCount = showtimeService.autoGenerateShowtimes(
                room, movie, startDate, endDate, startTime, endTimeLimit, gapMinutes
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã tạo thành công " + addedCount + " suất chiếu mới!"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}