package example.service;

import example.entity.*;
import example.repository.AccountRepository;
import example.repository.CommentRepository;
import example.repository.PostLikeRepository;
import example.repository.PostRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BlogServiceImpl implements BlogService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final AccountRepository accountRepository;

    public BlogServiceImpl(PostRepository postRepository,
                           CommentRepository commentRepository,
                           PostLikeRepository postLikeRepository,
                           AccountRepository accountRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.accountRepository = accountRepository;
    }

    // ===== POST =====

    @Override
    @Transactional
    public void createPost(String title, String content, String authorEmail) {
        Account author = accountRepository.findByEmail(authorEmail);
        if (author == null) throw new RuntimeException("Tài khoản không tồn tại.");
        Post post = new Post(title, content, author);
        postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Post getPostById(int id) {
        Post post = postRepository.findById(id);
        if (post == null) throw new RuntimeException("Bài viết không tồn tại.");
        Hibernate.initialize(post.getAuthor());
        Hibernate.initialize(post.getComments());
        Hibernate.initialize(post.getLikes());
        for (Comment c : post.getComments()) {
            Hibernate.initialize(c.getAuthor());
            Hibernate.initialize(c.getReplies());
            for (Comment reply : c.getReplies()) {
                Hibernate.initialize(reply.getAuthor());
            }
        }
        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        for (Post p : posts) {
            Hibernate.initialize(p.getAuthor());
            Hibernate.initialize(p.getComments());
            Hibernate.initialize(p.getLikes());
        }
        return posts;
    }

    @Override
    @Transactional
    public void deletePost(int postId, String currentUserEmail) {
        Post post = postRepository.findById(postId);
        if (post == null) throw new RuntimeException("Bài viết không tồn tại.");
        
        Account currentUser = accountRepository.findByEmail(currentUserEmail);
        boolean isOwner = post.getAuthor().getEmail().equals(currentUserEmail);
        boolean isModerator = currentUser.getRole() == Role.MANAGER;
        
        if (!isOwner && !isModerator) {
            throw new RuntimeException("Bạn không có quyền xóa bài viết này.");
        }
        
        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void updatePost(int postId, String title, String content, String currentUserEmail) {
        Post post = postRepository.findById(postId);
        if (post == null) throw new RuntimeException("Bài viết không tồn tại.");
        if (!post.getAuthor().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Bạn không có quyền sửa bài viết này.");
        }
        post.setTitle(title);
        post.setContent(content);
    }

    // ===== COMMENT =====

    @Override
    @Transactional
    public void addComment(int postId, String content, String authorEmail, Integer parentId) {
        Account author = accountRepository.findByEmail(authorEmail);
        Post post = postRepository.findById(postId);
        if (author == null || post == null) throw new RuntimeException("Dữ liệu không hợp lệ.");
        if (content == null || content.trim().isEmpty()) throw new RuntimeException("Nội dung bình luận không được để trống.");
        
        Comment comment = new Comment(content.trim(), author, post);
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId);
            if (parent != null) {
                comment.setParentComment(parent);
            }
        }
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void updateComment(int commentId, String content, String currentUserEmail) {
        Comment comment = commentRepository.findById(commentId);
        if (comment == null) throw new RuntimeException("Bình luận không tồn tại.");
        if (!comment.getAuthor().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Bạn không có quyền sửa bình luận này.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung không được để trống.");
        }
        comment.setContent(content.trim());
    }

    @Override
    @Transactional
    public void deleteComment(int commentId, String currentUserEmail) {
        Comment comment = commentRepository.findById(commentId);
        if (comment == null) throw new RuntimeException("Bình luận không tồn tại.");
        
        Account currentUser = accountRepository.findByEmail(currentUserEmail);
        boolean isOwner = comment.getAuthor().getEmail().equals(currentUserEmail);
        boolean isModerator = currentUser.getRole() == Role.MANAGER;
        
        if (!isOwner && !isModerator) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này.");
        }
        
        commentRepository.delete(comment);
    }

    // ===== LIKE (Toggle) =====

    @Override
    @Transactional
    public long toggleLike(int postId, String accountEmail) {
        Account account = accountRepository.findByEmail(accountEmail);
        Post post = postRepository.findById(postId);
        if (account == null || post == null) throw new RuntimeException("Dữ liệu không hợp lệ.");

        PostLike existing = postLikeRepository.findByAccountAndPost(account.getAccountID(), postId);
        if (existing != null) {
            // Đã Like rồi → Unlike
            postLikeRepository.delete(existing);
        } else {
            // Chưa Like → Like
            postLikeRepository.save(new PostLike(account, post));
        }
        return postLikeRepository.countByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLiked(int postId, String accountEmail) {
        Account account = accountRepository.findByEmail(accountEmail);
        if (account == null) return false;
        return postLikeRepository.findByAccountAndPost(account.getAccountID(), postId) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(int postId) {
        return postLikeRepository.countByPostId(postId);
    }
}
