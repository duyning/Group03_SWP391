package example.repository;

import example.entity.Comment;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CommentRepository {

    private final SessionFactory sessionFactory;

    public CommentRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Comment comment) {
        sessionFactory.getCurrentSession().persist(comment);
    }

    public Comment findById(int id) {
        return sessionFactory.getCurrentSession().get(Comment.class, id);
    }

    public void delete(Comment comment) {
        sessionFactory.getCurrentSession().remove(comment);
    }

    // Lấy danh sách comment của 1 bài viết, từ cũ nhất đến mới nhất
    public List<Comment> findByPostId(int postId) {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt ASC", Comment.class)
                .setParameter("postId", postId)
                .getResultList();
    }
}
