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

    // --- HÀM TÌM KIẾM SUẤT CHIẾU ĐỂ CHECK TRÙNG LỊCH QUA ĐÊM ---
    public List<Showtime> findForOverlapCheck(int roomId, LocalDate date1, LocalDate date2) {
        String hql = "FROM Showtime s WHERE s.room.id = :roomId AND (s.startDate = :date1 OR s.startDate = :date2)";
        return sessionFactory.getCurrentSession()
                .createQuery(hql, Showtime.class)
                .setParameter("roomId", roomId)
                .setParameter("date1", date1)
                .setParameter("date2", date2)
                .list();
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

    public List<example.entity.Movie> findMoviesByFilters(Integer cinemaId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        String hql = "SELECT DISTINCT s.movie FROM Showtime s WHERE s.startDate = :date ";
        if (cinemaId != null) {
            hql += "AND s.room.cinema.id = :cinemaId ";
        }
        if (startTime != null && endTime != null) {
            hql += "AND s.startTime >= :startTime AND s.startTime <= :endTime";
        }

        var query = sessionFactory.getCurrentSession().createQuery(hql, example.entity.Movie.class);
        query.setParameter("date", date);
        if (cinemaId != null) {
            query.setParameter("cinemaId", cinemaId);
        }
        if (startTime != null && endTime != null) {
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
        }

        return query.list();
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