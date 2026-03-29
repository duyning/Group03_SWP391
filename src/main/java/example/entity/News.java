package example.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news")
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", columnDefinition = "nvarchar(255)")
    private String title;

    @Column(name = "imageUrl", columnDefinition = "nvarchar(500)")
    private String imageUrl;

    // Trạng thái: true (Hiển thị), false (Ẩn)
    private boolean status;

    @Column(name = "content", columnDefinition = "nvarchar(MAX)")
    private String content;

    // Tự động gán ngày giờ hiện tại khi tạo mới
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    // Lifecycle hook: Tự động chạy trước khi insert vào DB
    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
