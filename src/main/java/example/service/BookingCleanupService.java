package example.service;

import example.entity.Booking;
import example.repository.BookingRepository;
import example.repository.TicketRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingCleanupService {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // Chạy mỗi 1 phút (60,000 ms)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredBookings() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(15);
        Session session = sessionFactory.getCurrentSession();
        
        // Tìm các Booking có trạng thái PENDING và đã tạo quá 15 phút
        String hql = "SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.bookingDate < :expiryTime";
        List<Booking> expiredBookings = session.createQuery(hql, Booking.class)
                .setParameter("expiryTime", expiryTime)
                .getResultList();
                
        for (Booking b : expiredBookings) {
            System.out.println("Cleaning up expired booking (Timeout VNPay): " + b.getId());
            ticketRepository.deleteByBookingId(b.getId());
            // Do Hibernate có session cache, có thể cần đảm bảo Ticket đã xoá trước khi xoá Booking
            session.flush();
            bookingRepository.deleteById(b.getId());
        }
    }
}
