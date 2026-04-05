package example.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "Showtimes")
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private CinemaRoom room;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false, columnDefinition = "TIME")
    private LocalTime startTime;

    // SỬA: Thêm columnDefinition = "TIME"
    @Column(nullable = false, columnDefinition = "TIME")
    private LocalTime endTime;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String status;

    public Showtime() {}

    // Getter & Setter (Đã xóa getPrice/setPrice)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public CinemaRoom getRoom() { return room; }
    public void setRoom(CinemaRoom room) { this.room = room; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}