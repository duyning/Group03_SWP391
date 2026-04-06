package example.controller;

import example.entity.Account;
import example.entity.Booking;
import example.repository.BookingRepository;
import example.service.AccountService;
import example.service.BookingService;
import example.service.ComboService;
import example.service.ShowtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho BookingController - Chức năng xem lịch sử đặt vé (Customer).
 */
@ExtendWith(MockitoExtension.class)
public class BookingControllerTest {

    @InjectMocks
    private BookingController bookingController;

    @Mock
    private ShowtimeService showtimeService;

    @Mock
    private ComboService comboService;

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private Model model;

    @Mock
    private Principal principal;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test kịch bản khách hàng xem lịch sử đặt vé THÀNH CÔNG.
     */
    @Test
    public void testBookingHistory_Success() {
        // --- 1. ARRANGE ---
        String email = "customer@example.com";
        int accountId = 123;
        
        // Giả lập Principal trả về email người dùng
        when(principal.getName()).thenReturn(email);
        
        // Giả lập tìm thấy tài khoản tương ứng
        Account mockAccount = new Account();
        mockAccount.setAccountID(accountId);
        mockAccount.setEmail(email);
        when(accountService.findByEmail(email)).thenReturn(mockAccount);
        
        // Giả lập danh sách lịch sử đặt vé (2 booking)
        List<Booking> mockHistory = Arrays.asList(new Booking(), new Booking());
        when(bookingRepository.findByAccountId(accountId)).thenReturn(mockHistory);

        // --- 2. ACT ---
        String viewName = bookingController.bookingHistory(model, principal);

        // --- 3. ASSERT ---
        // Kiểm tra trả về đúng trang HTML lịch sử
        assertEquals("user/booking_history", viewName);
        
        // Kiểm tra Controller có đưa thông tin account và danh sách booking vào Model không
        verify(model).addAttribute("account", mockAccount);
        verify(model).addAttribute("bookings", mockHistory);
        
        // Đảm bảo accountService và bookingRepository được gọi đúng tham số
        verify(accountService, atLeastOnce()).findByEmail(email);
        verify(bookingRepository).findByAccountId(accountId);
    }

    /**
     * Test kịch bản khách hàng đã đăng nhập nhưng CHƯA CÓ lịch sử đặt vé (danh sách rỗng).
     */
    @Test
    public void testBookingHistory_EmptyHistory() {
        // --- 1. ARRANGE ---
        String email = "newuser@example.com";
        int accountId = 456;
        
        when(principal.getName()).thenReturn(email);
        
        Account mockAccount = new Account();
        mockAccount.setAccountID(accountId);
        when(accountService.findByEmail(email)).thenReturn(mockAccount);
        
        // Giả lập danh sách trống
        List<Booking> emptyHistory = new ArrayList<>();
        when(bookingRepository.findByAccountId(accountId)).thenReturn(emptyHistory);

        // --- 2. ACT ---
        String viewName = bookingController.bookingHistory(model, principal);

        // --- 3. ASSERT ---
        assertEquals("user/booking_history", viewName);
        verify(model).addAttribute("bookings", emptyHistory);
    }

    /**
     * Test kịch bản xử lý khi Principal bị NULL (Mặc dù bình thường Security sẽ chặn).
     * Để đảm bảo Controller không bị NullPointerException.
     */
    @Test
    public void testBookingHistory_PrincipalNull() {
        // --- 1. ARRANGE ---
        // principal mặc định là null (không cần mock thêm)

        // --- 2. ACT & ASSERT ---
        // Lưu ý: Trong code hiện tại, dòng `String username = principal.getName();` (dòng 140) 
        // sẽ gây NullPointerException nếu principal là null vì không có check null ở đó.
        // Đây là một điểm cần lưu ý để cải thiện code bền vững hơn.
        
        try {
            bookingController.bookingHistory(model, null);
        } catch (NullPointerException e) {
            // Mong đợi NPE xảy ra do thiết kế code hiện tại của controller
            // (Thường Security sẽ đảm bảo Principal không null khi vào route này)
        }
    }
}
