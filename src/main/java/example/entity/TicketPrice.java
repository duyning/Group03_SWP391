package example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Ticket_prices")
public class TicketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Thêm columnDefinition = "nvarchar(255)" để lưu tiếng Việt có dấu
    @Column(name = "ticket_name", nullable = false, columnDefinition = "nvarchar(255)")
    private String ticketName; // Ví dụ: "Vé Người Lớn", "Vé HSSV"

    // Sửa seat_type sang nvarchar để lưu tên tiếng Việt
    @Column(name = "seat_type", nullable = false, columnDefinition = "nvarchar(50)")
    private String seatType; // Lưu trực tiếp: "Ghế Thường", "Ghế VIP", "Ghế Đôi"

    @Column(name = "price_standard", nullable = false)
    private Double priceStandard; // Giá Thứ 2 -> Thứ 5

    @Column(name = "price_weekend", nullable = false)
    private Double priceWeekend; // Giá Thứ 6 -> Chủ Nhật

    @Column(name = "active")
    private Boolean active = true; // Trạng thái hoạt động

    // --- CONSTRUCTORS ---
    public TicketPrice() {}

    public TicketPrice(Integer id, String ticketName, String seatType, Double priceStandard, Double priceWeekend, Boolean active) {
        this.id = id;
        this.ticketName = ticketName;
        this.seatType = seatType;
        this.priceStandard = priceStandard;
        this.priceWeekend = priceWeekend;
        this.active = active;
    }

    // --- GETTERS & SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTicketName() { return ticketName; }
    public void setTicketName(String ticketName) { this.ticketName = ticketName; }

    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }

    public Double getPriceStandard() { return priceStandard; }
    public void setPriceStandard(Double priceStandard) { this.priceStandard = priceStandard; }

    public Double getPriceWeekend() { return priceWeekend; }
    public void setPriceWeekend(Double priceWeekend) { this.priceWeekend = priceWeekend; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}