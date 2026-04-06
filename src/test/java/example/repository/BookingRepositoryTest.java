package example.repository;

import example.entity.Booking;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Booking> query;

    @InjectMocks
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Test: Tìm danh sách đặt vé theo Account ID (Normal)")
    void testFindByAccountId() {
        // 1. Arrange: Chuẩn bị dữ liệu
        int accountId = 1;
        List<Booking> mockBookings = Arrays.asList(new Booking(), new Booking());
        
        // Giả lập Hibernate Session tạo Query HQL
        when(session.createQuery(anyString(), eq(Booking.class))).thenReturn(query);
        // Giả lập việc gán tham số :accId vào query
        when(query.setParameter("accId", accountId)).thenReturn(query);
        // Giả lập kết quả trả về từ database
        when(query.getResultList()).thenReturn(mockBookings);

        // 2. Act: Gọi hàm repository
        List<Booking> result = bookingRepository.findByAccountId(accountId);

        // 3. Assert: Kiểm tra hành vi
        assertEquals(2, result.size(), "Phải trả về đúng 2 đơn hàng của account");
        // Kiểm tra xem Repo có truyền đúng Account ID vào câu lệnh SQL/HQL không
        verify(query).setParameter("accId", accountId);
    }

    @Test
    @DisplayName("Test: Tìm chi tiết đặt vé theo ID (Normal)")
    void testFindById() {
        // 1. Arrange
        Long bookingId = 100L;
        Booking mockBooking = new Booking();
        mockBooking.setId(bookingId);

        // Giả lập query tìm theo ID
        when(session.createQuery(anyString(), eq(Booking.class))).thenReturn(query);
        when(query.setParameter("id", bookingId)).thenReturn(query);
        when(query.uniqueResult()).thenReturn(mockBooking);

        // 2. Act
        Booking result = bookingRepository.findById(bookingId);

        // 3. Assert
        assertNotNull(result);
        assertEquals(bookingId, result.getId(), "ID trả về phải khớp với ID yêu cầu");
        verify(query).uniqueResult();
    }

    @Test
    @DisplayName("Test: Lưu Booking mới (Normal)")
    void testSave_NewBooking() {
        // Arrange
        Booking newBooking = new Booking();
        // ID null = New

        // Act
        bookingRepository.save(newBooking);

        // Assert
        verify(session).persist(newBooking);
    }

    @Test
    @DisplayName("Test: Cập nhật Booking (Normal)")
    void testSave_UpdateBooking() {
        // Arrange
        Booking existingBooking = new Booking();
        existingBooking.setId(50L);

        // Act
        bookingRepository.save(existingBooking);

        // Assert
        verify(session).merge(existingBooking);
    }
}
