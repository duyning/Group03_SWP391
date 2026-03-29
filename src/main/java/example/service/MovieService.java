package example.service;

import example.entity.Movie;
import example.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class MovieService {

    // Tiêm MovieRepository vào để thực hiện các thao tác dữ liệu
    @Autowired
    private MovieRepository movieRepository;

    /**
     * Lấy toàn bộ danh sách phim từ Database thông qua Repository
     * @return List<Movie>
     */
    @Transactional(readOnly = true)
    public List<Movie> getAllMovies() {
        // Gọi hàm findAll() đã viết trong MovieRepository
        return movieRepository.findAll();
    }

    /**
     * Lưu một bộ phim mới hoặc cập nhật phim cũ vào SQL Server
     * @param movie Đối tượng phim cần lưu
     */
    @Transactional
    public void saveMovie(Movie movie) {
        movieRepository.saveOrUpdate(movie);
    }

    @Transactional
    public void deleteMovie(int id) {
        // Tìm đối tượng phim trước khi xóa
        Movie movie = movieRepository.getMovieById(id);
        if (movie != null) {
            movieRepository.delete(movie);
        }
    }

    @Transactional
    public Movie getMovieById(int id) {
        return movieRepository.getMovieById(id);
    }

    @Transactional
    public List<Movie> searchMovies(String name, String type, String author, String duration, String status, String releaseDate) {
        // Logic tìm kiếm sử dụng Repository tại đây
        return movieRepository.findByMultiCriteria(name, type, author, duration, status, releaseDate);
    }

    @Transactional
    public List<Movie> getMoviesPaged(int page, int pageSize) {
        // Gọi xuống Repository để lấy dữ liệu theo trang
        return movieRepository.findPaged(page, pageSize);
    }

    @Transactional
    public long getTotalCount() {
        // Gọi xuống Repository để đếm tổng số phim
        return movieRepository.countAll();
    }
}