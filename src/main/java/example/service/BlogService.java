package example.service;

import example.entity.Post;
import java.util.List;

public interface BlogService {
    // Post operations
    void createPost(String title, String content, String authorEmail);
    Post getPostById(int id);
    List<Post> getAllPosts();
    void deletePost(int postId, String currentUserEmail);
    void updatePost(int postId, String title, String content, String currentUserEmail);

    // Comment operations
    void addComment(int postId, String content, String authorEmail, Integer parentId);
    void updateComment(int commentId, String content, String currentUserEmail);
    // Xóa comment
    void deleteComment(int commentId, String currentUserEmail);

    // Like operations (toggle: Like nếu chưa like, Unlike nếu đã like)
    // Trả về số like hiện tại sau khi toggle
    long toggleLike(int postId, String accountEmail);
    boolean hasLiked(int postId, String accountEmail);
    long getLikeCount(int postId);
}
