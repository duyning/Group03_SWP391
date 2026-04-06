package example.repository;

import example.entity.CinemaRoom;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaRoomRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<CinemaRoom> query;

    @InjectMocks
    private CinemaRoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Lấy danh sách phòng theo ID rạp thành công (sử dụng JOIN FETCH)")
    void testFindByCinemaId() {
        when(session.createQuery(anyString(), eq(CinemaRoom.class))).thenReturn(query);
        when(query.setParameter("cinemaId", 1)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new CinemaRoom(), new CinemaRoom()));

        List<CinemaRoom> result = roomRepository.findByCinemaId(1);

        assertEquals(2, result.size());
        verify(session).createQuery(contains("LEFT JOIN FETCH r.seats"), eq(CinemaRoom.class));
    }

    @Test
    @DisplayName("Lưu Phòng chiếu thành công qua hàm merge")
    void testSave() {
        CinemaRoom room = new CinemaRoom();
        when(session.merge(room)).thenReturn(room);

        assertDoesNotThrow(() -> roomRepository.save(room));

        verify(session, times(1)).merge(room);
    }

    @Test
    @DisplayName("Tìm Phòng chiếu theo ID thành công")
    void testFindById() {
        CinemaRoom room = new CinemaRoom();
        room.setId(10);
        
        when(session.createQuery(anyString(), eq(CinemaRoom.class))).thenReturn(query);
        when(query.setParameter("id", 10)).thenReturn(query);
        when(query.uniqueResult()).thenReturn(room);

        CinemaRoom result = roomRepository.findById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    @DisplayName("Xóa Phòng chiếu thành công khi ID thuộc về một đối tượng có thực")
    void testDelete_Success() {
        CinemaRoom room = new CinemaRoom();
        room.setId(5);
        
        when(session.createQuery(anyString(), eq(CinemaRoom.class))).thenReturn(query);
        when(query.setParameter("id", 5)).thenReturn(query);
        when(query.uniqueResult()).thenReturn(room);

        roomRepository.delete(5);

        verify(session, times(1)).remove(room);
    }

    @Test
    @DisplayName("Không xóa Phòng chiếu nếu ID rỗng/không tồn tại")
    void testDelete_NotFound() {
        when(session.createQuery(anyString(), eq(CinemaRoom.class))).thenReturn(query);
        when(query.setParameter("id", 99)).thenReturn(query);
        when(query.uniqueResult()).thenReturn(null);

        roomRepository.delete(99);

        verify(session, never()).remove(any());
    }

    @Test
    @DisplayName("Tìm kiếm Phòng chiếu động với các tham số, không lọc số ghế")
    void testSearch_NoSeatFilter() {
        when(session.createQuery(anyString(), eq(CinemaRoom.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        
        // Return a mutable list because repository does removeIf on result list
        List<CinemaRoom> mockResult = new ArrayList<>(Arrays.asList(new CinemaRoom()));
        when(query.getResultList()).thenReturn(mockResult);

        List<CinemaRoom> result = roomRepository.search(1, "Phòng 1", "2D", null, "Hoạt động");

        assertEquals(1, result.size());
        verify(query).setParameter("cinemaId", 1);
        verify(query).setParameter("name", "%Phòng 1%");
        verify(query).setParameter("type", "2D");
        verify(query).setParameter("status", "Hoạt động");
    }

    @Test
    @DisplayName("Tìm kiếm Phòng chiếu và lọc số lượng ghế tối thiểu an toàn")
    void testSearch_WithSeatFilter() {
        when(session.createQuery(anyString(), eq(CinemaRoom.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        
        // Create an anonymous subclass to override getTotalSeats for isolated testing
        CinemaRoom smallRoom = new CinemaRoom() {
            @Override public int getTotalSeats() { return 10; }
        };
        CinemaRoom largeRoom = new CinemaRoom() {
            @Override public int getTotalSeats() { return 50; }
        };
        
        List<CinemaRoom> mockResult = new ArrayList<>(Arrays.asList(smallRoom, largeRoom));
        when(query.getResultList()).thenReturn(mockResult);

        // Required min seats = 30
        List<CinemaRoom> result = roomRepository.search(1, "", "", 30, "");

        assertEquals(1, result.size());
        assertEquals(50, result.get(0).getTotalSeats(), "Chỉ phòng có số ghế >= 30 được giữ lại");
    }
}
