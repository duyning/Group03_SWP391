package example.repository;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
@Transactional
public class TicketRepository {

    @Autowired
    private SessionFactory sessionFactory;

    // Hàm trả về danh sách ID các ghế đã có người đặt
    public List<Integer> getBookedSeatIds(int showtimeId) {
        String hql = "SELECT t.seat.id FROM Ticket t WHERE t.showtime.id = :showtimeId";
        return sessionFactory.getCurrentSession()
                .createQuery(hql, Integer.class)
                .setParameter("showtimeId", showtimeId)
                .list();
    }

    public void save(example.entity.Ticket ticket) {
        sessionFactory.getCurrentSession().persist(ticket);
    }

    public void deleteByBookingId(Long bookingId) {
        String hql = "DELETE FROM Ticket t WHERE t.booking.id = :bookingId";
        sessionFactory.getCurrentSession()
                .createMutationQuery(hql)
                .setParameter("bookingId", bookingId)
                .executeUpdate();
    }
}