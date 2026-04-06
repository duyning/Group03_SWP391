package example.repository;

import example.entity.TicketPrice;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class TicketPriceRepository {

    @Autowired
    private SessionFactory sessionFactory;

    // Helper method để lấy session hiện tại
    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    // 1. Lấy danh sách tất cả
    public List<TicketPrice> findAll() {
        return getCurrentSession()
                .createQuery("FROM TicketPrice", TicketPrice.class)
                .list();
    }

    // 2. Tìm theo ID
    public TicketPrice findById(Integer id) {
        return getCurrentSession().get(TicketPrice.class, id);
    }

    // 3. Lưu mới hoặc Cập nhật (Save Or Update)
    public void save(TicketPrice ticketPrice) {
        if (ticketPrice.getId() == null) {
            // Nếu là thêm mới -> Dùng persist
            getCurrentSession().persist(ticketPrice);
        } else {
            // Nếu là cập nhật -> Dùng merge để tránh lỗi "Different object..."
            getCurrentSession().merge(ticketPrice);
        }
    }

    // 4. Xóa theo ID
    public void deleteById(Integer id) {
        // Cách 1: Tìm object rồi xóa (An toàn hơn để Hibernate quản lý cache)
        TicketPrice ticketPrice = findById(id);
        if (ticketPrice != null) {
            getCurrentSession().remove(ticketPrice);
        }
    }
}