package example.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // Quan hệ N-1: Nhiều phòng thuộc 1 rạp
    @ManyToOne
    @JoinColumn(name = "cinema_id", nullable = false)
    @JsonBackReference // Ngăn vòng lặp JSON (Chiều ngược)
    private Cinema cinema;

    // Quan hệ 1-N: Một phòng có nhiều ghế
    @OneToMany(mappedBy = "cinemaRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Không cần load ghế khi xem lịch chiếu
    private List<Seat> seats;

    public CinemaRoom() {}

    @Transient
    public int getTotalSeats() {
        return (seats != null) ? seats.size() : 0;
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Cinema getCinema() { return cinema; }
    public void setCinema(Cinema cinema) { this.cinema = cinema; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
}