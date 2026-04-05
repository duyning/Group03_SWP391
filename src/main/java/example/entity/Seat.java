package example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Seats",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"row_name", "seat_column", "room_id"})
        }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "row_name", nullable = false, length = 10)
    private String rowName; // A, B, C...

    @Column(name = "seat_column", nullable = false)
    private int seatColumn; // 1, 2, 3...

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber; // A1, A2...

    @Column(name = "seat_type", nullable = false, length = 50)
    private String seatType; // NORMAL, VIP, SWEETBOX

    @Column(name = "status", nullable = false, length = 50)
    private String status; // AVAILABLE, BOOKED, MAINTENANCE

    // ====== RELATION ======
    // Trong Seat.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore // Thêm dòng này
    private CinemaRoom cinemaRoom;

    // ====== CONSTRUCTORS ======
    public Seat() {}

    public Seat(String rowName, int seatColumn, String seatType, String status, CinemaRoom cinemaRoom) {
        this.rowName = rowName;
        this.seatColumn = seatColumn;
        this.seatType = seatType;
        this.status = status;
        this.cinemaRoom = cinemaRoom;
        this.seatNumber = rowName + seatColumn;
    }

    // ====== AUTO BUILD seatNumber ======
    @PrePersist
    @PreUpdate
    private void buildSeatNumber() {
        this.seatNumber = rowName + seatColumn;
    }

    // ====== GETTERS & SETTERS ======
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRowName() {
        return rowName;
    }

    public void setRowName(String rowName) {
        this.rowName = rowName;
    }

    public int getSeatColumn() {
        return seatColumn;
    }

    public void setSeatColumn(int seatColumn) {
        this.seatColumn = seatColumn;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CinemaRoom getCinemaRoom() {
        return cinemaRoom;
    }

    public void setCinemaRoom(CinemaRoom cinemaRoom) {
        this.cinemaRoom = cinemaRoom;
    }
}
