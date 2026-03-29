package example.controller;

import example.entity.Post;
import example.service.AccountService;
import example.service.BlogService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/blog")
public class BlogController {

    private final BlogService blogService;
    private final AccountService accountService;

    public BlogController(BlogService blogService, AccountService accountService) {
        this.blogService = blogService;
        this.accountService = accountService;
    }

    // GET /blog — Danh sách bài viết
    @GetMapping
    public String listPosts(Model model, Principal principal) {
        List<Post> posts = blogService.getAllPosts();
        model.addAttribute("posts", posts);
        if (principal != null) {
            model.addAttribute("currentUser", accountService.findByEmail(principal.getName()));
        }
        return "user/blog/index";
    }

    // GET /blog/create — Form tạo bài mới
    @GetMapping("/create")
    public String showCreateForm() {
        return "user/blog/create";
    }

    // POST /blog/create — Lưu bài mới
    @PostMapping("/create")
    public String createPost(@RequestParam String title,
                             @RequestParam String content,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            blogService.createPost(title.trim(), content.trim(), principal.getName());
            redirectAttributes.addFlashAttribute("success", "Đăng bài thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/blog";
    }

    // GET /blog/{id} — Chi tiết bài viết
    @GetMapping("/{id}")
    public String viewPost(@PathVariable int id, Model model, Principal principal) {
        try {
            Post post = blogService.getPostById(id);
            model.addAttribute("post", post);
            model.addAttribute("likeCount", blogService.getLikeCount(id));
            if (principal != null) {
                model.addAttribute("hasLiked", blogService.hasLiked(id, principal.getName()));
                model.addAttribute("currentUser", accountService.findByEmail(principal.getName()));
            } else {
                model.addAttribute("hasLiked", false);
            }
        } catch (RuntimeException e) {
            return "redirect:/blog";
        }
        return "user/blog/detail";
    }

    // POST /blog/{id}/comment — Thêm bình luận
    @PostMapping("/{id}/comment")
    public String addComment(@PathVariable int id,
                             @RequestParam String content,
                             @RequestParam(required = false) Integer parentId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            blogService.addComment(id, content, principal.getName(), parentId);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/blog/" + id;
    }

    // GET /blog/{id}/edit — Form sửa bài
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable int id, Model model, Principal principal) {
        try {
            Post post = blogService.getPostById(id);
            if (!post.getAuthor().getEmail().equals(principal.getName())) {
                return "redirect:/blog/" + id;
            }
            model.addAttribute("post", post);
            return "user/blog/edit";
        } catch (RuntimeException e) {
            return "redirect:/blog";
        }
    }

    // POST /blog/{id}/edit — Lưu sửa bài
    @PostMapping("/{id}/edit")
    public String updatePost(@PathVariable int id,
                             @RequestParam String title,
                             @RequestParam String content,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            blogService.updatePost(id, title.trim(), content.trim(), principal.getName());
            redirectAttributes.addFlashAttribute("success", "Cập nhật bài viết thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/blog/" + id;
    }

    // POST /blog/comment/{commentId}/edit — Lưu sửa bình luận
    @PostMapping("/comment/{commentId}/edit")
    public String updateComment(@PathVariable int commentId,
                                @RequestParam String content,
                                @RequestParam int postId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            blogService.updateComment(commentId, content.trim(), principal.getName());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/blog/" + postId;
    }

    // POST /blog/comment/{commentId}/delete — Xóa bình luận
    @PostMapping("/comment/{commentId}/delete")
    public String deleteComment(@PathVariable int commentId,
                                @RequestParam int postId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            blogService.deleteComment(commentId, principal.getName());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/blog/" + postId;
    }

    // POST /blog/{id}/like — Toggle Like (AJAX, trả về JSON)
    @PostMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable int id, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("error", "Bạn cần đăng nhập để thả tim.");
            return ResponseEntity.status(401).body(response);
        }
        try {
            long newCount = blogService.toggleLike(id, principal.getName());
            boolean liked = blogService.hasLiked(id, principal.getName());
            response.put("likeCount", newCount);
            response.put("liked", liked);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /blog/{id}/delete — Xóa bài viết (chỉ tác giả)
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable int id, Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            blogService.deletePost(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Đã xóa bài viết.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/blog";
    }
}
