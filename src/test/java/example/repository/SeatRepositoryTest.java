package example.repository;

import example.entity.Seat;
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
class SeatRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query query;

    @Mock
    private Query<Seat> seatQuery;

    @InjectMocks
    private SeatRepository seatRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Xóa toàn bộ ghế theo ID phòng chiếu thành công")
    void testDeleteByRoomId() {
        when(session.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(50); // giả lập xóa 50 dòng

        seatRepository.deleteByRoomId(1);

        verify(session).createQuery("DELETE FROM Seat s WHERE s.cinemaRoom.id = :roomId");
        verify(query).setParameter("roomId", 1);
        verify(query).executeUpdate();
    }

    @Test
    @DisplayName("Lưu ghế mới thành công (sử dụng persist)")
    void testSave() {
        Seat seat = new Seat();
        
        seatRepository.save(seat);
        
        verify(session, times(1)).persist(seat);
    }

    @Test
    @DisplayName("Lấy danh sách ghế theo ID phòng chiếu thành công")
    void testFindByRoomId() {
        when(session.createQuery(anyString(), eq(Seat.class))).thenReturn(seatQuery);
        when(seatQuery.setParameter(anyString(), any())).thenReturn(seatQuery);
        when(seatQuery.list()).thenReturn(Arrays.asList(new Seat(), new Seat()));

        List<Seat> result = seatRepository.findByRoomId(1);

        assertEquals(2, result.size());
        verify(session).createQuery(contains("ORDER BY s.rowName, s.seatColumn"), eq(Seat.class));
        verify(seatQuery).setParameter("roomId", 1);
    }

    @Test
    @DisplayName("Tìm ghế theo ID thành công")
    void testFindById() {
        Seat seat = new Seat();
        seat.setId(10);
        when(session.get(Seat.class, 10)).thenReturn(seat);

        Seat result = seatRepository.findById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        verify(session, times(1)).get(Seat.class, 10);
    }
}
