package example.service;

import example.entity.Account;
import example.entity.Voucher;
import example.repository.VoucherRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử toàn diện cho chức năng Voucher (Voucher Service).
 * Bao gồm kiểm tra tính hợp lệ, quy tắc thu thập và quy trình sử dụng.
 */
@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session hibernateSession;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private Voucher mockVoucher;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        // Chuẩn bị Voucher mẫu
        mockVoucher = new Voucher();
        mockVoucher.setId(1);
        mockVoucher.setCode("SALE20");
        mockVoucher.setDiscountPercent(20);
        mockVoucher.setActive(true);
        mockVoucher.setExpiryDate(LocalDateTime.now().plusDays(10)); // Còn hạn
        mockVoucher.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Chuẩn bị Account mẫu
        mockAccount = new Account();
        mockAccount.setAccountID(100);
        mockAccount.setMyVouchers(new HashSet<>());
        mockAccount.setUsedVouchers(new HashSet<>());
    }

    /**
     * Test chức năng kiểm tra Voucher hợp lệ (Active + Còn hạn).
     */
    @Test
    @DisplayName("UNTCID1: Kiểm tra Voucher hợp lệ (Success)")
    void testIsValid_Success() {
        when(voucherRepository.findByCode("SALE20")).thenReturn(mockVoucher);
        assertTrue(voucherService.isValid("SALE20"), "Voucher SALE20 phải hợp lệ");
    }

    /**
     * Test Voucher bị hết hạn.
     */
    @Test
    @DisplayName("UNTCID2: Voucher hết hạn phải trả về không hợp lệ")
    void testIsValid_Expired() {
        mockVoucher.setExpiryDate(LocalDateTime.now().minusHours(1)); // Đã hết hạn 1 tiếng trước
        when(voucherRepository.findByCode("SALE20")).thenReturn(mockVoucher);
        assertFalse(voucherService.isValid("SALE20"), "Voucher hết hạn không được hợp lệ");
    }

    /**
     * Test Voucher bị vô hiệu hóa (Active = false).
     */
    @Test
    @DisplayName("UNTCID3: Voucher bị tắt (Inactive) không được hợp lệ")
    void testIsValid_Inactive() {
        mockVoucher.setActive(false);
        when(voucherRepository.findByCode("SALE20")).thenReturn(mockVoucher);
        assertFalse(voucherService.isValid("SALE20"), "Voucher Inactive không được hợp lệ");
    }

    /**
     * Test chức năng tạo Voucher mới - Tự động sinh ngày tạo.
     */
    @Test
    @DisplayName("UNTCID4: Tự động gán ngày tạo khi thêm Voucher mới")
    void testSaveVoucher_New() {
        Voucher newVoucher = new Voucher();
        newVoucher.setId(0);
        newVoucher.setCode("NEWCODE");

        when(voucherRepository.findByCode("NEWCODE")).thenReturn(null);
        when(voucherRepository.saveOrUpdate(any())).thenReturn(newVoucher);

        voucherService.saveVoucher(newVoucher);

        assertNotNull(newVoucher.getCreatedAt(), "Ngày tạo phải được tự động thiết lập");
        verify(voucherRepository).saveOrUpdate(newVoucher);
    }

    /**
     * Test chặn tạo Voucher nếu trùng mã.
     */
    @Test
    @DisplayName("UNTCID5: Không cho phép tạo Voucher trùng mã code")
    void testSaveVoucher_DuplicateCode() {
        Voucher duplicate = new Voucher();
        duplicate.setId(0);
        duplicate.setCode("SALE20");

        when(voucherRepository.findByCode("SALE20")).thenReturn(mockVoucher);

        assertThrows(RuntimeException.class, () -> voucherService.saveVoucher(duplicate),
                "Phải ném ra lỗi khi trùng mã Voucher");
    }

    /**
     * Test cập nhật Voucher nhưng giữ nguyên ngày tạo gốc.
     */
    @Test
    @DisplayName("UNTCID6: Cập nhật Voucher phải giữ nguyên ngày tạo cũ")
    void testSaveVoucher_UpdatePreserveDate() {
        LocalDateTime originalDate = mockVoucher.getCreatedAt();
        Voucher updateData = new Voucher();
        updateData.setId(1);
        updateData.setCode("SALE20_MODIFIED");
        updateData.setExpiryDate(mockVoucher.getExpiryDate()); // Tránh NPE khi so sánh ngày

        when(voucherRepository.findById(1)).thenReturn(mockVoucher);
        when(voucherRepository.saveOrUpdate(any())).thenReturn(updateData);

        voucherService.saveVoucher(updateData);

        assertEquals(originalDate, updateData.getCreatedAt(), "Ngày tạo không được thay đổi khi cập nhật");
    }

    /**
     * Test thu thập Voucher công khai thành công.
     */
    @Test
    @DisplayName("UNTCID7: Người dùng thu thập Voucher công khai (Collect Success)")
    void testCollectVoucher_Success() {
        when(sessionFactory.getCurrentSession()).thenReturn(hibernateSession);
        when(hibernateSession.get(Account.class, 100)).thenReturn(mockAccount);
        when(voucherRepository.findById(1)).thenReturn(mockVoucher);

        boolean result = voucherService.collectVoucher(100, 1);

        assertTrue(result, "Thu thập voucher phải thành công");
        assertTrue(mockAccount.getMyVouchers().contains(mockVoucher), "Voucher phải nằm trong ví người dùng");
    }

    /**
     * Test chặn thu thập Voucher cá nhân (VIP reward).
     */
    @Test
    @DisplayName("UNTCID8: Voucher cá nhân (VIP) không được thu thập công khai")
    void testCollectVoucher_PersonalBlocked() {
        mockVoucher.setPersonal(true); // Voucher dành riêng cho cá nhân
        when(sessionFactory.getCurrentSession()).thenReturn(hibernateSession);
        when(hibernateSession.get(Account.class, 100)).thenReturn(mockAccount);
        when(voucherRepository.findById(1)).thenReturn(mockVoucher);

        boolean result = voucherService.collectVoucher(100, 1);

        assertFalse(result, "Voucher cá nhân không được cho phép collect tự do");
    }

    /**
     * Test quy trình sử dụng Voucher (Chuyển trạng thái).
     */
    @Test
    @DisplayName("UNTCID9: Đánh dấu Voucher đã sử dụng (Mark as Used)")
    void testMarkVoucherAsUsed() {
        // Cho voucher vào ví trước
        mockAccount.getMyVouchers().add(mockVoucher);
        
        when(sessionFactory.getCurrentSession()).thenReturn(hibernateSession);
        when(hibernateSession.get(Account.class, 100)).thenReturn(mockAccount);
        when(voucherRepository.findByCode("SALE20")).thenReturn(mockVoucher);

        voucherService.markVoucherAsUsed(100, "SALE20");

        assertFalse(mockAccount.getMyVouchers().contains(mockVoucher), "Voucher phải được gỡ khỏi ví");
        assertTrue(mockAccount.getUsedVouchers().contains(mockVoucher), "Voucher phải nằm trong danh sách đã dùng");
        verify(hibernateSession).merge(mockAccount);
    }

    /**
     * Test xóa Voucher khỏi hệ thống (Cleanup liên kết).
     */
    @Test
    @DisplayName("UNTCID10: Xóa Voucher phải dọn dẹp liên kết với người dùng")
    void testDeleteVoucher_Cleanup() {
        when(voucherRepository.findById(1)).thenReturn(mockVoucher);
        when(sessionFactory.getCurrentSession()).thenReturn(hibernateSession);
        
        // Mock query dọn dẹp (simplified)
        org.hibernate.query.Query<Account> mockQuery = mock(org.hibernate.query.Query.class);
        when(hibernateSession.createQuery(anyString(), eq(Account.class))).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(new ArrayList<>());

        voucherService.deleteVoucher(1);

        verify(voucherRepository).delete(1);
    }
}
