package example.repository;

import example.entity.PasswordResetToken;
import jakarta.persistence.NoResultException;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenRepository {

    private final SessionFactory sessionFactory;

    public PasswordResetTokenRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(PasswordResetToken token) {
        sessionFactory.getCurrentSession().persist(token);
    }

    public PasswordResetToken findByToken(String token) {
        try {
            return sessionFactory.getCurrentSession()
                    .createQuery("FROM PasswordResetToken WHERE token = :token", PasswordResetToken.class)
                    .setParameter("token", token)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void delete(PasswordResetToken token) {
        sessionFactory.getCurrentSession().remove(token);
    }

    // Xóa các token cũ của email khi request reset lần mới
    public void deleteByEmail(String email) {
        sessionFactory.getCurrentSession()
                .createMutationQuery("DELETE FROM PasswordResetToken WHERE email = :email")
                .setParameter("email", email)
                .executeUpdate();
    }
}
