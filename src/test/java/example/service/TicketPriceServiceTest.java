package example.service;

import example.entity.TicketPrice;
import example.repository.TicketPriceRepository;
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
class TicketPriceServiceTest {

    @Mock
    private TicketPriceRepository ticketPriceRepository;

    @InjectMocks
    private TicketPriceService ticketPriceService;

    @Test
    @DisplayName("Lấy danh sách tất cả giá vé thành công")
    void testGetAll() {
        when(ticketPriceRepository.findAll()).thenReturn(Arrays.asList(new TicketPrice(), new TicketPrice()));

        List<TicketPrice> result = ticketPriceService.getAll();

        assertEquals(2, result.size());
        verify(ticketPriceRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Lưu giá vé mới thành công")
    void testSave_New() {
        TicketPrice tp = new TicketPrice();
        // ID is null by default
        
        ticketPriceService.save(tp);

        verify(ticketPriceRepository, never()).findById(any());
        verify(ticketPriceRepository, times(1)).save(tp);
    }

    @Test
    @DisplayName("Cập nhật giá vé thành công (giữ nguyên trạng thái active cũ nếu update null active)")
    void testSave_UpdateKeepActive() {
        TicketPrice updateTp = new TicketPrice();
        updateTp.setId(1);
        updateTp.setActive(null);

        TicketPrice existingTp = new TicketPrice();
        existingTp.setId(1);
        existingTp.setActive(true);

        when(ticketPriceRepository.findById(1)).thenReturn(existingTp);

        ticketPriceService.save(updateTp);

        assertTrue(updateTp.getActive());
        verify(ticketPriceRepository).findById(1);
        verify(ticketPriceRepository).save(updateTp);
    }

    @Test
    @DisplayName("Xóa giá vé thành công")
    void testDelete() {
        ticketPriceService.delete(1);
        verify(ticketPriceRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Cập nhật trạng thái giá vé thành công")
    void testUpdateStatus_Success() {
        TicketPrice existingTp = new TicketPrice();
        existingTp.setId(1);
        existingTp.setActive(false);

        when(ticketPriceRepository.findById(1)).thenReturn(existingTp);

        ticketPriceService.updateStatus(1, true);

        assertTrue(existingTp.getActive());
        verify(ticketPriceRepository).save(existingTp);
    }

    @Test
    @DisplayName("Không cập nhật trạng thái khi không tìm thấy giá vé")
    void testUpdateStatus_NotFound() {
        when(ticketPriceRepository.findById(99)).thenReturn(null);

        ticketPriceService.updateStatus(99, true);

        verify(ticketPriceRepository, never()).save(any());
    }
}
