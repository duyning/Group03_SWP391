package example.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "CinemaRooms")
public class CinemaRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String roomName;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String roomType;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String status;

    // Quan hệ 1-N với ghế
    @OneToMany(mappedBy = "cinemaRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Seat> seats;

    @ManyToOne
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    public CinemaRoom() {}

    public CinemaRoom(int id, String roomName, String roomType, String status, List<Seat> seats, Cinema cinema) {
        this.id = id;
        this.roomName = roomName;
        this.roomType = roomType;
        this.status = status;
        this.seats = seats;
        this.cinema = cinema;
    }

    // Hàm lấy tổng số ghế dựa trên danh sách thực tế
    @Transient // Không tạo cột này trong DB, chỉ dùng để hiển thị
    public int getTotalSeats() {
        return (seats != null) ? seats.size() : 0;
    }

    // --- Giữ nguyên các Getter/Setter cũ ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
    public Cinema getCinema() { return cinema; }
    public void setCinema(Cinema cinema) { this.cinema = cinema; }
}