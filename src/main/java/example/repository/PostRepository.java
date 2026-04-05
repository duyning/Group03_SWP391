package example.repository;

import example.entity.Post;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostRepository {

    private final SessionFactory sessionFactory;

    public PostRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Post post) {
        sessionFactory.getCurrentSession().persist(post);
    }

    public void update(Post post) {
        sessionFactory.getCurrentSession().merge(post);
    }

    public void delete(Post post) {
        sessionFactory.getCurrentSession().remove(post);
    }

    public Post findById(int id) {
        return sessionFactory.getCurrentSession().get(Post.class, id);
    }

    // Lấy tất cả bài viết, sắp xếp mới nhất lên đầu
    public List<Post> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT p FROM Post p ORDER BY p.createdAt DESC", Post.class)
                .getResultList();
    }
}
