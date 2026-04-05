package example.repository;

import example.entity.TicketPrice;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
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
class TicketPriceRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<TicketPrice> query;

    @InjectMocks
    private TicketPriceRepository ticketPriceRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Lấy danh sách tất cả giá vé thành công")
    void testFindAll() {
        when(session.createQuery("FROM TicketPrice", TicketPrice.class)).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(new TicketPrice(), new TicketPrice()));

        List<TicketPrice> result = ticketPriceRepository.findAll();

        assertEquals(2, result.size());
        verify(session).createQuery("FROM TicketPrice", TicketPrice.class);
    }

    @Test
    @DisplayName("Tìm giá vé theo ID thành công")
    void testFindById() {
        TicketPrice tp = new TicketPrice();
        tp.setId(1);
        when(session.get(TicketPrice.class, 1)).thenReturn(tp);

        TicketPrice result = ticketPriceRepository.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    @DisplayName("Lưu giá vé mới thành công (dùng persist)")
    void testSaveNew() {
        TicketPrice tp = new TicketPrice();
        tp.setId(null);

        ticketPriceRepository.save(tp);

        verify(session, times(1)).persist(tp);
        verify(session, never()).merge(any());
    }

    @Test
    @DisplayName("Cập nhật giá vé thành công (dùng merge)")
    void testSaveUpdate() {
        TicketPrice tp = new TicketPrice();
        tp.setId(1);

        ticketPriceRepository.save(tp);

        verify(session, times(1)).merge(tp);
        verify(session, never()).persist(any());
    }

    @Test
    @DisplayName("Xóa giá vé theo ID thành công")
    void testDeleteById_Success() {
        TicketPrice tp = new TicketPrice();
        tp.setId(1);
        when(session.get(TicketPrice.class, 1)).thenReturn(tp);

        ticketPriceRepository.deleteById(1);

        verify(session, times(1)).remove(tp);
    }

    @Test
    @DisplayName("Không xóa khi ID giá vé không tồn tại")
    void testDeleteById_NotFound() {
        when(session.get(TicketPrice.class, 99)).thenReturn(null);

        ticketPriceRepository.deleteById(99);

        verify(session, never()).remove(any());
    }
}
