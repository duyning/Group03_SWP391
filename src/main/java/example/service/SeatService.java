package example.service;

import example.entity.CinemaRoom;
import example.entity.Seat;
import example.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SeatService {

    @Autowired
    private SeatRepository seatRepository;

    /**
     * Lưu sơ đồ ghế cho phòng chiếu
     * - Xóa toàn bộ ghế cũ
     * - Lưu danh sách ghế mới
     */
    public void saveSeatLayout(int roomId, CinemaRoom room, List<Seat> seatList) {

        // 1. Xóa sơ đồ ghế cũ
        seatRepository.deleteByRoomId(roomId);

        // 2. Lưu ghế mới
        for (Seat seat : seatList) {
            seat.setCinemaRoom(room);

            // Nếu FE không gửi status thì set mặc định
            if (seat.getStatus() == null || seat.getStatus().isBlank()) {
                seat.setStatus("AVAILABLE");
            }

            // seatNumber đã được tự sinh bằng @PrePersist trong Entity
            seatRepository.save(seat);
        }
    }
}
