package example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Movies")
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Sử dụng NVARCHAR để lưu tiếng Việt
    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String movieName;

    @Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String title;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String type;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String duration;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String status;

    private String imgUrl;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String director;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String actors;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String releaseDate;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String language;      // Ngôn ngữ (vd: 2D Phụ đề)

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String trailerUrl;

    public Movie() {
    }

    public Movie(int id, String movieName, String title, String type, String duration, String status, String imgUrl, String director, String actors, String releaseDate, String language, String trailerUrl) {
        this.id = id;
        this.movieName = movieName;
        this.title = title;
        this.type = type;
        this.duration = duration;
        this.status = status;
        this.imgUrl = imgUrl;
        this.director = director;
        this.actors = actors;
        this.releaseDate = releaseDate;
        this.language = language;
        this.trailerUrl = trailerUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getActors() {
        return actors;
    }

    public void setActors(String actors) {
        this.actors = actors;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }
}