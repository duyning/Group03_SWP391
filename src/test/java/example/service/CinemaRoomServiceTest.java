package example.service;

import example.entity.CinemaRoom;
import example.repository.CinemaRoomRepository;
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
class CinemaRoomServiceTest {

    @Mock
    private CinemaRoomRepository roomRepository;

    @InjectMocks
    private CinemaRoomService roomService;

    @Test
    @DisplayName("Lấy danh sách các phòng chiếu theo Id của Rạp")
    void testGetRoomsByCinemaId() {
        when(roomRepository.findByCinemaId(1)).thenReturn(Arrays.asList(new CinemaRoom(), new CinemaRoom()));

        List<CinemaRoom> result = roomService.getRoomsByCinemaId(1);

        assertEquals(2, result.size());
        verify(roomRepository, times(1)).findByCinemaId(1);
    }

    @Test
    @DisplayName("Lấy phòng chiếu theo ID thành công")
    void testGetRoomById() {
        CinemaRoom room = new CinemaRoom();
        room.setId(10);
        when(roomRepository.findById(10)).thenReturn(room);

        CinemaRoom result = roomService.getRoomById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    @DisplayName("Lưu Phòng chiếu mới: Tự gán trạng thái Hoạt động nếu bị trống")
    void testSaveRoom_StatusEmpty() {
        CinemaRoom room = new CinemaRoom();
        room.setStatus(""); 
        
        doNothing().when(roomRepository).save(room);

        roomService.saveRoom(room);

        assertEquals("Hoạt động", room.getStatus());
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    @DisplayName("Lưu Phòng chiếu: Giữ nguyên trạng thái nếu đã có dữ liệu")
    void testSaveRoom_HasStatus() {
        CinemaRoom room = new CinemaRoom();
        room.setStatus("Bảo trì"); 
        
        doNothing().when(roomRepository).save(room);

        roomService.saveRoom(room);

        assertEquals("Bảo trì", room.getStatus());
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    @DisplayName("Cập nhật phòng chiếu thành công, sao chép các properties cơ bản sang Entity cũ để giữ Collection gốc")
    void testUpdateRoom_Success() {
        CinemaRoom updateRequest = new CinemaRoom();
        updateRequest.setId(1);
        updateRequest.setRoomName("Phòng VIP 1");
        updateRequest.setStatus("Bảo trì");
        updateRequest.setRoomType("IMAX");

        CinemaRoom existingRoom = new CinemaRoom();
        existingRoom.setId(1);
        existingRoom.setRoomName("Phòng cũ");

        when(roomRepository.findById(1)).thenReturn(existingRoom);

        roomService.updateRoom(updateRequest);

        // Xác nhận dữ liệu được copy ngược lại obj đang bám (existingRoom)
        assertEquals("Phòng VIP 1", existingRoom.getRoomName());
        assertEquals("Bảo trì", existingRoom.getStatus());
        assertEquals("IMAX", existingRoom.getRoomType());
        verify(roomRepository).save(existingRoom);
    }

    @Test
    @DisplayName("Không cập nhật phòng chiếu nếu dữ liệu ID truyền vào là ảo / không tìm thấy")
    void testUpdateRoom_NotFound() {
        CinemaRoom room = new CinemaRoom();
        room.setId(99);
        when(roomRepository.findById(99)).thenReturn(null);

        roomService.updateRoom(room);

        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Xóa Phòng chiếu thành công")
    void testDeleteRoom() {
        doNothing().when(roomRepository).delete(10);
        
        roomService.deleteRoom(10);
        
        verify(roomRepository, times(1)).delete(10);
    }

    @Test
    @DisplayName("Tìm kiếm Phòng chiếu động chuyển tiếp thành công xuống Repository")
    void testSearchRooms() {
        when(roomRepository.search(anyInt(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Arrays.asList(new CinemaRoom()));

        List<CinemaRoom> result = roomService.searchRooms(1, "R", "2D", 50, "Hoạt động");

        assertEquals(1, result.size());
        verify(roomRepository).search(1, "R", "2D", 50, "Hoạt động");
    }
}
