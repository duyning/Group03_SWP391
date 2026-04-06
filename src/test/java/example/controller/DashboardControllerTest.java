package example.controller;

import example.entity.Booking;
import example.entity.Cinema;
import example.repository.BookingRepository;
import example.service.CinemaService;
import example.service.MovieService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử Unit Test cho DashboardController.
 * Sử dụng thư viện Mockito để cô lập Controller khỏi các Dependencies thật (Repository, Service).
 * Thử nghiệm này bao gồm chức năng xem thống kê và lịch sử đặt vé của Admin và Manager.
 */
@ExtendWith(MockitoExtension.class) // Kích hoạt sự hỗ trợ của Mockito trong JUnit 5
public class DashboardControllerTest {

    // @InjectMocks sẽ tự động tạo một phiên bản của DashboardController
    // và "tiêm" (inject) tất cả các mock (@Mock) ở dưới vào nó.
    @InjectMocks
    private DashboardController dashboardController;

    @Mock
    private MovieService movieService; // Mock cho truy vấn phim

    @Mock
    private BookingRepository bookingRepository; // Mock cho truy xuất dữ liệu doanh thu, vé

    @Mock
    private CinemaService cinemaService; // Mock lấy dữ liệu rạp

    @Mock
    private Model model; // Mock Spring UI Model để kiểm tra các thuộc tính được truyền ra View

    @Mock
    private HttpSession session; // Mock Http Session để kiểm soát dữ liệu session như messages lỗi

    /**
     * Phương thức setUp chạy trước MỖI test case (@Test).
     * Dùng để đảm bảo các mock được khởi tạo làm sạch trước mỗi kịch bản.
     */
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * TEST CASE 1: Truy cập trang Dashboard mặc định (KHÔNG GÓT BỘ LỌC TÌM KIẾM - Trang 1).
     * Kiểm tra xem thông tin doanh thu, số vé, danh sách rạp và danh sách booking phân trang 
     * có được tải lên và đưa vào Model chuẩn xác hay không.
     */
    @Test
    public void testShowDashboard_NoFilter() {
        // --- 1. ARRANGE (Chuẩn bị dữ liệu và mock hành vi) ---
        int page = 1;

        // Giả lập không có lỗi đăng nhập trong session
        when(session.getAttribute("accessDeniedMessage")).thenReturn(null);

        // Khi Controller gọi hàm tính doanh thu, trả về cố định 1,000,000. 
        // Dùng any() cho ngày tháng vì Dashboard sẽ tính toán cho Hôm nay + 7 ngày qua + 12 tháng.
        when(bookingRepository.getTotalRevenueByDate(any(), any())).thenReturn(1000000.0);
        // Trả về số tổng số lượng vé đã bán là 50
        when(bookingRepository.getTotalTicketsSold()).thenReturn(50L);

        // Giả lập hệ thống có 2 Rạp chiếu phim được trả về từ service
        List<Cinema> cinemas = Arrays.asList(new Cinema(), new Cinema());
        when(cinemaService.getAllCinemas()).thenReturn(cinemas);

        // Giả lập trong database có tổng cộng 25 Booking
        long totalItems = 25L;
        when(bookingRepository.countAll()).thenReturn(totalItems);

        // Giả lập kết quả trả về khi phân trang cho trang 1 (kích thước page 10) là 2 đối tượng Booking
        List<Booking> mockBookings = Arrays.asList(new Booking(), new Booking());
        when(bookingRepository.findAllPaged(page, 10)).thenReturn(mockBookings);

        // --- 2. ACT (Thực thi hành động) ---
        // Gọi thẳng hàm trong Controller như khi client tạo ra Request Http GET
        String viewName = dashboardController.showDashboard(
                null, null, null, null, page, model, session // null cho các tham số search/filter
        );

        // --- 3. ASSERT (Kiểm chứng kết quả xuất ra) ---
        // Phải trả về đúng tên Thymeleaf view template của Dashboard
        assertEquals("admin/dashboard", viewName);

        // Kiểm tra xem model đã được add các attribute thống kê tổng quan chưa
        verify(model).addAttribute(eq("todayRevenue"), anyDouble());
        verify(model).addAttribute("totalTickets", 50L);
        verify(model).addAttribute(eq("chartLabels"), anyList()); // Check dữ liệu đồ thị ngày
        verify(model).addAttribute(eq("chartData"), anyList());
        verify(model).addAttribute(eq("monthLabels"), anyList()); // Check dữ liệu đồ thị tháng
        verify(model).addAttribute(eq("monthData"), anyList());

        // Kiểm tra dropdown lọc rạp phim đã được chèn vào model chưa
        verify(model).addAttribute("cinemas", cinemas);

        // Kiểm tra thuật toán phân trang đã tính đúng Total Page chưa 
        // (Vd: 25 items / 10 pageSize = lên tròn thành 3 trang)
        verify(model).addAttribute("recentBookings", mockBookings);
        verify(model).addAttribute("currentPage", 1);
        verify(model).addAttribute("totalPages", 3);
        verify(model).addAttribute("totalItems", 25L);

        // Mấu chốt: Vì đây là test KHÔNG FILTER, nên khẳng định là "countSearchBookings" và "searchBookingsPaged"
        // chưa từng được gọi trong BookingRepository (sử dụng chữ never()).
        verify(bookingRepository, never()).countSearchBookings(any(), any(), any(), any());
        verify(bookingRepository, never()).searchBookingsPaged(any(), any(), any(), any(), anyInt(), anyInt());
    }

    /**
     * TEST CASE 2: Truy cập trang Dashboard kèm THEO CÁC BỘ LỌC TÌM KIẾM
     * Người dùng (Admin/Manager) nhập từ khóa search tìm tên khách/"mã NV", chọn rạp id=1, giá khoảng 50k-200k.
     */
    @Test
    public void testShowDashboard_WithFilter() {
        // --- 1. ARRANGE (Chuẩn bị dữ liệu) ---
        String search = "NV01";
        Integer cinemaId = 1;
        Double minPrice = 50000.0;
        Double maxPrice = 200000.0;
        int page = 2; // Yêu cầu xem kết quả trang số 2.

        when(session.getAttribute("accessDeniedMessage")).thenReturn(null);
        when(bookingRepository.getTotalRevenueByDate(any(), any())).thenReturn(500000.0);
        when(bookingRepository.getTotalTicketsSold()).thenReturn(20L);
        when(cinemaService.getAllCinemas()).thenReturn(Arrays.asList(new Cinema()));

        // Với bộ lọc search này, giả định ở csdl chỉ có 15 kết quả phù hợp
        long totalSearchItems = 15L;
        when(bookingRepository.countSearchBookings(search, cinemaId, minPrice, maxPrice)).thenReturn(totalSearchItems);

        // Query cho trang số 2 sẽ list ra list booking mock này
        List<Booking> mockBookings = Arrays.asList(new Booking());
        when(bookingRepository.searchBookingsPaged(search, cinemaId, minPrice, maxPrice, page, 10))
                .thenReturn(mockBookings);

        // --- 2. ACT ---
        String viewName = dashboardController.showDashboard(
                search, cinemaId, minPrice, maxPrice, page, model, session
        );

        // --- 3. ASSERT ---
        assertEquals("admin/dashboard", viewName);

        // Đảm bảo những dữ liệu tìm kiếm người dùng vừa tick đã được lưu lại model để Thymeleaf hiển thị lên lại form search
        verify(model).addAttribute("recentBookings", mockBookings);
        verify(model).addAttribute("searchKeyword", search);
        verify(model).addAttribute("selectedCinemaId", cinemaId);
        verify(model).addAttribute("minPrice", minPrice);
        verify(model).addAttribute("maxPrice", maxPrice);

        // Thuật toán: 15 items / PageSize 10 = trả ra tổng số 2 trang. Trang hiện tại là 2.
        verify(model).addAttribute("currentPage", 2);
        verify(model).addAttribute("totalPages", 2);
        verify(model).addAttribute("totalItems", 15L);

        // Vì CÓ FILTER, khẳng định "countAll" cơ bản và "findAllPaged" cơ bản KHÔNG ĐƯỢC CHẠY.
        verify(bookingRepository, never()).countAll();
        verify(bookingRepository, never()).findAllPaged(anyInt(), anyInt());
    }

    /**
     * TEST CASE 3: Lỗi tham số URL - Người dùng nhập quá số lượng trang hợp lệ
     * VD: Tổng số items trong DB chỉ là 5, page size là 10. => Nghĩa là cao nhất chỉ có trang 1.
     * Người dùng tự gõ URL "?page=5" -> Controller phải xử lý được, tự động lùi về lại trang số 1.
     */
    @Test
    public void testShowDashboard_PageExceedsTotalPages() {
        // --- 1. ARRANGE ---
        int requestedPage = 5; 
        long totalItems = 5L; // Ít hơn 1 pageSize (10)
        
        when(bookingRepository.getTotalRevenueByDate(any(), any())).thenReturn(0.0);
        when(bookingRepository.getTotalTicketsSold()).thenReturn(0L);
        when(cinemaService.getAllCinemas()).thenReturn(Arrays.asList());
        
        when(bookingRepository.countAll()).thenReturn(totalItems);
        // Do DashboardController hiện tại đang tải dữ liệu 'bookingRepository.findAllPaged(page, PAGE_SIZE)'
        // *TRƯỚC KHI* thực hiện gán 'page = totalPages', nên nó vẫn gọi xuống CSDL bằng `requestedPage = 5`.
        // (Lưu ý: Đây có thể là một lỗi thiết kế nhỏ trong vòng đời Controller cần được cấu trúc lại).
        when(bookingRepository.findAllPaged(requestedPage, 10)).thenReturn(Arrays.asList());

        // --- 2. ACT ---
        dashboardController.showDashboard(
                null, null, null, null, requestedPage, model, session
        );

        // --- 3. ASSERT ---
        // Biến currentPage bị force lui lại số 1 để tránh lỗi hiển thị trang rỗng ở View
        verify(model).addAttribute("currentPage", 1); 
        verify(model).addAttribute("totalPages", 1);
        verify(model).addAttribute("totalItems", 5L);
        
        // Assert chắn chắn DB đã bị query bằng số trang cũ (5) do logic Controller hiện tại tính chặn Page SAU KHI query
        verify(bookingRepository).findAllPaged(requestedPage, 10); 
    }

    /**
     * TEST CASE 4: Xử lý hiển thị thông báo "TỪ CHỐI TRUY CẬP" (Access Denied) từ hệ thống phân quyền
     * Khi 1 Role nhỏ cố truy cập chức năng Role lớn nhưng bị cản và quăng quay lại dashboard,
     * Security đẩy thông báo lỗi vào Session, Controller phải nạp nó ra dạng Alert, rồi xóa Session rác đó đi.
     */
    @Test
    public void testShowDashboard_WithAccessDeniedMessage() {
        // --- 1. ARRANGE ---
        String deniedMsg = "You don't have permission to perform this action."; // Lỗi phân quyền
        
        // Móc session trả về có thông báo trong đó
        when(session.getAttribute("accessDeniedMessage")).thenReturn(deniedMsg);
        
        // Cần thiết lập vì controller vẫn load tiếp bảng điều khiển bên dưới alert
        when(bookingRepository.getTotalRevenueByDate(any(), any())).thenReturn(100.0);
        when(bookingRepository.getTotalTicketsSold()).thenReturn(5L);
        when(bookingRepository.countAll()).thenReturn(0L);

        // --- 2. ACT ---
        dashboardController.showDashboard(
                null, null, null, null, 1, model, session
        );

        // --- 3. ASSERT ---
        // Kiểm tra controller đã nạp thông báo cấm vô model chờ popup html hiển thị
        verify(model).addAttribute("accessDeniedMessage", deniedMsg);
        
        // Đặc biệt: đảm bảo Controller phải thu dọn và gỡ bỏ rác message đã hiển thị xong trong Session 
        // để người dùng F5 không bị popup lỗi chê mãi.
        verify(session).removeAttribute("accessDeniedMessage");
    }
}
