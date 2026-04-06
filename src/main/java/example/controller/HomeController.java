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

import java.security.Principal; 
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import example.entity.Cinema;
import example.entity.Showtime;
import example.service.ShowtimeService;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private CinemaService cinemaService;

    @Autowired
    private ShowtimeService showtimeService;

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
    public String showHomePage(@RequestParam(value = "cinemaId", required = false) Integer cinemaId,
                               @RequestParam(value = "searchDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate searchDate,
                               Principal principal, Model model) { 
        prepareHomeData(principal, model);

        List<Cinema> cinemas = (List<Cinema>) model.getAttribute("cinemas");

        // 1. Nếu không chọn rạp, tự động chọn Rạp có suất chiếu đầu tiên
        if (cinemaId == null && cinemas != null) {
            for (Cinema c : cinemas) {
                if (!showtimeService.getActiveMovieIds(c.getId()).isEmpty()) {
                    cinemaId = c.getId();
                    break;
                }
            }
            if (cinemaId == null && !cinemas.isEmpty()) {
                cinemaId = cinemas.get(0).getId();
            }
        }

        // 2. Chỉ lấy những ngày CÓ lịch chiếu trong DB đối với rạp đã chọn
        List<LocalDate> availableDates = showtimeService.getAvailableDates(cinemaId);

        // 3. Nếu chưa request ngày nào, tự động chọn ngày gần nhất có suất chiếu
        if (searchDate == null) {
            if (!availableDates.isEmpty()) {
                searchDate = availableDates.get(0);
            } else {
                searchDate = LocalDate.now();
            }
        }

        List<Showtime> showtimes;
        if (cinemaId != null) {
            showtimes = showtimeService.getSchedule(cinemaId, searchDate);
        } else {
            showtimes = showtimeService.getShowtimesByDate(searchDate);
        }

        Map<Integer, List<Showtime>> showtimeMap = showtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getMovie().getId(), LinkedHashMap::new, Collectors.toList()));

        List<Movie> allMovies = (List<Movie>) model.getAttribute("dangChieu"); 
        List<Movie> sapChieu = (List<Movie>) model.getAttribute("sapChieu");
        List<Movie> dacBiet = (List<Movie>) model.getAttribute("dacBiet");

        List<Movie> combinedMovies = new ArrayList<>();
        if (allMovies != null) combinedMovies.addAll(allMovies);
        if (sapChieu != null) combinedMovies.addAll(sapChieu);
        if (dacBiet != null) combinedMovies.addAll(dacBiet);

        List<Movie> filteredMovies = combinedMovies.stream()
            .distinct()
            .filter(m -> showtimeMap.containsKey(m.getId())) // CHỈ lấy phim CÓ lịch chiếu
            .collect(Collectors.toList());

        model.addAttribute("availableDates", availableDates);
        model.addAttribute("searchDate", searchDate);
        model.addAttribute("searchCinemaId", cinemaId);
        model.addAttribute("showtimeMap", showtimeMap);
        model.addAttribute("moviesList", filteredMovies);

        return "user/home";
    }

    @GetMapping("/movies")
    public String showMoviesPage(@RequestParam(value = "searchName", required = false) String searchName,
                                 @RequestParam(value = "searchDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate searchDate,
                                 @RequestParam(value = "cinemaId", required = false) Integer cinemaId,
                                 Principal principal, Model model) {
        prepareHomeData(principal, model);
        
        List<Movie> dangChieu = (List<Movie>) model.getAttribute("dangChieu");
        List<Movie> sapChieu = (List<Movie>) model.getAttribute("sapChieu");
        List<Movie> dacBiet = (List<Movie>) model.getAttribute("dacBiet");

        // 1. Luôn lấy danh sách ID rạp có suất chiếu (để ẩn/hiện nút Mua vé)
        List<Integer> activeMovieIds = showtimeService.getActiveMovieIds(cinemaId);
        model.addAttribute("activeMovieIds", activeMovieIds);

        // 2. Lọc theo Rạp
        if (cinemaId != null) {
            filterListByAllowedIds(dangChieu, activeMovieIds);
            filterListByAllowedIds(sapChieu, activeMovieIds);
            filterListByAllowedIds(dacBiet, activeMovieIds);
            model.addAttribute("searchCinemaId", cinemaId);
        }

        // 3. Lọc theo Tên Phim
        if (searchName != null && !searchName.trim().isEmpty()) {
            filterListByName(dangChieu, searchName);
            filterListByName(sapChieu, searchName);
            filterListByName(dacBiet, searchName);
            model.addAttribute("searchName", searchName);
        }

        if (searchDate != null) {
            model.addAttribute("searchDate", searchDate);
            List<Showtime> showtimes = showtimeService.getShowtimesByDate(searchDate); 
            Map<Integer, List<Showtime>> showtimeMap = showtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getMovie().getId()));
                
            model.addAttribute("showtimeMap", showtimeMap);
            
            filterListByShowtimeMap(dangChieu, showtimeMap);
            filterListByShowtimeMap(sapChieu, showtimeMap);
            filterListByShowtimeMap(dacBiet, showtimeMap);
        }

        return "user/movies";
    }

    private void filterListByName(List<Movie> list, String name) {
        if (list == null) return;
        list.removeIf(m -> !m.getMovieName().toLowerCase().contains(name.toLowerCase()));
    }

    private void filterListByShowtimeMap(List<Movie> list, Map<Integer, List<Showtime>> showtimeMap) {
        if (list == null) return;
        list.removeIf(m -> !showtimeMap.containsKey(m.getId()) || showtimeMap.get(m.getId()).isEmpty());
    }

    private void filterListByAllowedIds(List<Movie> list, List<Integer> allowedIds) {
        if (list == null) return;
        list.removeIf(m -> !allowedIds.contains(m.getId()));
    }

    @GetMapping("/movie/detail/{id}")
    public String showDetail(@PathVariable("id") int id, Model model) {
        Movie movie = movieService.getMovieById(id);
        model.addAttribute("movie", movie);
        model.addAttribute("cinemas", cinemaService.getAllCinemas());
        return "user/detail";
    }
}