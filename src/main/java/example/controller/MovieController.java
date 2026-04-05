package example.controller;

import example.entity.Movie;
import example.service.MovieService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class MovieController {

    @Autowired
    private MovieService movieService;

    /**
     * @ModelAttribute này đảm bảo 'movieForm' luôn tồn tại trong Model của mọi Request.
     * Giải quyết lỗi: "Neither BindingResult nor plain target object for bean name 'movieForm' available"
     */
    @ModelAttribute("movieForm")
    public Movie setupMovieForm() {
        return new Movie();
    }

    // Hàm chính hiển thị danh sách có phân trang
    @GetMapping("/manager_movie")
    public String listMovies(@RequestParam(defaultValue = "1") int page, Model model) {
        int pageSize = 10;

        List<Movie> movies = movieService.getMoviesPaged(page, pageSize);
        long totalMovies = movieService.getTotalCount();
        int totalPages = (int) Math.ceil((double) totalMovies / pageSize);

        // Đẩy dữ liệu bảng và phân trang
        model.addAttribute("movies", movies);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalMovies", totalMovies);

        addMovieStatsToModel(model);

        return "admin/manager_movie";
    }

    // 1. Mở Form Sửa và đổ dữ liệu
    @GetMapping("/movie/edit/{id}")
    public String editMovieForm(@PathVariable("id") int id,
                                @RequestParam(defaultValue = "1") int page,
                                Model model) {
        // Lấy dữ liệu phim cần sửa
        Movie movie = movieService.getMovieById(id);

        // Đổ dữ liệu vào object form và kích hoạt hiển thị Modal
        model.addAttribute("movieForm", movie);
        model.addAttribute("isEdit", true);

        // QUAN TRỌNG: Vẫn phải nạp dữ liệu phân trang để trang web render không bị lỗi SUBTRACT
        return listMovies(page, model);
    }

    @GetMapping("/movie/search")
    public String searchMovies(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "releaseDate", required = false) String releaseDate,
            Model model) {

        List<Movie> filteredMovies = movieService.searchMovies(name, type, author, duration, status, releaseDate);

        model.addAttribute("movies", filteredMovies);
        model.addAttribute("totalMovies", filteredMovies.size());

        // Khi search, chúng ta tạm coi là 1 trang để không lỗi phân trang
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 1);

        model.addAttribute("searchName", name);
        model.addAttribute("searchType", type);
        model.addAttribute("searchAuthor", author);
        model.addAttribute("searchDuration", duration);
        model.addAttribute("searchStatus", status);
        model.addAttribute("searchReleaseDate", releaseDate);

        addMovieStatsToModel(model);

        return "admin/manager_movie";
    }

    @PostMapping("/movie/add")
    public String addMovie(@ModelAttribute("movieForm") Movie movie,
                           @RequestParam("file") MultipartFile file,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) { // Thêm cái này
        try {
            handleFileUpload(movie, file, request);
            movieService.saveMovie(movie);

            // Gửi thông báo thành công sang trang redirect
            redirectAttributes.addFlashAttribute("successMessage", "Thêm phim mới thành công!");
        } catch (Exception e) {
            // Gửi thông báo lỗi nếu có
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu phim: " + e.getMessage());
        }
        return "redirect:/admin/manager_movie";
    }

    @PostMapping("/movie/update")
    public String updateMovie(@ModelAttribute("movieForm") Movie movie,
                              @RequestParam("file") MultipartFile file,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) { // Thêm RedirectAttributes
        try {
            // Nếu người dùng không upload ảnh mới, lấy lại ảnh cũ từ DB
            if (file.isEmpty()) {
                Movie oldMovie = movieService.getMovieById(movie.getId());
                movie.setImgUrl(oldMovie.getImgUrl());
            } else {
                handleFileUpload(movie, file, request);
            }

            movieService.saveMovie(movie);

            // Gửi thông báo thành công
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin phim thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật: " + e.getMessage());
        }

        return "redirect:/admin/manager_movie";
    }

    @GetMapping("/movie/delete")
    public String deleteMovie(@RequestParam("id") Integer id,
                              RedirectAttributes redirectAttributes) {
        try {
            movieService.deleteMovie(id);
            // Gửi thông báo thành công
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phim khỏi hệ thống thành công!");
        } catch (Exception e) {
            // Gửi thông báo lỗi nếu phim đang có lịch chiếu hoặc lỗi khác
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa phim này: " + e.getMessage());
        }
        return "redirect:/admin/manager_movie";
    }

    // Hàm phụ xử lý File tránh lặp code
    private void handleFileUpload(Movie movie, MultipartFile file, HttpServletRequest request) {
        try {
            if (!file.isEmpty()) {
                String uploadDir = request.getServletContext().getRealPath("/resources/images/");
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                file.transferTo(new File(dir, fileName));
                movie.setImgUrl("resources/images/" + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm phụ load thống kê phim
    private void addMovieStatsToModel(Model model) {
        List<Movie> allMovies = movieService.getAllMovies();
        long countDangChieu = allMovies.stream().filter(m -> "Đang chiếu".equals(m.getStatus())).count();
        long countSapChieu = allMovies.stream().filter(m -> "Sắp chiếu".equals(m.getStatus())).count();
        long countDacBiet = allMovies.stream().filter(m -> "Suất chiếu đặc biệt".equals(m.getStatus())).count();

        model.addAttribute("countDangChieu", countDangChieu);
        model.addAttribute("countSapChieu", countSapChieu);
        model.addAttribute("countDacBiet", countDacBiet);
    }
}