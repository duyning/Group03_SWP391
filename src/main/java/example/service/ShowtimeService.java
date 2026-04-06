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
import java.time.LocalTime;
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

        // --- BỔ SUNG: Kiểm tra trạng thái Rạp và Phòng chiếu ---
        if (s.getRoom() != null) {
            String roomStatus = s.getRoom().getStatus();
            if ("Bảo trì".equalsIgnoreCase(roomStatus) || "Tạm ngưng".equalsIgnoreCase(roomStatus)) {
                throw new Exception("Lỗi! Phòng " + s.getRoom().getRoomName() + " đang " + roomStatus + ".");
            }
            if (s.getRoom().getCinema() != null) {
                String cinemaStatus = s.getRoom().getCinema().getStatus();
                if ("Bảo trì".equalsIgnoreCase(cinemaStatus) || "Tạm ngưng".equalsIgnoreCase(cinemaStatus)) {
                    throw new Exception("Lỗi! Rạp " + s.getRoom().getCinema().getCinemaName() + " đang " + cinemaStatus + ".");
                }
            }
        }
        // ----------------------------------------------------

        calculateEndTime(s);

        if (checkOverlap(s, 0)) {
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

        // --- BỔ SUNG: Kiểm tra trạng thái Rạp và Phòng chiếu ---
        if (s.getRoom() != null) {
            String roomStatus = s.getRoom().getStatus();
            if ("Bảo trì".equalsIgnoreCase(roomStatus) || "Tạm ngưng".equalsIgnoreCase(roomStatus)) {
                throw new Exception("Lỗi! Phòng " + s.getRoom().getRoomName() + " đang " + roomStatus + ".");
            }
            if (s.getRoom().getCinema() != null) {
                String cinemaStatus = s.getRoom().getCinema().getStatus();
                if ("Bảo trì".equalsIgnoreCase(cinemaStatus) || "Tạm ngưng".equalsIgnoreCase(cinemaStatus)) {
                    throw new Exception("Lỗi! Rạp " + s.getRoom().getCinema().getCinemaName() + " đang " + cinemaStatus + ".");
                }
            }
        }
        // ----------------------------------------------------

        calculateEndTime(s);

        if (checkOverlap(s, s.getId())) {
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

    // Hàm check trùng lịch tuyệt đối (Xử lý được cả phim vắt qua 12h đêm)
    private boolean checkOverlap(Showtime newShowtime, int excludeId) {
        LocalDate date1 = newShowtime.getStartDate();
        LocalDate date2 = date1.minusDays(1);
        
        // Lấy danh sách suất chiếu của phòng trong 2 ngày (ngày hiện tại và ngày trước đó)
        List<Showtime> existingShows = showtimeRepository.findForOverlapCheck(newShowtime.getRoom().getId(), date1, date2);

        LocalDateTime newStartDT = LocalDateTime.of(newShowtime.getStartDate(), newShowtime.getStartTime());
        LocalDateTime newEndDT = newShowtime.getEndTime().isBefore(newShowtime.getStartTime()) 
                ? LocalDateTime.of(newShowtime.getStartDate().plusDays(1), newShowtime.getEndTime()) 
                : LocalDateTime.of(newShowtime.getStartDate(), newShowtime.getEndTime());

        for (Showtime ex : existingShows) {
            if (excludeId != 0 && ex.getId() == excludeId) continue;

            LocalDateTime exStartDT = LocalDateTime.of(ex.getStartDate(), ex.getStartTime());
            LocalDateTime exEndDT = ex.getEndTime().isBefore(ex.getStartTime()) 
                    ? LocalDateTime.of(ex.getStartDate().plusDays(1), ex.getEndTime()) 
                    : LocalDateTime.of(ex.getStartDate(), ex.getEndTime());

            // Thuật toán kiểm tra giao nhau khoảng thời gian: StartA < EndB && StartB < EndA
            if (newStartDT.isBefore(exEndDT) && exStartDT.isBefore(newEndDT)) {
                return true;
            }
        }
        return false;
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

    // --- HÀM TẠO SUẤT CHIẾU TỰ ĐỘNG HÀNG LOẠT ---
    @Transactional
    public int autoGenerateShowtimes(example.entity.CinemaRoom room, example.entity.Movie movie, 
                                     LocalDate startDate, LocalDate endDate,
                                     LocalTime startTime, LocalTime endTimeLimit, int gapMinutes) throws Exception {

        // Lấy thời lượng phim
        String durationStr = movie.getDuration();
        int durationMins = 90; // Mặc định nếu không phân tích được
        if (durationStr != null && !durationStr.isEmpty()) {
            try {
                String numberOnly = durationStr.replaceAll("[^0-9]", "");
                if (!numberOnly.isEmpty()) durationMins = Integer.parseInt(numberOnly);
            } catch (Exception e) {}
        }

        int addedCount = 0;
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalTime currentTime = startTime;
            
            // Loop bảo vệ chống lặp vô hạn
            int safetyCounter = 0;
            while (safetyCounter++ < 50) {
                // Kiểm tra giới hạn thời gian (VD: không trễ hơn 23:00)
                if (currentTime.isAfter(endTimeLimit)) {
                    break;
                }
                
                Showtime s = new Showtime();
                s.setRoom(room);
                s.setMovie(movie);
                s.setStartDate(currentDate);
                s.setStartTime(currentTime);
                
                // Logic gốc: EndTime = StartTime + Duration + 15'
                LocalTime endTime = currentTime.plusMinutes(durationMins + 15);
                s.setEndTime(endTime);
                s.setStatus("Hoạt động");
                
                // Nếu bị trùng với một lịch khác đã tồn tại
                if (checkOverlap(s, 0)) {
                    // Tịnh tiến thời gian lên 15 phút để dò tìm khe hở kế tiếp
                    LocalTime nextTime = currentTime.plusMinutes(15);
                    if (nextTime.isBefore(currentTime)) break; // Rollover qua ngày mới thì dừng
                    currentTime = nextTime;
                    continue; 
                }
                
                // Lưu thành công
                showtimeRepository.save(s);
                addedCount++;
                
                // Chuẩn bị cho suất chiếu tiếp theo
                // Do EndTime đã tự cộng 15 phút, ta cấu trừ lại với Gap thực tế chuẩn bị
                LocalTime nextTime = endTime.plusMinutes(gapMinutes - 15);
                
                // Kiểm tra rollover (qua ngưỡng nửa đêm)
                if (nextTime.isBefore(currentTime)) {
                    break;
                }
                currentTime = nextTime;
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return addedCount;
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

    // --- HÀM LẤY DANH SÁCH PHIM KÈM SUẤT CHIẾU THEO RẠP VÀ NGÀY ---
    public List<Map<String, Object>> getMoviesWithShowtimes(int cinemaId, LocalDate date) {
        List<Showtime> showtimes = getSchedule(cinemaId, date);
        Map<Integer, Map<String, Object>> movieMap = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (Showtime s : showtimes) {
            // Lọc các suất chiếu đá qua
            if (date.isBefore(today)) continue;
            if (date.isEqual(today) && s.getStartTime().isBefore(now)) continue;

            example.entity.Movie movie = s.getMovie();
            Map<String, Object> movieData = movieMap.computeIfAbsent(movie.getId(), k -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", movie.getId());
                map.put("movieName", movie.getMovieName());
                map.put("imgUrl", movie.getImgUrl());
                map.put("type", movie.getType());
                map.put("duration", movie.getDuration());
                map.put("showtimes", new ArrayList<Map<String, Object>>());
                return map;
            });

            int totalSeats = seatRepository.findByRoomId(s.getRoom().getId()).size();
            int bookedSeats = ticketRepository.getBookedSeatIds(s.getId()).size();
            int seatsLeft = Math.max(0, totalSeats - bookedSeats);

            Map<String, Object> stMap = new HashMap<>();
            stMap.put("id", s.getId());
            stMap.put("time", s.getStartTime().toString().substring(0, 5));
            stMap.put("roomName", s.getRoom().getRoomName());
            stMap.put("seatsLeft", seatsLeft);

            ((List<Map<String, Object>>) movieData.get("showtimes")).add(stMap);
        }

        return new ArrayList<>(movieMap.values());
    }
}