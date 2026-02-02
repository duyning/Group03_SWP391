package example.controller;

import example.entity.Movie;
import example.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    private MovieService movieService;

    @GetMapping("/home")
    public String showHomePage(Model model) {
        // 1. Lấy toàn bộ danh sách phim từ Database
        List<Movie> allMovies = movieService.getAllMovies();

        // 2. Lọc phim "Đang chiếu" (Sử dụng Java Stream để lọc nhanh)
        List<Movie> dangChieu = allMovies.stream()
                .filter(m -> "Đang chiếu".equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());

        // 3. Lọc phim "Sắp chiếu"
        List<Movie> sapChieu = allMovies.stream()
                .filter(m -> "Sắp chiếu".equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());

        // Lọc phim Suất chiếu đặc biệt
        List<Movie> dacBiet = allMovies.stream()
                .filter(m -> "Suất chiếu đặc biệt".equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());

        // 4. Đẩy 2 danh sách riêng biệt vào Model
        model.addAttribute("dangChieu", dangChieu);
        model.addAttribute("sapChieu", sapChieu);
        model.addAttribute("dacBiet", dacBiet);

        // Trả về trang home nằm trong WEB-INF/user/
        return "user/home";
    }

    @GetMapping("/movie/detail/{id}")
    public String showDetail(@PathVariable("id") int id, Model model) {
        Movie movie = movieService.getMovieById(id); // Lấy phim từ DB
        model.addAttribute("movie", movie); // Đẩy dữ liệu sang View
        return "user/detail"; // Trả về file detail.html hoàn chỉnh
    }
}