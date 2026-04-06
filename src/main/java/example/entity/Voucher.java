package example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "Mã voucher không được để trống")
    @Column(unique = true, nullable = false)
    private String code;

    @Min(value = 1, message = "Giảm tối thiểu 1%")
    @Max(value = 100, message = "Giảm tối đa 100%")
    private int discountPercent;

    private LocalDateTime createdAt;

    @NotNull(message = "Vui lòng chọn ngày hết hạn")
    private LocalDateTime expiryDate;

    private boolean active = true;

    // Voucher cá nhân (VIP reward) - không hiển thị trong trang public voucher
    @Column(name = "is_personal", nullable = false, columnDefinition = "BIT NOT NULL DEFAULT 0")
    private boolean isPersonal = false;

    // Chạy trước khi lưu vào database để tự lấy ngày giờ hiện tại
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isPersonal() {
        return isPersonal;
    }

    public void setPersonal(boolean personal) {
        isPersonal = personal;
    }
}
