package example.repository;

import example.entity.Movie;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Movie> query;

    @Mock
    private Query rawQuery; // Dùng cho các hàm trả về kiểu long (countAll)

    @InjectMocks
    private MovieRepository movieRepository;

    @BeforeEach
    void setUp() {
        // Luôn giả lập sessionFactory trả về session hiện tại
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    // ==========================================
    // TEST CHO HÀM: findAll()
    // ==========================================

    @Test
    @DisplayName("testFindAll_Success: Lấy danh sách phim thành công")
    void testFindAll_Success() {
        List<Movie> mockMovies = Arrays.asList(new Movie(), new Movie());
        when(session.createQuery("FROM Movie", Movie.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(mockMovies);

        List<Movie> result = movieRepository.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(session, times(1)).createQuery("FROM Movie", Movie.class);
    }

    // ==========================================
    // TEST CHO HÀM: findByMultiCriteria()
    // ==========================================

    @Test
    @DisplayName("testFindByMultiCriteria_WithName: Tìm kiếm phim theo tên")
    void testFindByMultiCriteria_WithName() {
        String name = "Mai";
        // Mock chuỗi HQL động khi có tham số name
        when(session.createQuery(anyString(), eq(Movie.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Movie()));

        List<Movie> result = movieRepository.findByMultiCriteria(name, null, null, null, null, null);

        assertFalse(result.isEmpty());
        // Kiểm tra xem tham số "name" có được set đúng vào query không
        verify(query).setParameter("name", "%Mai%");
    }

    // ==========================================
    // TEST CHO HÀM: saveOrUpdate()
    // ==========================================

    @Test
    @DisplayName("testSaveOrUpdate_Success: Lưu hoặc cập nhật phim thành công")
    void testSaveOrUpdate_Success() {
        Movie movie = new Movie();
        movieRepository.saveOrUpdate(movie);

        // Kiểm tra xem phương thức merge() của Hibernate có được gọi không
        verify(session, times(1)).merge(movie);
    }

    // ==========================================
    // TEST CHO HÀM: findPaged()
    // ==========================================

    @Test
    @DisplayName("testFindPaged_Success: Phân trang phim thành công")
    void testFindPaged_Success() {
        int page = 2;
        int pageSize = 10;
        when(session.createQuery("FROM Movie", Movie.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Movie()));

        movieRepository.findPaged(page, pageSize);

        // Kiểm tra logic tính toán vị trí bắt đầu: (2-1)*10 = 10
        verify(query).setFirstResult(10);
        verify(query).setMaxResults(10);
    }

    // ==========================================
    // TEST CHO HÀM: countAll()
    // ==========================================

    @Test
    @DisplayName("testCountAll_Success: Đếm tổng số phim thành công")
    void testCountAll_Success() {
        when(session.createQuery("SELECT count(m) FROM Movie m")).thenReturn(rawQuery);
        when(rawQuery.uniqueResult()).thenReturn(50L);

        long count = movieRepository.countAll();

        assertEquals(50L, count);
        verify(session).createQuery("SELECT count(m) FROM Movie m");
    }
}