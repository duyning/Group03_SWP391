package example.repository;

import example.entity.CinemaRoom;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
@Transactional
public class CinemaRoomRepository {
    @Autowired
    private SessionFactory sessionFactory;

    public List<CinemaRoom> findByCinemaId(int cinemaId) {
        Session session = sessionFactory.getCurrentSession();
        // SỬA TẠI ĐÂY: Sử dụng LEFT JOIN FETCH r.seats để lấy danh sách ghế
        // Điều này giúp hàm getTotalSeats() trong Entity chạy được mà không lỗi Lazy Loading
        String hql = "SELECT DISTINCT r FROM CinemaRoom r " +
                "LEFT JOIN FETCH r.seats " +
                "WHERE r.cinema.id = :cinemaId";

        return session.createQuery(hql, CinemaRoom.class)
                .setParameter("cinemaId", cinemaId)
                .getResultList();
    }

    public void save(CinemaRoom room) {
        sessionFactory.getCurrentSession().merge(room);
    }

    public CinemaRoom findById(int id) {
        // Nên fetch seats nếu cần hiển thị số lượng ghế khi tìm theo ID
        Session session = sessionFactory.getCurrentSession();
        String hql = "SELECT r FROM CinemaRoom r LEFT JOIN FETCH r.seats WHERE r.id = :id";
        return session.createQuery(hql, CinemaRoom.class)
                .setParameter("id", id)
                .uniqueResult();
    }

    public void delete(int id) {
        CinemaRoom room = findById(id);
        if (room != null) sessionFactory.getCurrentSession().remove(room);
    }

    @Transactional(readOnly = true)
    public List<CinemaRoom> search(int cinemaId, String roomName, String roomType, Integer minSeats, String status) {
        Session session = sessionFactory.getCurrentSession();

        // SỬA TẠI ĐÂY: Nối thêm LEFT JOIN FETCH r.seats vào câu truy vấn tìm kiếm
        StringBuilder hql = new StringBuilder("SELECT DISTINCT r FROM CinemaRoom r " +
                "JOIN FETCH r.cinema " +
                "LEFT JOIN FETCH r.seats " +
                "WHERE r.cinema.id = :cinemaId ");

        if (roomName != null && !roomName.trim().isEmpty()) hql.append("AND r.roomName LIKE :name ");
        if (roomType != null && !roomType.trim().isEmpty()) hql.append("AND r.roomType = :type ");
        if (status != null && !status.trim().isEmpty()) hql.append("AND r.status = :status ");

        hql.append("ORDER BY r.id DESC");

        Query<CinemaRoom> query = session.createQuery(hql.toString(), CinemaRoom.class);
        query.setParameter("cinemaId", cinemaId);

        if (roomName != null && !roomName.trim().isEmpty()) query.setParameter("name", "%" + roomName.trim() + "%");
        if (roomType != null && !roomType.trim().isEmpty()) query.setParameter("type", roomType);
        if (status != null && !status.trim().isEmpty()) query.setParameter("status", status);

        List<CinemaRoom> results = query.getResultList();

        // XỬ LÝ LỌC THEO SỐ GHẾ (Vì không còn cột totalSeats trong DB nên phải lọc thủ công hoặc dùng HQL phức tạp)
        if (minSeats != null) {
            results.removeIf(r -> r.getTotalSeats() < minSeats);
        }

        return results;
    }
}