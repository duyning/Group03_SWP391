package example.service;

import example.entity.Movie;
import example.repository.MovieRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService; //

    // ==========================================
    // TEST CHO HÀM: getAllMovies()
    // ==========================================

    @Test
    @DisplayName("UTCID01: Lấy danh sách phim thành công (Normal)")
    void testGetAllMovies_Success() {
        // Arrange: Giả lập Repository trả về 1 danh sách có 2 bộ phim
        Movie m1 = new Movie(); m1.setMovieName("Con Kể Ba Nghe");
        Movie m2 = new Movie(); m2.setMovieName("Thiên Đường Máu");
        List<Movie> mockList = Arrays.asList(m1, m2);

        Mockito.when(movieRepository.findAll()).thenReturn(mockList);

        // Act: Gọi service
        List<Movie> result = movieService.getAllMovies();

        // Assert: Kiểm tra kết quả
        assertEquals(2, result.size());
        assertEquals("Con Kể Ba Nghe", result.get(0).getMovieName());
        assertEquals("Thiên Đường Máu", result.get(1).getMovieName());

        // Đảm bảo hàm findAll của Repo được gọi đúng 1 lần
        Mockito.verify(movieRepository, Mockito.times(1)).findAll();
    }

    @Test
    @DisplayName("UTCID02: Lấy danh sách phim khi Database trống (Normal)")
    void testGetAllMovies_EmptyList() {
        // Arrange: Giả lập không có phim nào trong DB
        Mockito.when(movieRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Movie> result = movieService.getAllMovies();

        // Assert
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
        Mockito.verify(movieRepository, Mockito.times(1)).findAll();
    }

    // ==========================================
    // TEST CHO HÀM: saveMovie()
    // ==========================================

    @Test
    @DisplayName("UTCID03: Lưu thông tin phim mới/cập nhật thành công (Normal)")
    void testSaveMovie_Success() {
        // Arrange
        Movie input = new Movie();
        input.setMovieName("Bố Già");

        // Vì saveOrUpdate là hàm void nên dùng doNothing()
        Mockito.doNothing().when(movieRepository).saveOrUpdate(any(Movie.class));

        // Act & Assert
        assertDoesNotThrow(() -> movieService.saveMovie(input));
        Mockito.verify(movieRepository, Mockito.times(1)).saveOrUpdate(input);
    }

    @Test
    @DisplayName("UTCID04: Báo lỗi khi lưu phim thất bại do lỗi Database (Abnormal)")
    void testSaveMovie_FailWhenDatabaseError() {
        // Arrange
        Movie input = new Movie();

        // Giả lập Repository văng lỗi (vd: đứt kết nối DB, sai cú pháp...)
        Mockito.doThrow(new RuntimeException("Lỗi kết nối CSDL!"))
                .when(movieRepository).saveOrUpdate(any(Movie.class));

        // Act & Assert: Phải bắt được đúng cái RuntimeException đó
        RuntimeException exception = assertThrows(RuntimeException.class, () -> movieService.saveMovie(input));
        assertEquals("Lỗi kết nối CSDL!", exception.getMessage());

        Mockito.verify(movieRepository, Mockito.times(1)).saveOrUpdate(input);
    }

    // ==========================================
    // TEST CHO HÀM: deleteMovie()
    // ==========================================

    @Test
    @DisplayName("UTCID05: Xóa phim thành công khi tìm thấy ID (Normal)")
    void testDeleteMovie_WhenMovieExists() {
        // Arrange: Phải giả lập tìm thấy phim trước
        Movie existingMovie = new Movie();
        existingMovie.setId(1);

        Mockito.when(movieRepository.getMovieById(1)).thenReturn(existingMovie);
        Mockito.doNothing().when(movieRepository).delete(existingMovie);

        // Act
        assertDoesNotThrow(() -> movieService.deleteMovie(1));

        // Assert: Đảm bảo Repo lấy phim ra 1 lần, và gọi hàm delete 1 lần
        Mockito.verify(movieRepository, Mockito.times(1)).getMovieById(1);
        Mockito.verify(movieRepository, Mockito.times(1)).delete(existingMovie);
    }

    @Test
    @DisplayName("UTCID06: Không xóa gì cả khi ID phim không tồn tại (Abnormal)")
    void testDeleteMovie_WhenMovieDoesNotExist() {
        // Arrange: Giả lập không tìm thấy phim (trả về null)
        Mockito.when(movieRepository.getMovieById(99)).thenReturn(null);

        // Act
        assertDoesNotThrow(() -> movieService.deleteMovie(99));

        // Assert:
        // 1. Phải gọi hàm getMovieById để tìm
        Mockito.verify(movieRepository, Mockito.times(1)).getMovieById(99);
        // 2. KHÔNG ĐƯỢC PHÉP gọi hàm delete (vì movie bị null)
        Mockito.verify(movieRepository, Mockito.never()).delete(any());
    }
}