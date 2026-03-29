package example.service;

import example.entity.CinemaRoom;
import example.repository.CinemaRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CinemaRoomService {

    @Autowired
    private CinemaRoomRepository roomRepository;

    public List<CinemaRoom> getRoomsByCinemaId(int cinemaId) {
        // Repository hiện đã dùng JOIN FETCH seats nên dữ liệu sẽ đầy đủ
        return roomRepository.findByCinemaId(cinemaId);
    }

    public CinemaRoom getRoomById(int id) {
        return roomRepository.findById(id);
    }

    @Transactional
    public void saveRoom(CinemaRoom room) {
        // Nếu là phòng mới, đặt mặc định trạng thái nếu chưa có
        if (room.getStatus() == null || room.getStatus().trim().isEmpty()) {
            room.setStatus("Hoạt động");
        }
        // Lưu ý: Không còn room.setTotalSeats(0) vì Entity đã bỏ thuộc tính này
        roomRepository.save(room);
    }

    @Transactional
    public void updateRoom(CinemaRoom room) {
        // 1. Lấy đối tượng đang tồn tại trong DB (Đã bao gồm fetch seats từ Repository)
        CinemaRoom existingRoom = roomRepository.findById(room.getId());

        if (existingRoom != null) {
            // 2. Cập nhật các thông tin cơ bản
            existingRoom.setRoomName(room.getRoomName());
            existingRoom.setRoomType(room.getRoomType());
            existingRoom.setStatus(room.getStatus());
            existingRoom.setCinema(room.getCinema());

            // 3. Thực hiện merge thông qua repository
            roomRepository.save(existingRoom);
        }
    }

    @Transactional
    public void deleteRoom(int id) {
        roomRepository.delete(id);
    }

    /**
     * Tìm kiếm phòng chiếu.
     * Lưu ý: Việc lọc minSeats hiện được xử lý trong Repository sau khi fetch danh sách ghế.
     */
    public List<CinemaRoom> searchRooms(int cinemaId, String roomName, String roomType, Integer minSeats, String status) {
        return roomRepository.search(cinemaId, roomName, roomType, minSeats, status);
    }
}