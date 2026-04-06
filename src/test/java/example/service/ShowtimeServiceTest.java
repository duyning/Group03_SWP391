package example.service;

import example.entity.CinemaRoom;
import example.entity.Movie;
import example.entity.Showtime;
import example.repository.ShowtimeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @InjectMocks
    private ShowtimeService showtimeService;

    // ==========================================
    // TEST CHO HÀM: saveShowtime() (THÊM LỊCH CHIẾU)
    // ==========================================

    @Test
    @DisplayName("UTCID01: Thêm suất chiếu thành công (Normal)")
    void testSaveShowtime_Success() throws Exception {
        // Arrange
        Showtime s = new Showtime();
        s.setStartDate(LocalDate.now().plusDays(1));
        s.setStartTime(LocalTime.of(10, 0));
        
        Movie movie = new Movie();
        movie.setDuration("120 phút");
        s.setMovie(movie);
        
        CinemaRoom room = new CinemaRoom();
        room.setId(1);
        room.setRoomName("Phòng 1");
        s.setRoom(room);

        Mockito.when(showtimeRepository.isPastDateTime(any(), any())).thenReturn(false);
        Mockito.when(showtimeRepository.findForOverlapCheck(anyInt(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        Mockito.doNothing().when(showtimeRepository).save(any(Showtime.class));

        // Act & Assert
        assertDoesNotThrow(() -> showtimeService.saveShowtime(s));
        assertEquals("Hoạt động", s.getStatus());
        assertNotNull(s.getEndTime()); // Ensures calculateEndTime was called
        
        Mockito.verify(showtimeRepository, Mockito.times(1)).save(s);
    }

    @Test
    @DisplayName("UTCID02: Báo lỗi khi thêm suất chiếu trong quá khứ (Abnormal)")
    void testSaveShowtime_PastTime() {
        // Arrange
        Showtime s = new Showtime();
        Mockito.when(showtimeRepository.isPastDateTime(any(), any())).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> showtimeService.saveShowtime(s));
        assertEquals("Không thể tạo suất chiếu trong quá khứ! Vui lòng chọn thời gian sau thời điểm hiện tại.", exception.getMessage());
        
        Mockito.verify(showtimeRepository, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("UTCID03: Báo lỗi trùng lịch chiếu (Abnormal)")
    void testSaveShowtime_Overlap() {
        // Arrange
        Showtime s = new Showtime();
        s.setStartDate(LocalDate.now().plusDays(1));
        s.setStartTime(LocalTime.of(10, 0));
        
        Movie movie = new Movie();
        movie.setDuration("120 phút");
        s.setMovie(movie);
        
        CinemaRoom room = new CinemaRoom();
        room.setId(1);
        room.setRoomName("Phòng 1");
        s.setRoom(room);

        Mockito.when(showtimeRepository.isPastDateTime(any(), any())).thenReturn(false);
        // Simulate overlapping
        Showtime overlapping = new Showtime();
        overlapping.setId(999);
        overlapping.setStartDate(s.getStartDate());
        overlapping.setStartTime(LocalTime.of(9, 0));
        overlapping.setEndTime(LocalTime.of(11, 0));
        Mockito.when(showtimeRepository.findForOverlapCheck(anyInt(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.singletonList(overlapping));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> showtimeService.saveShowtime(s));
        assertEquals("Lỗi trùng lịch! Đã có suất chiếu khác tại phòng Phòng 1", exception.getMessage());
        
        Mockito.verify(showtimeRepository, Mockito.never()).save(any());
    }

    // ==========================================
    // TEST CHO HÀM: deleteShowtime() (XÓA LỊCH CHIẾU - MÔ PHỎNG THEO ẢNH PHÂN CẦN)
    // ==========================================

    @Test
    @DisplayName("UTCID04: Xóa suất chiếu thành công khi tìm thấy ID (Normal / Điều kiện 1)")
    void testDeleteShowtime_WhenShowtimeExists() {
        // Arrange
        Showtime existingShowtime = new Showtime();
        existingShowtime.setId(1);

        Mockito.when(showtimeRepository.findById(1)).thenReturn(existingShowtime);
        Mockito.doNothing().when(showtimeRepository).delete(existingShowtime);

        // Act
        assertDoesNotThrow(() -> showtimeService.deleteShowtime(1));

        // Assert
        Mockito.verify(showtimeRepository, Mockito.times(1)).findById(1);
        Mockito.verify(showtimeRepository, Mockito.times(1)).delete(existingShowtime);
    }

    @Test
    @DisplayName("UTCID05: Không xóa gì cả khi ID lịch chiếu không tồn tại (Abnormal / Điều kiện 2)")
    void testDeleteShowtime_WhenShowtimeDoesNotExist() {
        // Arrange: Giả lập DB không tìm thấy (return null)
        Mockito.when(showtimeRepository.findById(99)).thenReturn(null);

        // Act
        assertDoesNotThrow(() -> showtimeService.deleteShowtime(99));

        // Assert: Không chạy hàm delete
        Mockito.verify(showtimeRepository, Mockito.times(1)).findById(99);
        Mockito.verify(showtimeRepository, Mockito.never()).delete(any());
    }

    @Test
    @DisplayName("UTCID06: Lỗi kết nối server văng ngoại lệ khi cố xóa (Abnormal / Điều kiện 4)")
    void testDeleteShowtime_FailWhenDatabaseError() {
        // Arrange
        Showtime existingShowtime = new Showtime();
        existingShowtime.setId(1);

        Mockito.when(showtimeRepository.findById(1)).thenReturn(existingShowtime);
        // Simulate Server Connection Error
        Mockito.doThrow(new RuntimeException("Lỗi kết nối server")).when(showtimeRepository).delete(existingShowtime);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> showtimeService.deleteShowtime(1));
        assertEquals("Lỗi kết nối server", exception.getMessage());

        Mockito.verify(showtimeRepository, Mockito.times(1)).findById(1);
        Mockito.verify(showtimeRepository, Mockito.times(1)).delete(existingShowtime);
    }

    // ==========================================
    // TEST CHO HÀM: updateShowtime() (CẬP NHẬT LỊCH CHIẾU)
    // ==========================================

    @Test
    @DisplayName("UTCID07: Cập nhật suất chiếu thành công (Normal)")
    void testUpdateShowtime_Success() throws Exception {
        // Arrange
        Showtime s = new Showtime();
        s.setId(1);
        s.setStartDate(LocalDate.now().plusDays(2));
        s.setStartTime(LocalTime.of(14, 0));
        
        Movie movie = new Movie();
        movie.setDuration("100 phút");
        s.setMovie(movie);
        
        CinemaRoom room = new CinemaRoom();
        room.setId(2);
        room.setRoomName("Phòng 2");
        s.setRoom(room);

        // Pass time valid + no overlap
        Mockito.when(showtimeRepository.isPastDateTime(any(), any())).thenReturn(false);
        Mockito.when(showtimeRepository.findForOverlapCheck(anyInt(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        Mockito.doNothing().when(showtimeRepository).save(any(Showtime.class));

        // Act
        showtimeService.updateShowtime(s);

        // Assert
        Mockito.verify(showtimeRepository, Mockito.times(1)).save(s);
    }

}
