package example.entity;

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

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String city;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String address;


    private String phone;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String status; // Hoạt động, Bảo trì

    // Thêm liên kết ngược tới bảng CinemaRoom
    @OneToMany(mappedBy = "cinema", fetch = FetchType.LAZY)
    private List<CinemaRoom> rooms;

    public Cinema() {
    }

    public Cinema(int id, String cinemaName, String city, String address, String phone, String status, List<CinemaRoom> rooms) {
        this.id = id;
        this.cinemaName = cinemaName;
        this.city = city;
        this.address = address;
        this.phone = phone;
        this.status = status;
        this.rooms = rooms;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<CinemaRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<CinemaRoom> rooms) {
        this.rooms = rooms;
    }
}