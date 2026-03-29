package example.service;

import example.entity.News;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NewsService {
    List<News> findAllForAdmin(int page, int size);
    long countTotal();
    List<News> findVisibleForUser(int page, int size);
    News getById(Long id);

    void saveOrUpdate(News news);
    void delete(Long id);
    void toggleStatus(Long id);

    // Thêm các hàm đếm để hiển thị lên Stats Cards (Thẻ thống kê)
    long countActive();
    long countHidden();

    List<News> search(String title, Boolean status);

    News getByIdForUser(Long id);
}
