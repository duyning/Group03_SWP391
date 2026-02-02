package example.repository;

import example.entity.Seat;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class SeatRepository {

    @Autowired
    private SessionFactory sessionFactory;

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    // Xóa toàn bộ ghế của 1 phòng (khi thiết kế lại sơ đồ)
    public void deleteByRoomId(int roomId) {
        getCurrentSession()
                .createQuery("DELETE FROM Seat s WHERE s.cinemaRoom.id = :roomId")
                .setParameter("roomId", roomId)
                .executeUpdate();
    }

    public void save(Seat seat) {
        getCurrentSession().persist(seat);
    }
}
