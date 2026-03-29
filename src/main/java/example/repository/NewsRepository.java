package example.repository;

import example.entity.News;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class NewsRepository {
    @Autowired
    private SessionFactory sessionFactory;

    /**
     * Dành cho ADMIN: Lấy tất cả tin tức (cả ẩn và hiện) có phân trang
     */
    @Transactional(readOnly = true)
    public List<News> findAllPaged(int page, int size) {
        Session session = sessionFactory.getCurrentSession();
        // Sắp xếp theo ngày tạo mới nhất (createdDate)
        String hql = "FROM News n ORDER BY n.createdDate DESC";
        Query<News> query = session.createQuery(hql, News.class);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Dành cho NGƯỜI DÙNG: Chỉ lấy tin tức có trạng thái status = true
     */
    @Transactional(readOnly = true)
    public List<News> findActiveOnly(int page, int size) {
        Session session = sessionFactory.getCurrentSession();
        // Đổi từ n.active thành n.status để khớp với Entity của bạn
        String hql = "FROM News n WHERE n.status = true ORDER BY n.createdDate DESC";
        Query<News> query = session.createQuery(hql, News.class);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Tìm tin tức theo ID
     */
    @Transactional(readOnly = true)
    public News findById(Long id) {
        return sessionFactory.getCurrentSession().get(News.class, id);
    }

    /**
     * Lưu hoặc cập nhật tin tức
     */
    @Transactional
    public void saveOrUpdate(News news) {
        // Sử dụng merge hoặc saveOrUpdate tùy thuộc vào cấu hình Hibernate của bạn
        // merge an toàn hơn khi làm việc với các đối tượng detached
        Session session = sessionFactory.getCurrentSession();
        session.merge(news);
    }

    /**
     * Xóa tin tức
     */
    @Transactional
    public void delete(Long id) {
        Session session = sessionFactory.getCurrentSession();
        News news = session.get(News.class, id);
        if (news != null) {
            session.remove(news);
        }
    }

    /**
     * Đếm tổng số tin tức để làm phân trang (Admin)
     */
    @Transactional(readOnly = true)
    public long countAll() {
        Session session = sessionFactory.getCurrentSession();
        return (Long) session.createQuery("SELECT count(n) FROM News n", Long.class).uniqueResult();
    }

    /**
     * Đếm tổng số tin tức đang hiển thị (Người dùng)
     */
    @Transactional(readOnly = true)
    public long countActive() {
        Session session = sessionFactory.getCurrentSession();
        return (Long) session.createQuery("SELECT count(n) FROM News n WHERE n.status = true", Long.class).uniqueResult();
    }

    @Transactional(readOnly = true)
    public List<News> searchNews(String title, Boolean status) {
        Session session = sessionFactory.getCurrentSession();
        // Khởi tạo câu lệnh HQL cơ bản
        StringBuilder hql = new StringBuilder("FROM News n WHERE 1=1 ");

        // Thêm điều kiện lọc tiêu đề nếu có
        if (title != null && !title.trim().isEmpty()) {
            hql.append("AND n.title LIKE :title ");
        }
        // Thêm điều kiện lọc trạng thái nếu có
        if (status != null) {
            hql.append("AND n.status = :status ");
        }

        hql.append("ORDER BY n.createdDate DESC");

        Query<News> query = session.createQuery(hql.toString(), News.class);

        // Set tham số an toàn (tránh SQL Injection)
        if (title != null && !title.trim().isEmpty()) {
            query.setParameter("title", "%" + title.trim() + "%");
        }
        if (status != null) {
            query.setParameter("status", status);
        }

        return query.getResultList();
    }
}
