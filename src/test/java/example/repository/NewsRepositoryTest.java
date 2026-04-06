package example.repository;

import example.entity.News;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử cho NewsRepository.
 * Tập trung vào các câu lệnh HQL tìm kiếm động và phân trang.
 */
@ExtendWith(MockitoExtension.class)
class NewsRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<News> query;

    @InjectMocks
    private NewsRepository newsRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Tìm kiếm tin tức theo tiêu đề (Search by Title HQL)")
    void testSearchNews_ByTitle() {
        // 1. Arrange
        String title = "Khuyến mãi";
        when(session.createQuery(anyString(), eq(News.class))).thenReturn(query);
        when(query.setParameter("title", "%" + title + "%")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new News()));

        // 2. Act
        List<News> results = newsRepository.searchNews(title, null);

        // 3. Assert
        assertEquals(1, results.size());
        verify(session).createQuery(contains("AND n.title LIKE :title"), eq(News.class));
    }

    @Test
    @DisplayName("Tìm kiếm tin tức theo tiêu đề và trạng thái")
    void testSearchNews_Both() {
        // 1. Arrange
        String title = "Phim mới";
        when(session.createQuery(anyString(), eq(News.class))).thenReturn(query);
        when(query.setParameter("title", "%" + title + "%")).thenReturn(query);
        when(query.setParameter("status", true)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // 2. Act
        newsRepository.searchNews(title, true);

        // 3. Assert
        verify(session).createQuery(contains("AND n.status = :status"), eq(News.class));
    }

    @Test
    @DisplayName("Lấy tin tức đang hoạt động có phân trang")
    void testFindActiveOnly_Paged() {
        // 1. Arrange
        int page = 1;
        int size = 10;
        when(session.createQuery(anyString(), eq(News.class))).thenReturn(query);
        when(query.setFirstResult(0)).thenReturn(query);
        when(query.setMaxResults(size)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // 2. Act
        newsRepository.findActiveOnly(page, size);

        // 3. Assert
        verify(query).setFirstResult(0);
        verify(query).setMaxResults(10);
        verify(session).createQuery(contains("n.status = true"), eq(News.class));
    }
}
