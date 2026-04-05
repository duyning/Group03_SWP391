package example.repository;

import example.entity.Showtime;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Showtime> query;

    @Mock
    private NativeQuery<Long> nativeQuery;

    @InjectMocks
    private ShowtimeRepository showtimeRepository;

    @BeforeEach
    void setUp() {
        // Lenient mock so that it doesn't fail tests that don't invoke session factory
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Tìm suất chiếu theo rạp và ngày thành công")
    void testFindByCinemaAndDate() {
        int cinemaId = 1;
        LocalDate date = LocalDate.now();
        Showtime sh1 = new Showtime();
        sh1.setId(1);

        when(session.createQuery(anyString(), eq(Showtime.class))).thenReturn(query);
        when(query.setParameter(eq("cinemaId"), anyInt())).thenReturn(query);
        when(query.setParameter(eq("date"), any(LocalDate.class))).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(sh1));

        List<Showtime> result = showtimeRepository.findByCinemaAndDate(cinemaId, date);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());

        verify(session).createQuery(anyString(), eq(Showtime.class));
        verify(query).setParameter("cinemaId", cinemaId);
        verify(query).setParameter("date", date);
    }

    @Test
    @DisplayName("Lưu suất chiếu thành công")
    void testSave() {
        Showtime showtime = new Showtime();
        when(session.merge(showtime)).thenReturn(showtime);

        assertDoesNotThrow(() -> showtimeRepository.save(showtime));

        verify(session, times(1)).merge(showtime);
    }

    @Test
    @DisplayName("Tìm suất chiếu theo ID thành công")
    void testFindById() {
        Showtime showtime = new Showtime();
        showtime.setId(10);
        when(session.get(Showtime.class, 10)).thenReturn(showtime);

        Showtime result = showtimeRepository.findById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        verify(session, times(1)).get(Showtime.class, 10);
    }

    @Test
    @DisplayName("Xóa suất chiếu thành công")
    void testDelete() {
        Showtime showtime = new Showtime();
        doNothing().when(session).remove(showtime);

        assertDoesNotThrow(() -> showtimeRepository.delete(showtime));

        verify(session, times(1)).remove(showtime);
    }

    @Test
    @DisplayName("Tìm suất chiếu để check trùng lịch qua đêm thành công")
    void testFindForOverlapCheck() {
        int roomId = 1;
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = date1.minusDays(1);

        when(session.createQuery(anyString(), eq(Showtime.class))).thenReturn(query);
        when(query.setParameter(eq("roomId"), anyInt())).thenReturn(query);
        when(query.setParameter(eq("date1"), any(LocalDate.class))).thenReturn(query);
        when(query.setParameter(eq("date2"), any(LocalDate.class))).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(new Showtime()));

        List<Showtime> result = showtimeRepository.findForOverlapCheck(roomId, date1, date2);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(session).createQuery(anyString(), eq(Showtime.class));
        verify(query).setParameter("roomId", roomId);
        verify(query).setParameter("date1", date1);
        verify(query).setParameter("date2", date2);
    }

    @Test
    @DisplayName("Tìm suất chiếu để đặt vé (theo ID phim, ID rạp, ngày) thành công")
    void testFindForBooking() {
        when(session.createQuery(anyString(), eq(Showtime.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(new Showtime(), new Showtime()));

        List<Showtime> result = showtimeRepository.findForBooking(1, 1, LocalDate.now());

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(session).createQuery(anyString(), eq(Showtime.class));
    }

    @Test
    @DisplayName("Kiểm tra: Trả về true nếu thời gian truyền vào nằm trong quá khứ")
    void testIsPastDateTime_True() {
        when(session.createNativeQuery(anyString(), eq(Long.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.uniqueResult()).thenReturn(1L);

        boolean result = showtimeRepository.isPastDateTime(LocalDate.now(), LocalTime.now());

        assertTrue(result);
    }

    @Test
    @DisplayName("Kiểm tra: Trả về false nếu thời gian truyền vào lớn hơn thời gian hiện hành")
    void testIsPastDateTime_False() {
        when(session.createNativeQuery(anyString(), eq(Long.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.uniqueResult()).thenReturn(0L);

        boolean result = showtimeRepository.isPastDateTime(LocalDate.now(), LocalTime.now());

        assertFalse(result);
    }
}
