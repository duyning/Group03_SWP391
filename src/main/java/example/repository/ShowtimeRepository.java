package example.repository;

import example.entity.Showtime;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery; // Import cái này
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
@Transactional
public class ShowtimeRepository {
    @Autowired
    private SessionFactory sessionFactory;

    public List<Showtime> findByCinemaAndDate(int cinemaId, LocalDate date) {
        String hql = "FROM Showtime s WHERE s.room.cinema.id = :cinemaId AND s.startDate = :date ORDER BY s.room.id, s.startTime ASC";
        return sessionFactory.getCurrentSession()
                .createQuery(hql, Showtime.class)
                .setParameter("cinemaId", cinemaId)
                .setParameter("date", date)
                .list();
    }

    public void save(Showtime showtime) {
        sessionFactory.getCurrentSession().merge(showtime);
    }

    public Showtime findById(int id) {
        return sessionFactory.getCurrentSession().get(Showtime.class, id);
    }

    // Xóa hàm delete (nếu có) hoặc giữ nguyên tùy bạn
    public void delete(Showtime showtime) {
        sessionFactory.getCurrentSession().remove(showtime);
    }

    // --- HÀM CHECK TRÙNG LỊCH (SỬA LẠI DÙNG NATIVE SQL) ---
    // Trong file ShowtimeRepository.java

    public boolean checkOverlap(int roomId, LocalDate date, LocalTime newStart, LocalTime newEnd, int excludeId) {
        // 1. VIẾT SQL THUẦN (NATIVE)
        // Lưu ý: CAST(:tham_so AS TIME) -> Đây là cú pháp ép kiểu của SQL Server
        String sql = "SELECT COUNT(*) FROM Showtimes " +
                "WHERE room_id = :roomId " +
                "AND startDate = :date " +
                "AND id != :excludeId " +
                "AND (startTime < CAST(:newEndStr AS TIME) AND endTime > CAST(:newStartStr AS TIME))";

        // 2. THỰC THI QUERY
        Object result = sessionFactory.getCurrentSession()
                .createNativeQuery(sql, Long.class) // Hibernate 6
                // Nếu lỗi dòng trên (do Hibernate cũ), đổi thành .createNativeQuery(sql)

                .setParameter("roomId", roomId)
                .setParameter("date", date)
                .setParameter("excludeId", excludeId)

                // 3. QUAN TRỌNG: TRUYỀN VÀO LÀ STRING (Vd: "17:30:00")
                // Để SQL Server tự CAST cái chuỗi này thành TIME
                .setParameter("newStartStr", newStart.toString())
                .setParameter("newEndStr", newEnd.toString())

                .uniqueResult();

        // Xử lý kết quả trả về (đôi khi native query trả về BigInteger hoặc Integer tùy phiên bản DB)
        long count = 0;
        if (result instanceof Number) {
            count = ((Number) result).longValue();
        }

        return count > 0;
    }

    public List<Showtime> findForBooking(int movieId, int cinemaId, LocalDate date) {
        String hql = "FROM Showtime s " +
                "WHERE s.movie.id = :movieId " +
                "AND s.room.cinema.id = :cinemaId " +
                "AND s.startDate = :date " +
                "ORDER BY s.startTime ASC";

        return sessionFactory.getCurrentSession()
                .createQuery(hql, Showtime.class)
                .setParameter("movieId", movieId)
                .setParameter("cinemaId", cinemaId)
                .setParameter("date", date)
                .list();
    }

    public boolean isPastDateTime(LocalDate date, LocalTime time) {
        // Sử dụng hàm GETDATE() của SQL Server để so sánh thời gian thực
        String sql = "SELECT COUNT(*) FROM (SELECT 1 as result) t " +
                "WHERE CAST(:dateStr + ' ' + :timeStr AS DATETIME) < GETDATE()";

        Object result = sessionFactory.getCurrentSession()
                .createNativeQuery(sql, Long.class)
                .setParameter("dateStr", date.toString())
                .setParameter("timeStr", time.toString())
                .uniqueResult();

        long count = (result instanceof Number) ? ((Number) result).longValue() : 0;
        return count > 0;
    }
}