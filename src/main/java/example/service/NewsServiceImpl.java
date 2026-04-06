package example.service;

import example.entity.News;
import example.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NewsServiceImpl implements NewsService {
    @Autowired
    private NewsRepository newsRepository;

    @Override
    public List<News> findAllForAdmin(int page, int size) {
        return newsRepository.findAllPaged(page, size);
    }

    @Override
    public News getById(Long id) {
        return newsRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<News> search(String title, Boolean status) {
        return newsRepository.searchNews(title, status);
    }

    @Override
    @Transactional
    public void saveOrUpdate(News news) {
        if (news.getId() != null && news.getId() <= 0) {
            news.setId(null);
        }

        // Đảm bảo ngày tạo không bị null khi cập nhật
        if (news.getId() != null) {
            News existing = newsRepository.findById(news.getId());
            if (existing != null) {
                news.setCreatedDate(existing.getCreatedDate());
            }
        } else {
            news.setCreatedDate(java.time.LocalDateTime.now());
        }

        newsRepository.saveOrUpdate(news);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        News news = newsRepository.findById(id);
        if (news != null) {
            news.setStatus(!news.isStatus());
            newsRepository.saveOrUpdate(news);
        }
    }

    // Các hàm thống kê để Controller truyền ra View
    @Override
    public long countTotal() { return newsRepository.countAll(); }

    @Override
    public long countActive() {
        // Bạn có thể viết thêm hàm này trong Repo hoặc filter từ list
        return newsRepository.findAllPaged(1, 1000).stream().filter(News::isStatus).count();
    }

    @Override
    public long countHidden() {
        return countTotal() - countActive();
    }

    @Override
    @Transactional
    public void delete(Long id) { newsRepository.delete(id); }

    @Override
    public List<News> findVisibleForUser(int page, int size) {
        return newsRepository.findActiveOnly(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public News getByIdForUser(Long id) {
        News news = newsRepository.findById(id);
        // Bảo mật: Nếu bài viết không tồn tại hoặc đang bị ẩn (status = false), trả về null
        if (news == null || !news.isStatus()) {
            return null;
        }
        return news;
    }
}
