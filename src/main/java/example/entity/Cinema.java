package example.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "Cinemas")
public class Cinema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String cinemaName;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String city;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String address;

    @Column(nullable = false)
    private String phone;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String status;

    // Quan hệ 1-N: Một rạp có nhiều phòng
    // Dùng EAGER để khi query Cinema sẽ lấy luôn List<CinemaRoom>
    @OneToMany(mappedBy = "cinema", fetch = FetchType.EAGER)
    @JsonManagedReference // Quản lý vòng lặp JSON (Chiều xuôi)
    private List<CinemaRoom> rooms;

    public Cinema() {}

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCinemaName() { return cinemaName; }
    public void setCinemaName(String cinemaName) { this.cinemaName = cinemaName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<CinemaRoom> getRooms() { return rooms; }
    public void setRooms(List<CinemaRoom> rooms) { this.rooms = rooms; }
}