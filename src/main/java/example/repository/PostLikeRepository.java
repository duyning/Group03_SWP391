package example.repository;

import example.entity.PostLike;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PostLikeRepository {

    private final SessionFactory sessionFactory;

    public PostLikeRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(PostLike like) {
        sessionFactory.getCurrentSession().persist(like);
    }

    public void delete(PostLike like) {
        sessionFactory.getCurrentSession().remove(like);
    }

    // Kiểm tra user đã Like bài này chưa
    public PostLike findByAccountAndPost(int accountId, int postId) {
        try {
            return sessionFactory.getCurrentSession()
                    .createQuery("SELECT l FROM PostLike l WHERE l.account.accountID = :accountId AND l.post.id = :postId", PostLike.class)
                    .setParameter("accountId", accountId)
                    .setParameter("postId", postId)
                    .getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }

    // Đếm tổng số Like của 1 bài
    public long countByPostId(int postId) {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(l) FROM PostLike l WHERE l.post.id = :postId", Long.class)
                .setParameter("postId", postId)
                .getSingleResult();
    }
}
