package example.repository;

import example.entity.Cinema;
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
class CinemaRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Cinema> query;

    @Mock
    private Query<Long> queryLong;

    @InjectMocks
    private CinemaRepository cinemaRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Lấy danh sách tất cả Rạp thành công qua HQL")
    void testFindAll() {
        when(session.createQuery(anyString(), eq(Cinema.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Cinema(), new Cinema()));

        List<Cinema> result = cinemaRepository.findAll();

        assertEquals(2, result.size());
        verify(session).createQuery("SELECT DISTINCT c FROM Cinema c LEFT JOIN FETCH c.rooms", Cinema.class);
    }

    @Test
    @DisplayName("Lưu Rạp thành công bằng hàm merge")
    void testSave() {
        Cinema cinema = new Cinema();
        when(session.merge(cinema)).thenReturn(cinema);

        assertDoesNotThrow(() -> cinemaRepository.save(cinema));

        verify(session, times(1)).merge(cinema);
    }

    @Test
    @DisplayName("Tìm Rạp theo ID qua HQL FETCH thành công")
    void testFindById() {
        Cinema cinema = new Cinema();
        cinema.setId(1);
        
        when(session.createQuery(anyString(), eq(Cinema.class))).thenReturn(query);
        when(query.setParameter("id", 1)).thenReturn(query);
        when(query.uniqueResult()).thenReturn(cinema);

        Cinema result = cinemaRepository.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    @DisplayName("Xóa Rạp thành công khi tìm thấy Rạp")
    void testDelete_Success() {
        Cinema cinema = new Cinema();
        cinema.setId(10);
        when(session.get(Cinema.class, 10)).thenReturn(cinema);

        cinemaRepository.delete(10);

        verify(session, times(1)).remove(cinema);
    }
    
    @Test
    @DisplayName("Không xóa Rạp nếu rạp không tồn tại")
    void testDelete_NotFound() {
        when(session.get(Cinema.class, 99)).thenReturn(null);

        cinemaRepository.delete(99);

        verify(session, never()).remove(any());
    }

    @Test
    @DisplayName("Lấy danh sách rạp phân trang thành công")
    void testFindAllPaged() {
        when(session.createQuery(anyString(), eq(Cinema.class))).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Cinema(), new Cinema()));

        List<Cinema> result = cinemaRepository.findAllPaged(2, 10);

        assertEquals(2, result.size());
        
        // Trang 2, size 10 => firstResult = (2 - 1) * 10 = 10
        verify(query).setFirstResult(10);
        verify(query).setMaxResults(10);
    }

    @Test
    @DisplayName("Đếm tổng số lượng Rạp thành công")
    void testGetTotalCount() {
        // Casting logic from Number -> long, but query returns Long direct in best scenario
        when(session.createQuery(anyString())).thenReturn((Query) queryLong);
        when(queryLong.uniqueResult()).thenReturn(100L);

        long count = cinemaRepository.getTotalCount();

        assertEquals(100L, count);
        verify(session).createQuery("SELECT count(c) FROM Cinema c");
    }

    @Test
    @DisplayName("Tìm kiếm Rạp với các tiêu chí đầu vào động")
    void testSearchCinemas() {
        when(session.createQuery(anyString(), eq(Cinema.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        
        Cinema cinema = new Cinema();
        when(query.getResultList()).thenReturn(Arrays.asList(cinema));

        List<Cinema> result = cinemaRepository.searchCinemas("CGV", "Hồ Chí Minh", "Hoạt động", "Quận 1", 5, "1900");

        assertEquals(1, result.size());
        
        verify(query).setParameter("name", "%CGV%");
        verify(query).setParameter("city", "Hồ Chí Minh");
        verify(query).setParameter("status", "Hoạt động");
        verify(query).setParameter("address", "%Quận 1%");
        verify(query).setParameter("minRooms", 5);
        verify(query).setParameter("phone", "%1900%");
    }
    
    @Test
    @DisplayName("Tìm kiếm Rạp không truyền tiêu chí lọc")
    void testSearchCinemas_NoCriteria() {
        when(session.createQuery(anyString(), eq(Cinema.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Cinema(), new Cinema()));

        List<Cinema> result = cinemaRepository.searchCinemas(null, "", null, "", null, null);

        assertEquals(2, result.size());
        verify(query, never()).setParameter(anyString(), any());
    }
}
