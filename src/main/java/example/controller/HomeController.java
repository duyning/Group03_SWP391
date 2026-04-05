package example.controller;

import example.entity.Account;
import example.entity.Cinema;
import example.entity.Movie;
import example.service.AccountService; // <-- Import thêm
import example.service.CinemaService;
import example.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal; // <-- Import thêm
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private CinemaService cinemaService;

    @Autowired
    private AccountService accountService; // <-- Tiêm thêm AccountService

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    private void prepareHomeData(Principal principal, Model model) {
        // --- 1. LOGIC TÀI KHOẢN (Chuyển từ AccountController sang) ---
        // Kiểm tra xem user đã đăng nhập chưa (principal != null)
        if (principal != null) {
            String email = principal.getName();
            Account acc = accountService.findByEmail(email);
            model.addAttribute("account", acc);
        }

        // --- 2. LOGIC PHIM VÀ RẠP (Giữ nguyên) ---
        List<Movie> allMovies = movieService.getAllMovies();

        List<Movie> dangChieu = allMovies.stream()
                .filter(m -> "Đang chiếu".equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());

        List<Movie> sapChieu = allMovies.stream()
                .filter(m -> "Sắp chiếu".equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());

        List<Movie> dacBiet = allMovies.stream()
                .filter(m -> "Suất chiếu đặc biệt".equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("dangChieu", dangChieu);
        model.addAttribute("sapChieu", sapChieu);
        model.addAttribute("dacBiet", dacBiet);

        List<Cinema> cinemas = cinemaService.getAllCinemas();
        model.addAttribute("cinemas", cinemas);
    }

    @GetMapping("/home")
    public String showHomePage(Principal principal, Model model) { // <-- Thêm Principal vào tham số
        prepareHomeData(principal, model);
        return "user/home";
    }

    @GetMapping("/movies")
    public String showMoviesPage(Principal principal, Model model) {
        prepareHomeData(principal, model);
        return "user/movies";
    }

    @GetMapping("/movie/detail/{id}")
    public String showDetail(@PathVariable("id") int id, Model model) {
        Movie movie = movieService.getMovieById(id);
        model.addAttribute("movie", movie);
        model.addAttribute("cinemas", cinemaService.getAllCinemas());
        return "user/detail";
    }
}