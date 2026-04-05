package example.repository;

import example.entity.Booking;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BookingRepository {
    @Autowired
    private SessionFactory sessionFactory;

    /**
     * Dành cho ADMIN: Lấy tất cả lịch sử đặt vé của hệ thống (Phân trang)
     */
    @Transactional(readOnly = true)
    public List<Booking> findAllPaged(int page, int size) {
        Session session = sessionFactory.getCurrentSession();
        // Sử dụng JOIN FETCH để tránh lỗi LazyInitializationException và tối ưu hiệu năng (N+1 query)
        String hql = "SELECT b FROM Booking b " +
                "JOIN FETCH b.showtime s " +
                "JOIN FETCH s.movie m " +
                "JOIN FETCH b.account a " +
                "JOIN FETCH s.room r " +
                "JOIN FETCH r.cinema c " +
                "ORDER BY b.bookingDate DESC";
        Query<Booking> query = session.createQuery(hql, Booking.class);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Dành cho NGƯỜI DÙNG: Lấy lịch sử đặt vé của một tài khoản cụ thể
     */
    @Transactional(readOnly = true)
    public List<Booking> findByAccountId(int accountId) {
        Session session = sessionFactory.getCurrentSession();
        String hql = "SELECT b FROM Booking b " +
                "JOIN FETCH b.account a " +
                "JOIN FETCH b.showtime s " +
                "JOIN FETCH s.movie m " +
                "JOIN FETCH s.room r " +
                "JOIN FETCH r.cinema c " +
                "WHERE a.accountID = :accId " +
                "ORDER BY b.bookingDate DESC";
        Query<Booking> query = session.createQuery(hql, Booking.class);
        query.setParameter("accId", accountId);
        return query.getResultList();
    }

    /**
     * Tìm chi tiết một đơn đặt vé theo ID
     */
    @Transactional(readOnly = true)
    public Booking findById(Long id) {
        Session session = sessionFactory.getCurrentSession();
        String hql = "SELECT b FROM Booking b " +
                "JOIN FETCH b.showtime s " +
                "JOIN FETCH s.movie m " +
                "JOIN FETCH s.room r " +
                "JOIN FETCH r.cinema c " +
                "WHERE b.id = :id";
        return session.createQuery(hql, Booking.class)
                .setParameter("id", id)
                .uniqueResult();
    }

    /**
     * Lưu thông tin đặt vé mới
     */
    @Transactional
    public Booking save(Booking booking) {
        Session session = sessionFactory.getCurrentSession();
        if (booking.getId() == null) {
            session.persist(booking);
            return booking;
        } else {
            return session.merge(booking);
        }
    }

    /**
     * Xóa thông tin đặt vé theo ID
     */
    @Transactional
    public void deleteById(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Booking booking = session.get(Booking.class, id);
        if (booking != null) {
            session.remove(booking);
        }
    }

    /**
     * Đếm tổng số đơn hàng (Admin)
     */
    @Transactional(readOnly = true)
    public long countAll() {
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("SELECT count(b) FROM Booking b", Long.class).uniqueResult();
    }

    /**
     * Tìm kiếm nâng cao (Admin): Theo tên khách hàng, tên phim hoặc mã booking
     */
    @Transactional(readOnly = true)
    public List<Booking> searchBookings(String keyword) {
        Session session = sessionFactory.getCurrentSession();
        StringBuilder hql = new StringBuilder("SELECT b FROM Booking b ");
        hql.append("JOIN FETCH b.showtime s ");
        hql.append("JOIN FETCH s.movie m ");
        hql.append("JOIN FETCH b.account a ");
        hql.append("WHERE a.name LIKE :key OR m.movieName LIKE :key ");
        hql.append("ORDER BY b.bookingDate DESC");

        Query<Booking> query = session.createQuery(hql.toString(), Booking.class);
        query.setParameter("key", "%" + keyword + "%");

        return query.getResultList();
    }

    // 1. Tính tổng doanh thu của một ngày cụ thể
    @Transactional(readOnly = true)
    public Double getTotalRevenueByDate(LocalDateTime start, LocalDateTime end) {
        Session session = sessionFactory.getCurrentSession();
        String hql = "SELECT SUM(b.totalAmount) FROM Booking b WHERE b.bookingDate BETWEEN :start AND :end";
        Double result = session.createQuery(hql, Double.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .uniqueResult();
        return result != null ? result : 0.0;
    }

    // 2. Tính tổng số vé đã bán (Dựa trên ticketQuantity bạn đã lưu)
    @Transactional(readOnly = true)
    public Long getTotalTicketsSold() {
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("SELECT SUM(b.ticketQuantity) FROM Booking b", Long.class).uniqueResult();
    }

    // 3. Lấy danh sách đặt vé chi tiết cho Dashboard (Top 10 gần nhất)
    @Transactional(readOnly = true)
    public List<Booking> findRecentBookings(int limit) {
        Session session = sessionFactory.getCurrentSession();
        String hql = "SELECT b FROM Booking b " +
                "JOIN FETCH b.showtime s JOIN FETCH s.movie m " +
                "JOIN FETCH b.account a JOIN FETCH s.room r JOIN FETCH r.cinema c " +
                "ORDER BY b.bookingDate DESC";
        return session.createQuery(hql, Booking.class)
                .setMaxResults(limit)
                .getResultList();
    }

    // 4. Tìm kiếm đặt vé thông minh tùy chỉnh bộ lọc (Gồm Rạp và Khoảng Giá)
    @Transactional(readOnly = true)
    public List<Booking> searchRecentBookings(String keyword, Integer cinemaId, Double minPrice, Double maxPrice, int limit) {
        Session session = sessionFactory.getCurrentSession();
        StringBuilder hql = new StringBuilder("SELECT b FROM Booking b ");
        hql.append("JOIN FETCH b.showtime s JOIN FETCH s.movie m ");
        hql.append("JOIN FETCH b.account a JOIN FETCH s.room r JOIN FETCH r.cinema c ");
        hql.append("WHERE 1=1 ");

        if (keyword != null && !keyword.trim().isEmpty()) {
            hql.append("AND (LOWER(m.movieName) LIKE LOWER(:key) ");
            hql.append("OR LOWER(a.name) LIKE LOWER(:key) ");
            hql.append("OR LOWER(c.cinemaName) LIKE LOWER(:key)) ");
        }
        if (cinemaId != null) {
            hql.append("AND c.id = :cinemaId ");
        }
        if (minPrice != null) {
            hql.append("AND b.totalAmount >= :minPrice ");
        }
        if (maxPrice != null) {
            hql.append("AND b.totalAmount <= :maxPrice ");
        }
        
        hql.append("ORDER BY b.bookingDate DESC");

        Query<Booking> query = session.createQuery(hql.toString(), Booking.class);
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.setParameter("key", "%" + keyword.trim() + "%");
        }
        if (cinemaId != null) {
            query.setParameter("cinemaId", cinemaId);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        query.setMaxResults(limit);
        return query.getResultList();
    }

    // 7. Đếm tổng số kết quả khi tìm kiếm có filter
    @Transactional(readOnly = true)
    public long countSearchBookings(String keyword, Integer cinemaId, Double minPrice, Double maxPrice) {
        Session session = sessionFactory.getCurrentSession();
        StringBuilder hql = new StringBuilder("SELECT COUNT(b) FROM Booking b ");
        hql.append("JOIN b.showtime s JOIN s.movie m ");
        hql.append("JOIN b.account a JOIN s.room r JOIN r.cinema c ");
        hql.append("WHERE 1=1 ");
        if (keyword != null && !keyword.trim().isEmpty()) {
            hql.append("AND (LOWER(m.movieName) LIKE LOWER(:key) OR LOWER(a.name) LIKE LOWER(:key) OR LOWER(c.cinemaName) LIKE LOWER(:key)) ");
        }
        if (cinemaId != null) hql.append("AND c.id = :cinemaId ");
        if (minPrice != null) hql.append("AND b.totalAmount >= :minPrice ");
        if (maxPrice != null) hql.append("AND b.totalAmount <= :maxPrice ");

        Query<Long> query = session.createQuery(hql.toString(), Long.class);
        if (keyword != null && !keyword.trim().isEmpty()) query.setParameter("key", "%" + keyword.trim() + "%");
        if (cinemaId != null) query.setParameter("cinemaId", cinemaId);
        if (minPrice != null) query.setParameter("minPrice", minPrice);
        if (maxPrice != null) query.setParameter("maxPrice", maxPrice);

        Long result = query.uniqueResult();
        return result != null ? result : 0L;
    }

    // 8. Tìm kiếm có filter VÀ phân trang
    @Transactional(readOnly = true)
    public List<Booking> searchBookingsPaged(String keyword, Integer cinemaId, Double minPrice, Double maxPrice, int page, int size) {
        Session session = sessionFactory.getCurrentSession();
        StringBuilder hql = new StringBuilder("SELECT b FROM Booking b ");
        hql.append("JOIN FETCH b.showtime s JOIN FETCH s.movie m ");
        hql.append("JOIN FETCH b.account a JOIN FETCH s.room r JOIN FETCH r.cinema c ");
        hql.append("WHERE 1=1 ");
        if (keyword != null && !keyword.trim().isEmpty()) {
            hql.append("AND (LOWER(m.movieName) LIKE LOWER(:key) OR LOWER(a.name) LIKE LOWER(:key) OR LOWER(c.cinemaName) LIKE LOWER(:key)) ");
        }
        if (cinemaId != null) hql.append("AND c.id = :cinemaId ");
        if (minPrice != null) hql.append("AND b.totalAmount >= :minPrice ");
        if (maxPrice != null) hql.append("AND b.totalAmount <= :maxPrice ");
        hql.append("ORDER BY b.bookingDate DESC");

        Query<Booking> query = session.createQuery(hql.toString(), Booking.class);
        if (keyword != null && !keyword.trim().isEmpty()) query.setParameter("key", "%" + keyword.trim() + "%");
        if (cinemaId != null) query.setParameter("cinemaId", cinemaId);
        if (minPrice != null) query.setParameter("minPrice", minPrice);
        if (maxPrice != null) query.setParameter("maxPrice", maxPrice);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);
        return query.getResultList();
    }
}
