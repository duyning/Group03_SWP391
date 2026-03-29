package example.service;

import example.entity.Seat;
import example.entity.Showtime;
import example.entity.TicketPrice;
import example.repository.SeatRepository;
import example.repository.ShowtimeRepository;
import example.repository.TicketPriceRepository;
import example.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShowtimeService {
    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketPriceRepository ticketPriceRepository;

    public List<Showtime> getSchedule(int cinemaId, LocalDate date) {
        return showtimeRepository.findByCinemaAndDate(cinemaId, date);
    }

    @Transactional
    public void saveShowtime(Showtime s) throws Exception {
        // --- BỔ SUNG: Kiểm tra thời gian thực ---
        if (showtimeRepository.isPastDateTime(s.getStartDate(), s.getStartTime())) {
            throw new Exception("Không thể tạo suất chiếu trong quá khứ! Vui lòng chọn thời gian sau thời điểm hiện tại.");
        }
        // ----------------------------------------

        calculateEndTime(s);

        if (showtimeRepository.checkOverlap(s.getRoom().getId(), s.getStartDate(), s.getStartTime(), s.getEndTime(), 0)) {
            throw new Exception("Lỗi trùng lịch! Đã có suất chiếu khác tại phòng " + s.getRoom().getRoomName());
        }

        if (s.getStatus() == null || s.getStatus().isEmpty()) {
            s.setStatus("Hoạt động");
        }

        showtimeRepository.save(s);
    }

    // Thêm vào class ShowtimeService
    @Transactional
    public void updateShowtime(Showtime s) throws Exception {
        // --- BỔ SUNG: Kiểm tra thời gian thực khi cập nhật ---
        if (showtimeRepository.isPastDateTime(s.getStartDate(), s.getStartTime())) {
            throw new Exception("Thời gian cập nhật không hợp lệ (nằm trong quá khứ).");
        }
        // ----------------------------------------------------

        calculateEndTime(s);

        if (showtimeRepository.checkOverlap(s.getRoom().getId(), s.getStartDate(), s.getStartTime(), s.getEndTime(), s.getId())) {
            throw new Exception("Lỗi trùng lịch! Đã có suất chiếu khác trong khung giờ này.");
        }

        showtimeRepository.save(s);
    }

    // Hàm phụ trợ tính giờ kết thúc
    private void calculateEndTime(Showtime s) {
        String durationStr = s.getMovie().getDuration();
        int minutes = 90; // Mặc định
        if (durationStr != null && !durationStr.isEmpty()) {
            try {
                String numberOnly = durationStr.replaceAll("[^0-9]", "");
                if (!numberOnly.isEmpty()) minutes = Integer.parseInt(numberOnly);
            } catch (Exception e) {}
        }
        // EndTime = StartTime + Duration + 15p nghỉ
        s.setEndTime(s.getStartTime().plusMinutes(minutes + 15));
    }

    public Showtime getShowtimeById(int id) {
        return showtimeRepository.findById(id);
    }

    @Transactional
    public void deleteShowtime(int id) {
        Showtime s = showtimeRepository.findById(id);
        if (s != null) {
            showtimeRepository.delete(s);
        }
    }

    public List<Showtime> findShowtimesForBooking(int movieId, int cinemaId, LocalDate date) {
        return showtimeRepository.findForBooking(movieId, cinemaId, date);
    }

    // --- HÀM LẤY SƠ ĐỒ GHẾ (Trả về List Map) ---
    public List<Map<String, Object>> getSeatMap(int showtimeId) {
        // 1. Lấy thông tin suất chiếu
        Showtime showtime = showtimeRepository.findById(showtimeId);
        if (showtime == null) return new ArrayList<>();

        // 2. Tính toán thời gian (Ngày thường/Cuối tuần, Sau 22h)
        LocalDateTime startDateTime = LocalDateTime.of(showtime.getStartDate(), showtime.getStartTime());
        boolean isWeekend = checkIsWeekend(startDateTime);
        boolean isAfter22h = startDateTime.getHour() >= 22;

        // 3. Lấy dữ liệu cần thiết
        List<Seat> allSeats = seatRepository.findByRoomId(showtime.getRoom().getId());
        List<Integer> bookedSeatIds = ticketRepository.getBookedSeatIds(showtimeId);

        // Lấy toàn bộ bảng giá
        List<TicketPrice> priceList = ticketPriceRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Seat s : allSeats) {
            Map<String, Object> map = new HashMap<>();

            map.put("id", s.getId());
            map.put("code", s.getSeatNumber()); // A1, A2...
            map.put("type", s.getSeatType());   // NORMAL, VIP...

            // --- TÍNH GIÁ ĐỘNG ---
            double finalPrice = calculatePrice(s, priceList, isWeekend, isAfter22h);
            map.put("price", finalPrice);
            // ---------------------

            // Kiểm tra trạng thái booked
            boolean isBooked = bookedSeatIds.contains(s.getId());
            if ("MAINTENANCE".equalsIgnoreCase(s.getStatus())) {
                isBooked = true;
            }
            map.put("isBooked", isBooked);

            result.add(map);
        }

        return result;
    }

    // --- (3) HÀM LOGIC TÍNH GIÁ (Thêm mới) ---
    private double calculatePrice(Seat seat, List<TicketPrice> priceList, boolean isWeekend, boolean isAfter22h) {
        // 1. Mapping loại ghế từ Seat (Tiếng Anh/Code/Tiếng Việt) sang TicketPrice
        String dbSeatType = seat.getSeatType();
        String targetType = "Ghế Thường"; // Mặc định

        if (dbSeatType != null) {
            String upperType = dbSeatType.toUpperCase();

            // Bắt các từ khóa của ghế VIP
            if (upperType.contains("VIP")) {
                targetType = "Ghế VIP";
            }
            // Bắt các từ khóa của ghế Đôi (Thêm chữ "ĐÔI" có dấu)
            else if (upperType.contains("DOUBLE") || upperType.contains("SWEET") || upperType.contains("DOI") || upperType.contains("ĐÔI")) {
                targetType = "Ghế Đôi";
            }
        }

        // 2. Tìm cấu hình giá phù hợp
        String finalTargetType = targetType;

        Optional<TicketPrice> config = priceList.stream()
                .filter(tp -> {
                    // Kiểm tra active
                    if (tp.getActive() == null || !tp.getActive()) return false;

                    // Kiểm tra đúng loại ghế (Ghế Đôi == Ghế Đôi)
                    if (!tp.getSeatType().equalsIgnoreCase(finalTargetType)) return false;

                    // Kiểm tra tên vé:
                    // Vì trong DB của bạn (ảnh gửi), Ghế Đôi vẫn đặt tên là "Vé Người Lớn"
                    // Nên ta chỉ cần kiểm tra xem tên vé có chứa "Người Lớn" hay không là đủ.
                    String name = tp.getTicketName().toLowerCase();
                    return name.contains("người lớn");
                })
                .findFirst();

        // 3. Trả về giá tiền
        if (config.isPresent()) {
            TicketPrice tp = config.get();
             if (isWeekend) {
                return tp.getPriceWeekend();
            } else {
                return tp.getPriceStandard();
            }
        }

        return 0.0;
    }

    private boolean checkIsWeekend(LocalDateTime date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}