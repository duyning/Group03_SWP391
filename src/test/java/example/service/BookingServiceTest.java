package example.service;

import example.entity.Combo;
import example.entity.ComboBookingDTO;
import example.entity.Seat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử nâng cao cho chức năng Booking (Booking Service).
 * Bao gồm các trường hợp tính toán phức tạp, xử lý dữ liệu lỗi và các tình huống thực tế.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private ShowtimeService showtimeService;

    @Mock
    private SeatService seatService;

    @Mock
    private ComboService comboService;

    @InjectMocks
    private BookingService bookingService;

    private List<Map<String, Object>> mockSeatMap;

    @BeforeEach
    void setUp() {
        mockSeatMap = new ArrayList<>();
        
        // Giả lập 2 ghế trong Showtime
        Map<String, Object> seat1 = new HashMap<>();
        seat1.put("id", 101);
        seat1.put("code", "A1");
        seat1.put("price", 50000.0);
        
        Map<String, Object> seat2 = new HashMap<>();
        seat2.put("id", 102);
        seat2.put("code", "A2");
        seat2.put("price", 60000.0);
        
        mockSeatMap.add(seat1);
        mockSeatMap.add(seat2);
    }

    @Test
    @DisplayName("UNTCID1: Tính tổng tiền chỉ có vé ghế (Normal)")
    void testCalculateTotalAmount_OnlySeats() {
        // 1. Chuẩn bị dữ liệu (Arrange)
        int showtimeId = 1;
        String seatIds = "101,102"; // Hai ghế A1 và A2
        String comboDataJson = "[]"; // Không mua combo
        List<ComboBookingDTO> outCombos = new ArrayList<>();
        List<Seat> outSeats = new ArrayList<>();

        // Giả lập service trả về sơ đồ ghế của suất chiều (A1=50k, A2=60k)
        when(showtimeService.getSeatMap(showtimeId)).thenReturn(mockSeatMap);

        // 2. Thực thi hàm cần test (Act)
        Double total = bookingService.calculateTotalAmount(seatIds, comboDataJson, showtimeId, outCombos, outSeats);

        // 3. Kiểm tra kết quả (Assert)
        // Ghế A1 (50k) + Ghế A2 (60k) = 110.000đ
        assertEquals(110000.0, total, "Tổng tiền phải là 110.000đ");
        assertEquals(2, outSeats.size(), "Danh sách ghế trả về phải có 2 ghế");
        assertEquals("A1", outSeats.get(0).getSeatNumber());
        assertEquals("A2", outSeats.get(1).getSeatNumber());
        assertTrue(outCombos.isEmpty(), "Danh sách combo phải trống");
    }

    @Test
    @DisplayName("UNTCID2: Tính tổng tiền có cả vé và combo (Normal)")
    void testCalculateTotalAmount_SeatsAndCombos() {
        // 1. Chuẩn bị dữ liệu (Arrange)
        int showtimeId = 1;
        String seatIds = "101"; // Chọn 1 ghế A1 (50.000đ)
        // JSON giả lập: Chọn Combo mã 1 với số lượng là 2
        String comboDataJson = "[{\"comboId\": 1, \"quantity\": 2}]";
        List<ComboBookingDTO> outCombos = new ArrayList<>();
        List<Seat> outSeats = new ArrayList<>();
        // Giả lập thông tin Combo từ database
        Combo mockCombo = new Combo();
        mockCombo.setId(1);
        mockCombo.setComboName("Combo Bắp Nước");
        mockCombo.setPrice(75000.0);
        mockCombo.setActive(true);

        // Định nghĩa hành vi cho Mock
        when(showtimeService.getSeatMap(showtimeId)).thenReturn(mockSeatMap);
        when(comboService.findById(1)).thenReturn(mockCombo);

        // 2. Thực thi (Act)
        Double total = bookingService.calculateTotalAmount(seatIds, comboDataJson, showtimeId, outCombos, outSeats);
        // 3. Kiểm tra (Assert)
        // Ghế A1 (50k) + 2 x Combo (2 * 75k = 150k) = 200.000đ
        assertEquals(200000.0, total, "Tổng tiền (ghế + 2 combo) phải là 200.000đ");
        assertEquals(1, outSeats.size());
        assertEquals(1, outCombos.size());
        assertEquals("Combo Bắp Nước", outCombos.get(0).getComboName());
    }

    @Test
    @DisplayName("UNTCID3: Bỏ qua combo nếu bị ẩn (Inactive)")
    void testCalculateTotalAmount_IgnoreInactiveCombos() {
        // 1. Arrange
        int showtimeId = 1;
        String seatIds = ""; // Không chọn ghế
        String comboDataJson = "[{\"comboId\": 9, \"quantity\": 1}]";
        List<ComboBookingDTO> outCombos = new ArrayList<>();
        List<Seat> outSeats = new ArrayList<>();

        Combo inactiveCombo = new Combo();
        inactiveCombo.setId(9);
        inactiveCombo.setActive(false); // Combo đang bị MANAGER tắt (Active = false)

        when(showtimeService.getSeatMap(showtimeId)).thenReturn(mockSeatMap);
        when(comboService.findById(9)).thenReturn(inactiveCombo);

        // 2. Act
        Double total = bookingService.calculateTotalAmount(seatIds, comboDataJson, showtimeId, outCombos, outSeats);

        // 3. Assert
        // Vì combo không active nên tổng tiền là 0 và danh sách đầu ra trống
        assertEquals(0.0, total, "Tổng tiền phải là 0 vì combo bị ẩn");
        assertTrue(outCombos.isEmpty());
    }

    @Test
    @DisplayName("UNTCID4: Tính tổng tiền khi chọn nhiều loại Combo khác nhau")
    void testCalculateTotalAmount_MultipleCombos() {
        // 1. Arrange
        int showtimeId = 1;
        String seatIds = "";
        String comboDataJson = "[{\"comboId\": 1, \"quantity\": 1}, {\"comboId\": 2, \"quantity\": 2}]";
        List<ComboBookingDTO> outCombos = new ArrayList<>();
        List<Seat> outSeats = new ArrayList<>();

        Combo c1 = new Combo(); c1.setId(1); c1.setPrice(75000.0); c1.setActive(true);
        Combo c2 = new Combo(); c2.setId(2); c2.setPrice(100000.0); c2.setActive(true);

        when(showtimeService.getSeatMap(showtimeId)).thenReturn(mockSeatMap);
        when(comboService.findById(1)).thenReturn(c1);
        when(comboService.findById(2)).thenReturn(c2);

        // 2. Act
        Double total = bookingService.calculateTotalAmount(seatIds, comboDataJson, showtimeId, outCombos, outSeats);

        // 3. Assert
        // (1 * 75k) + (2 * 100k) = 275.000đ
        assertEquals(275000.0, total, "Tổng tiền 2 loại combo phải là 275.000đ");
        assertEquals(2, outCombos.size());
    }

    @Test
    @DisplayName("UNTCID5: Xử lý dữ liệu JSON Combo bị lỗi (Malformed JSON)")
    void testCalculateTotalAmount_MalformedJson() {
        // 1. Arrange
        int showtimeId = 1;
        String seatIds = "101"; // Ghế A1 (50k)
        String malformedJson = "[{comboId: 1, quantity: 1]"; // Thiếu dấu ngoặc nhọn kết thúc
        List<ComboBookingDTO> outCombos = new ArrayList<>();
        List<Seat> outSeats = new ArrayList<>();

        when(showtimeService.getSeatMap(showtimeId)).thenReturn(mockSeatMap);

        // 2. Act
        Double total = bookingService.calculateTotalAmount(seatIds, malformedJson, showtimeId, outCombos, outSeats);

        // 3. Assert
        // Khi JSON lỗi, hệ thống nên bỏ qua phần combo và chỉ tính tiền ghế
        assertEquals(50000.0, total, "Khi JSON lỗi, chỉ tính tiền ghế (50k)");
        assertTrue(outCombos.isEmpty());
    }

    @Test
    @DisplayName("UNTCID6: Combo có số lượng bằng 0 hoặc âm")
    void testCalculateTotalAmount_ZeroQuantityCombo() {
        // 1. Arrange
        int showtimeId = 1;
        String seatIds = "";
        String comboDataJson = "[{\"comboId\": 1, \"quantity\": 0}]";
        List<ComboBookingDTO> outCombos = new ArrayList<>();
        List<Seat> outSeats = new ArrayList<>();

        Combo c1 = new Combo(); c1.setId(1); c1.setPrice(75000.0); c1.setActive(true);

        when(showtimeService.getSeatMap(showtimeId)).thenReturn(mockSeatMap);
        when(comboService.findById(1)).thenReturn(c1);

        // 2. Act
        Double total = bookingService.calculateTotalAmount(seatIds, comboDataJson, showtimeId, outCombos, outSeats);

        // 3. Assert
        assertEquals(0.0, total, "Tổng tiền phải là 0 khi quantity = 0");
        // Theo code hiện tại, nó vẫn add vào list cho dù quantity = 0
        assertFalse(outCombos.isEmpty(), "Code hiện tại vẫn add vào list ngay cả khi quantity = 0");
    }

    @Test
    @DisplayName("UNTCID7: Xử lý khi dữ liệu vào trống hoặc null (Robustness)")
    void testCalculateTotalAmount_EmptyInputs() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            bookingService.calculateTotalAmount
                    (null, null, 1, new ArrayList<>(), new ArrayList<>());
            bookingService.calculateTotalAmount
                    ("", "", 1, new ArrayList<>(), new ArrayList<>());
        });
        
        Double total = bookingService.calculateTotalAmount
                ("", "[]", 1, new ArrayList<>(), new ArrayList<>());
        assertEquals(0.0, total);
    }
}
