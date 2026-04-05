package example.repository;

import example.entity.Account;
import jakarta.persistence.NoResultException;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountRepository {

    private final SessionFactory sessionFactory;

    public AccountRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Account account) {
        sessionFactory.getCurrentSession().save(account);
    }

    public Account findByEmail(String email) {
        try {
            return sessionFactory.getCurrentSession()
                    .createQuery("FROM Account WHERE email = :email", Account.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void update(Account account) {
        sessionFactory.getCurrentSession().merge(account);
    }

    public boolean existsByEmail(String email) {
        String hql = "SELECT COUNT(a) FROM Account a WHERE a.email = :email";
        Long count = sessionFactory.getCurrentSession()
                .createQuery(hql, Long.class)
                .setParameter("email", email)
                .uniqueResult();
        return count != null && count > 0;
    }

    public List<Account> findByMembershipLevel(example.entity.MembershipLevel level) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Account WHERE membershipLevel = :level AND status = true", Account.class)
                .setParameter("level", level)
                .getResultList();
    }

    public List<Account> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Account ORDER BY accountID ASC", Account.class)
                .getResultList();
    }

    public Account findById(int id) {
        return sessionFactory.getCurrentSession().get(Account.class, id);
    }

    public void toggleStatus(int id) {
        Account account = findById(id);
        if (account != null) {
            account.setStatus(!account.isStatus());
            sessionFactory.getCurrentSession().merge(account);
        }
    }
}
