package example.service;

import example.entity.CinemaRoom;
import example.entity.Seat;
import example.repository.SeatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatService seatService;

    @Test
    @DisplayName("Lưu danh sách sơ đồ ghế thành công: Xóa ghế cũ và sinh ghế mới (bỏ trống status => gắn AVAILABLE)")
    void testSaveSeatLayout_EmptyStatusGivesAvailable() {
        int roomId = 1;
        CinemaRoom room = new CinemaRoom();
        room.setId(roomId);

        Seat seat1 = new Seat();
        seat1.setStatus(""); // Chuỗi rỗng
        
        Seat seat2 = new Seat();
        seat2.setStatus(null); // Null value

        List<Seat> seatList = Arrays.asList(seat1, seat2);

        doNothing().when(seatRepository).deleteByRoomId(roomId);
        doNothing().when(seatRepository).save(any(Seat.class));

        seatService.saveSeatLayout(roomId, room, seatList);

        // Verify that old seats were deleted
        verify(seatRepository, times(1)).deleteByRoomId(roomId);

        // Verify proper statuses were set
        assertEquals("AVAILABLE", seat1.getStatus());
        assertEquals("AVAILABLE", seat2.getStatus());
        
        // Ensure Room relation was formed
        assertEquals(room, seat1.getCinemaRoom());
        assertEquals(room, seat2.getCinemaRoom());

        // Verify save was called for each element
        verify(seatRepository, times(2)).save(any(Seat.class));
    }

    @Test
    @DisplayName("Lưu sơ đồ ghế: Nếu ghế đã truyền trạng thái thì giữ nguyên")
    void testSaveSeatLayout_StatusProvided() {
        int roomId = 2;
        CinemaRoom room = new CinemaRoom();
        
        Seat seat1 = new Seat();
        seat1.setStatus("MAINTENANCE"); 

        doNothing().when(seatRepository).deleteByRoomId(roomId);
        doNothing().when(seatRepository).save(seat1);

        seatService.saveSeatLayout(roomId, room, Arrays.asList(seat1));

        // Delete should fire
        verify(seatRepository, times(1)).deleteByRoomId(roomId);

        // Status must not be overwritten
        assertEquals("MAINTENANCE", seat1.getStatus());
        
        // Save must fire
        verify(seatRepository, times(1)).save(seat1);
    }
}
