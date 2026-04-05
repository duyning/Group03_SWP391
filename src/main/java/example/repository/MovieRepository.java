package example.repository;

import example.entity.Movie;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public class MovieRepository {

    @Autowired
    private SessionFactory sessionFactory;

    // Sử dụng HQL (Hibernate Query Language) để lấy danh sách phim
    @Transactional(readOnly = true)
    public List<Movie> findAll() {
        Session session = sessionFactory.getCurrentSession();
        // Hibernate 6 khuyến khích chỉ định rõ kiểu dữ liệu trả về
        Query<Movie> query = session.createQuery("FROM Movie m ORDER BY m.id DESC", Movie.class);
        return query.getResultList();
    }

    // Lưu mới hoặc Cập nhật (Sử dụng persist/merge thay cho saveOrUpdate cũ)
    @Transactional
    public void saveOrUpdate(Movie movie) {
        Session session = sessionFactory.getCurrentSession();
        // Nếu ID = 0 hoặc null, Hibernate sẽ thực hiện Insert, ngược lại là Update
        session.merge(movie);
    }

    // Xóa phim dựa trên ID
    @Transactional
    public void delete(Movie movie) {
        Session session = sessionFactory.getCurrentSession();
        session.remove(movie); // Hoặc session.delete(movie)
    }

    public Movie getMovieById(int id) {
        Session session = sessionFactory.getCurrentSession();
        return session.get(Movie.class, id);
    }

    @Transactional(readOnly = true)
    public List<Movie> findByMultiCriteria(String name, String type, String author,
                                           String duration, String status, String date) {
        Session session = sessionFactory.getCurrentSession();

        // Khởi tạo StringBuilder để nối câu lệnh HQL động
        StringBuilder hql = new StringBuilder("FROM Movie m WHERE 1=1 ");

        // Kiểm tra và nối điều kiện (Sử dụng đúng tên biến trong Entity Movie)
        if (name != null && !name.trim().isEmpty())
            hql.append("AND m.movieName LIKE :name "); // Khớp với private String movieName

        if (type != null && !type.trim().isEmpty())
            hql.append("AND m.type LIKE :type ");

        if (author != null && !author.trim().isEmpty())
            hql.append("AND m.director LIKE :author "); // Khớp với private String director

        if (duration != null && !duration.trim().isEmpty())
            hql.append("AND m.duration LIKE :duration ");

        if (status != null && !status.trim().isEmpty())
            hql.append("AND m.status = :status ");

        if (date != null && !date.trim().isEmpty())
            hql.append("AND m.releaseDate LIKE :date ");

        hql.append("ORDER BY m.id DESC");

        Query<Movie> query = session.createQuery(hql.toString(), Movie.class);

        // Gán giá trị tham số (Binding parameters)
        if (name != null && !name.trim().isEmpty()) query.setParameter("name", "%" + name.trim() + "%");
        if (type != null && !type.trim().isEmpty()) query.setParameter("type", "%" + type.trim() + "%");
        if (author != null && !author.trim().isEmpty()) query.setParameter("author", "%" + author.trim() + "%");
        if (duration != null && !duration.trim().isEmpty()) query.setParameter("duration", "%" + duration.trim() + "%");
        if (status != null && !status.trim().isEmpty()) query.setParameter("status", status.trim());
        if (date != null && !date.trim().isEmpty()) query.setParameter("date", "%" + date.trim() + "%");

        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public List<Movie> findPaged(int page, int pageSize) {
        Session session = sessionFactory.getCurrentSession();
        Query<Movie> query = session.createQuery("FROM Movie m ORDER BY m.id DESC", Movie.class);
        query.setFirstResult((page - 1) * pageSize); // Vị trí bắt đầu lấy
        query.setMaxResults(pageSize);               // Số lượng lấy
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public long countAll() {
        Session session = sessionFactory.getCurrentSession();
        return (long) session.createQuery("SELECT count(m) FROM Movie m").uniqueResult();
    }

}