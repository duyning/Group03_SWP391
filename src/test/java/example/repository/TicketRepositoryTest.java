package example.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.MutationQuery;
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

/**
 * Lớp kiểm thử cho TicketRepository.
 * Tập trung vào các câu lệnh HQL lấy danh sách ghế và xóa hàng loạt.
 */
@ExtendWith(MockitoExtension.class)
class TicketRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Integer> query;

    @Mock
    private MutationQuery mutationQuery;

    @InjectMocks
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Lấy danh sách ID ghế đã được đặt cho một suất chiếu (HQL Join)")
    void testGetBookedSeatIds() {
        // 1. Arrange
        int showtimeId = 1;
        List<Integer> mockSeatIds = Arrays.asList(10, 11, 12);

        when(session.createQuery(anyString(), eq(Integer.class))).thenReturn(query);
        when(query.setParameter("showtimeId", showtimeId)).thenReturn(query);
        when(query.list()).thenReturn(mockSeatIds);

        // 2. Act
        List<Integer> result = ticketRepository.getBookedSeatIds(showtimeId);

        // 3. Assert
        assertEquals(3, result.size());
        assertEquals(10, result.get(0));
        verify(session).createQuery(contains("SELECT t.seat.id FROM Ticket t"), eq(Integer.class));
    }

    @Test
    @DisplayName("Xóa hàng loạt vé theo ID đơn hàng (Bulk Delete HQL)")
    void testDeleteByBookingId() {
        // 1. Arrange
        Long bookingId = 999L;
        when(session.createMutationQuery(anyString())).thenReturn(mutationQuery);
        when(mutationQuery.setParameter("bookingId", bookingId)).thenReturn(mutationQuery);
        when(mutationQuery.executeUpdate()).thenReturn(1);

        // 2. Act
        ticketRepository.deleteByBookingId(bookingId);

        // 3. Assert
        verify(session).createMutationQuery(contains("DELETE FROM Ticket t"));
        verify(mutationQuery).executeUpdate();
    }
}
