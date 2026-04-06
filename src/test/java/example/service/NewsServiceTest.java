package example.service;

import example.entity.News;
import example.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử mở rộng cho chức năng Quản lý Tin tức (News Service).
 * Bao gồm các trường hợp tìm kiếm linh hoạt, bảo trì dữ liệu và thống kê.
 */
@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News mockNews;

    @BeforeEach
    void setUp() {
        mockNews = new News();
        mockNews.setId(1L);
        mockNews.setTitle("Khuyến mãi mùa hè");
        mockNews.setContent("Nội dung khuyến mãi...");
        mockNews.setStatus(true);
        mockNews.setCreatedDate(LocalDateTime.now().minusDays(5)); // Tạo cách đây 5 ngày
    }

    @Test
    @DisplayName("UNTCID1: Thay đổi trạng thái bài viết (Toggle Status)")
    void testToggleStatus() {
        when(newsRepository.findById(1L)).thenReturn(mockNews);
        newsService.toggleStatus(1L);
        assertFalse(mockNews.isStatus(), "Trạng thái bài viết phải chuyển sang FALSE");
        verify(newsRepository).saveOrUpdate(mockNews);
    }

    @Test
    @DisplayName("UNTCID2: Người dùng không thể xem bài viết bị ẩn")
    void testGetByIdForUser_Hidden() {
        News hiddenNews = new News();
        hiddenNews.setId(2L);
        hiddenNews.setStatus(false);
        when(newsRepository.findById(2L)).thenReturn(hiddenNews);

        News result = newsService.getByIdForUser(2L);
        assertNull(result, "Phải trả về null khi truy cập bài viết bị ẩn");
    }

    @Test
    @DisplayName("UNTCID3: Tự động tạo ngày đăng cho bài viết mới")
    void testSaveOrUpdate_NewNews() {
        News newNews = new News();
        newNews.setId(null);
        newsService.saveOrUpdate(newNews);
        assertNotNull(newNews.getCreatedDate(), "Ngày tạo không được để trống khi tạo mới");
    }

    @Test
    @DisplayName("UNTCID4: Cập nhật bài viết cũ và giữ nguyên ngày tạo gốc")
    void testSaveOrUpdate_UpdateExisting() {
        // 1. Arrange: Bài viết đã có ngày tạo từ 5 ngày trước
        LocalDateTime originalDate = mockNews.getCreatedDate();
        News updatedNews = new News();
        updatedNews.setId(1L);
        updatedNews.setTitle("Tiêu đề mới");
        
        when(newsRepository.findById(1L)).thenReturn(mockNews); // Tìm thấy bản ghi cũ

        // 2. Act: Cập nhật bài viết
        newsService.saveOrUpdate(updatedNews);

        // 3. Assert: Ngày tạo phải được copy từ bản cũ sang bản mới
        assertEquals(originalDate, updatedNews.getCreatedDate(), "Ngày tạo phải được giữ nguyên khi cập nhật");
        verify(newsRepository).saveOrUpdate(updatedNews);
    }

    @Test
    @DisplayName("UNTCID5: Tìm kiếm bài viết theo tiêu đề (Search by Title)")
    void testSearch_TitleOnly() {
        // 1. Arrange
        String query = "Khuyến mãi";
        when(newsRepository.searchNews(query, null)).thenReturn(Collections.singletonList(mockNews));

        // 2. Act
        List<News> results = newsService.search(query, null);

        // 3. Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).getTitle().contains("Khuyến mãi"));
    }

    @Test
    @DisplayName("UNTCID6: Tìm kiếm bài viết theo trạng thái (Search by Status)")
    void testSearch_StatusOnly() {
        // 1. Arrange
        when(newsRepository.searchNews(null, true)).thenReturn(Collections.singletonList(mockNews));

        // 2. Act
        List<News> results = newsService.search(null, true);

        // 3. Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).isStatus());
    }

    @Test
    @DisplayName("UNTCID7: Đếm số lượng bài viết đang hoạt động")
    void testCountActive() {
        List<News> allNews = new ArrayList<>();
        News n1 = new News(); n1.setStatus(true);
        News n2 = new News(); n2.setStatus(false);
        allNews.add(n1); allNews.add(n2);
        when(newsRepository.findAllPaged(1, 1000)).thenReturn(allNews);

        long count = newsService.countActive();
        assertEquals(1, count);
    }
}
