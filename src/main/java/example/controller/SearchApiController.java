package example.controller;

import example.entity.Movie;
import example.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchApiController {

    @Autowired
    private MovieService movieService;

    @GetMapping("/movies")
    public List<Movie> searchMovies(
            @RequestParam(required = false) Integer cinemaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String timeRange) {
                
        // Convert "all" cinema case to null or just pass through
        if (cinemaId != null && cinemaId == 0) {
            cinemaId = null;
        }
        if ("all".equalsIgnoreCase(timeRange)) {
            timeRange = null;
        }

        return movieService.searchMoviesByTimeRange(cinemaId, date, timeRange);
    }
}
