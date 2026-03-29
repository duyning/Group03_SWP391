package example.repository;

import example.entity.Voucher;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class VoucherRepository {
    @Autowired
    private SessionFactory sessionFactory;

    @Transactional(readOnly = true)
    public List<Voucher> findAll() {
        Session session = sessionFactory.getCurrentSession();
        // Sắp xếp theo ngày tạo mới nhất lên đầu
        String hql = "FROM Voucher v ORDER BY v.createdAt DESC";
        return session.createQuery(hql, Voucher.class).getResultList();
    }

    @Transactional(readOnly = true)
    public Voucher findById(int id) {
        return sessionFactory.getCurrentSession().get(Voucher.class, id);
    }

    @Transactional(readOnly = true)
    public Voucher findByCode(String code) {
        Session session = sessionFactory.getCurrentSession();
        // Sử dụng UPPER để so sánh không phân biệt hoa thường
        String hql = "FROM Voucher v WHERE UPPER(v.code) = UPPER(:code)";
        return session.createQuery(hql, Voucher.class)
                .setParameter("code", code)
                .uniqueResult();
    }

    @Transactional
    public Voucher saveOrUpdate(Voucher voucher) {
        Session session = sessionFactory.getCurrentSession();
        if (voucher.getId() == 0) {
            session.persist(voucher);
            return voucher;
        } else {
            return session.merge(voucher);
        }
    }

    @Transactional
    public void delete(int id) {
        Session session = sessionFactory.getCurrentSession();
        Voucher voucher = session.get(Voucher.class, id);
        if (voucher != null) {
            session.remove(voucher);
        }
    }

    @Transactional(readOnly = true)
    public List<Voucher> findActiveVouchers() {
        Session session = sessionFactory.getCurrentSession();
        // Lấy các voucher còn hạn, đang active, và KHÔNG phải voucher cá nhân (VIP reward)
        String hql = "FROM Voucher v WHERE v.active = true AND v.expiryDate > CURRENT_TIMESTAMP AND v.isPersonal = false";
        return session.createQuery(hql, Voucher.class).getResultList();
    }
}
