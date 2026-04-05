package example.service;

import example.entity.Cinema;
import example.repository.CinemaRepository;
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
class CinemaServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;

    @InjectMocks
    private CinemaService cinemaService;

    @Test
    @DisplayName("Lấy danh sách tất cả Rạp thành công")
    void testGetAllCinemas() {
        when(cinemaRepository.findAll()).thenReturn(Arrays.asList(new Cinema(), new Cinema()));

        List<Cinema> result = cinemaService.getAllCinemas();

        assertEquals(2, result.size());
        verify(cinemaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Lưu Rạp thành công: Tự động set trạng thái mặc định nếu rỗng")
    void testSaveCinema_StatusNull() {
        Cinema cinema = new Cinema();
        cinema.setStatus(null); // Không truyền status
        
        // Cần đảm bảo hàm mock repository.save nhận được entity có status = "Hoạt động"
        doNothing().when(cinemaRepository).save(any(Cinema.class));

        cinemaService.saveCinema(cinema);

        assertEquals("Hoạt động", cinema.getStatus());
        verify(cinemaRepository, times(1)).save(cinema);
    }

    @Test
    @DisplayName("Lưu Rạp thành công: Giữ nguyên trạng thái nếu đã có")
    void testSaveCinema_WithStatus() {
        Cinema cinema = new Cinema();
        cinema.setStatus("Bảo trì");

        doNothing().when(cinemaRepository).save(cinema);

        cinemaService.saveCinema(cinema);

        assertEquals("Bảo trì", cinema.getStatus());
        verify(cinemaRepository, times(1)).save(cinema);
    }

    @Test
    @DisplayName("Tìm kiếm Rạp với các biến cần trim() khoảng trắng")
    void testSearchCinemas_WithTrim() {
        when(cinemaRepository.searchCinemas(anyString(), any(), any(), anyString(), anyInt(), anyString()))
                .thenReturn(Arrays.asList(new Cinema()));

        // Tên " Rạp 1 ", address "  Hà Nội  ", phone " 0988 " (có khoảng trắng)
        List<Cinema> result = cinemaService.searchCinemas(" Rạp 1 ", "Hà Nội", "Hoạt động", "  Ba Đình  ", 3, " 0988 ");

        assertEquals(1, result.size());
        verify(cinemaRepository).searchCinemas("Rạp 1", "Hà Nội", "Hoạt động", "Ba Đình", 3, "0988");
    }

    @Test
    @DisplayName("Lấy rạp theo ID thành công")
    void testGetCinemaById() {
        Cinema cinema = new Cinema();
        cinema.setId(1);
        when(cinemaRepository.findById(1)).thenReturn(cinema);

        Cinema result = cinemaService.getCinemaById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(cinemaRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Xóa rạp thành công")
    void testDeleteCinema() {
        doNothing().when(cinemaRepository).delete(10);
        
        cinemaService.deleteCinema(10);
        
        verify(cinemaRepository, times(1)).delete(10);
    }

    @Test
    @DisplayName("Lấy danh sách các rạp có phân trang")
    void testGetAllCinemasPaged() {
        when(cinemaRepository.findAllPaged(1, 10)).thenReturn(Arrays.asList(new Cinema(), new Cinema()));

        List<Cinema> result = cinemaService.getAllCinemasPaged(1, 10);

        assertEquals(2, result.size());
        verify(cinemaRepository).findAllPaged(1, 10);
    }

    @Test
    @DisplayName("Phân trang: Kiểm tra xử lý nếu trang nhỏ hơn 1 được đưa về trang 1")
    void testGetAllCinemasPaged_PageLessThanOne() {
        when(cinemaRepository.findAllPaged(1, 10)).thenReturn(Arrays.asList(new Cinema()));

        List<Cinema> result = cinemaService.getAllCinemasPaged(-5, 10);

        assertEquals(1, result.size());
        verify(cinemaRepository).findAllPaged(1, 10);
    }

    @Test
    @DisplayName("Đếm tổng số Rạp thành công")
    void testGetTotalCount() {
        when(cinemaRepository.getTotalCount()).thenReturn(50L);

        long count = cinemaService.getTotalCount();

        assertEquals(50L, count);
        verify(cinemaRepository, times(1)).getTotalCount();
    }
}
