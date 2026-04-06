package example.repository;

import example.entity.Voucher;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử cho VoucherRepository.
 * Tập trung xác nhận các câu lệnh HQL truy vấn mã voucher và lọc trạng thái.
 */
@ExtendWith(MockitoExtension.class)
class VoucherRepositoryTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Voucher> query;

    @InjectMocks
    private VoucherRepository voucherRepository;

    @BeforeEach
    void setUp() {
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    @DisplayName("Tìm Voucher theo mã code (Không phân biệt hoa thường)")
    void testFindByCode() {
        // 1. Arrange
        String code = "sale20";
        Voucher mockVoucher = new Voucher();
        mockVoucher.setCode("SALE20");

        when(session.createQuery(anyString(), eq(Voucher.class))).thenReturn(query);
        when(query.setParameter("code", code)).thenReturn(query);
        when(query.uniqueResult()).thenReturn(mockVoucher);

        // 2. Act
        Voucher result = voucherRepository.findByCode(code);

        // 3. Assert
        assertNotNull(result);
        assertEquals("SALE20", result.getCode());
        verify(session).createQuery(contains("UPPER(v.code) = UPPER(:code)"), eq(Voucher.class));
    }

    @Test
    @DisplayName("Tìm danh sách Voucher còn hạn và đang kích hoạt")
    void testFindActiveVouchers() {
        // 1. Arrange
        List<Voucher> activeList = Collections.singletonList(new Voucher());
        when(session.createQuery(anyString(), eq(Voucher.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(activeList);

        // 2. Act
        List<Voucher> result = voucherRepository.findActiveVouchers();

        // 3. Assert
        assertEquals(1, result.size());
        verify(session).createQuery(contains("v.active = true AND v.expiryDate > CURRENT_TIMESTAMP"), eq(Voucher.class));
    }

    @Test
    @DisplayName("Đếm tổng số lượng Voucher")
    void testCountAll() {
        // 1. Arrange
        Query<Long> countQuery = mock(Query.class);
        when(session.createQuery(contains("SELECT count(v)"), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.uniqueResult()).thenReturn(10L);

        // 2. Act
        long count = voucherRepository.countAll();

        // 3. Assert
        assertEquals(10L, count);
    }
}
