package example.repository;

import example.entity.Combo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ComboRepository {

    @Autowired
    private SessionFactory sessionFactory;

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    public List<Combo> findAll() {
        return getCurrentSession()
                .createQuery("FROM Combo", Combo.class)
                .list();
    }

    public Combo findById(Integer id) {
        return getCurrentSession().get(Combo.class, id);
    }

    public void save(Combo combo) {
        if (combo.getId() == null) {
            getCurrentSession().persist(combo); // Thêm mới
        } else {
            getCurrentSession().merge(combo);   // Cập nhật (tránh lỗi duplicate session)
        }
    }

    public void deleteById(Integer id) {
        Combo combo = findById(id);
        if (combo != null) {
            getCurrentSession().remove(combo);
        }
    }

    // --- THÊM HÀM TÌM KIẾM ---
    public List<Combo> search(String name, String description, Double maxPrice, Boolean status) {
        Session session = getCurrentSession();
        StringBuilder hql = new StringBuilder("FROM Combo c WHERE 1=1 ");

        // Xây dựng câu lệnh HQL động
        if (name != null && !name.trim().isEmpty()) {
            hql.append("AND lower(c.comboName) LIKE :name ");
        }
        if (description != null && !description.trim().isEmpty()) {
            hql.append("AND lower(c.description) LIKE :description ");
        }
        if (maxPrice != null) {
            hql.append("AND c.price <= :maxPrice ");
        }
        if (status != null) {
            hql.append("AND c.active = :status ");
        }

        // Tạo query
        org.hibernate.query.Query<Combo> query = session.createQuery(hql.toString(), Combo.class);

        // Truyền tham số vào query
        if (name != null && !name.trim().isEmpty()) {
            query.setParameter("name", "%" + name.trim().toLowerCase() + "%");
        }
        if (description != null && !description.trim().isEmpty()) {
            query.setParameter("description", "%" + description.trim().toLowerCase() + "%");
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        if (status != null) {
            query.setParameter("status", status);
        }

        return query.list();
    }
}