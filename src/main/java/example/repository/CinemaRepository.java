package example.repository;

import example.entity.Cinema;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public class CinemaRepository {

    @Autowired
    private SessionFactory sessionFactory;

    // Lấy tất cả và nạp luôn danh sách rooms để đếm số lượng động
    @Transactional(readOnly = true)
    public List<Cinema> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT DISTINCT c FROM Cinema c LEFT JOIN FETCH c.rooms", Cinema.class)
                .getResultList();
    }

    @Transactional
    public void save(Cinema cinema) {
        sessionFactory.getCurrentSession().merge(cinema);
    }

    @Transactional(readOnly = true)
    public Cinema findById(int id) {
        // Dùng HQL fetch để khi xem chi tiết hoặc sửa cũng có sẵn list rooms
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT c FROM Cinema c LEFT JOIN FETCH c.rooms WHERE c.id = :id", Cinema.class)
                .setParameter("id", id)
                .uniqueResult();
    }

    @Transactional
    public void delete(int id) {
        Session session = sessionFactory.getCurrentSession();
        Cinema cinema = session.get(Cinema.class, id);
        if (cinema != null) {
            session.remove(cinema);
        }
    }

    // Phân trang kết hợp JOIN FETCH
    @Transactional(readOnly = true)
    public List<Cinema> findAllPaged(int page, int pageSize) {
        Session session = sessionFactory.getCurrentSession();
        // Lưu ý: JOIN FETCH với phân trang nên cẩn thận với bộ nhớ, dùng DISTINCT để tránh trùng lặp bản ghi
        Query<Cinema> query = session.createQuery("SELECT DISTINCT c FROM Cinema c LEFT JOIN FETCH c.rooms ORDER BY c.id DESC", Cinema.class);
        query.setFirstResult((page - 1) * pageSize);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public long getTotalCount() {
        return (long) sessionFactory.getCurrentSession()
                .createQuery("SELECT count(c) FROM Cinema c")
                .uniqueResult();
    }

    /**
     * Hàm Search tổng hợp: Đã sửa toán tử == thành >= cho số phòng
     */
    @Transactional(readOnly = true)
    public List<Cinema> searchCinemas(String name, String city, String status, String address, Integer minRooms, String phone) {
        Session session = sessionFactory.getCurrentSession();

        // Luôn FETCH rooms để hiển thị cột số lượng phòng chính xác sau khi search
        StringBuilder hql = new StringBuilder("SELECT DISTINCT c FROM Cinema c LEFT JOIN FETCH c.rooms WHERE 1=1 ");

        if (name != null && !name.isEmpty()) hql.append("AND c.cinemaName LIKE :name ");
        if (city != null && !city.isEmpty()) hql.append("AND c.city = :city ");
        if (status != null && !status.isEmpty()) hql.append("AND c.status = :status ");
        if (address != null && !address.isEmpty()) hql.append("AND c.address LIKE :address ");
        if (minRooms != null) hql.append("AND size(c.rooms) >= :minRooms "); // So sánh số lượng phần tử trong List rooms
        if (phone != null && !phone.isEmpty()) hql.append("AND c.phone LIKE :phone ");

        Query<Cinema> query = session.createQuery(hql.toString(), Cinema.class);

        if (name != null && !name.isEmpty()) query.setParameter("name", "%" + name + "%");
        if (city != null && !city.isEmpty()) query.setParameter("city", city);
        if (status != null && !status.isEmpty()) query.setParameter("status", status);
        if (address != null && !address.isEmpty()) query.setParameter("address", "%" + address + "%");
        if (minRooms != null) query.setParameter("minRooms", minRooms);
        if (phone != null && !phone.isEmpty()) query.setParameter("phone", "%" + phone + "%");

        return query.getResultList();
    }

    public boolean hasDependencies(int cinemaId) {
        Session session = sessionFactory.getCurrentSession();
        
        // Kiểm tra xem rạp chiếu có phòng chiếu nào chưa
        String hql = "SELECT count(r.id) FROM CinemaRoom r WHERE r.cinema.id = :cinemaId";
        Long roomCount = session.createQuery(hql, Long.class)
                .setParameter("cinemaId", cinemaId)
                .uniqueResult();
                
        return roomCount != null && roomCount > 0;
    }
}