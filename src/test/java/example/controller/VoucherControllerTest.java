package example.controller;

import example.entity.Account;
import example.entity.Voucher;
import example.service.AccountService;
import example.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho VoucherController - Chức năng Khuyến mãi (Promotion).
 * Bao gồm: Xem danh sách voucher, Lưu voucher và Xem ví voucher cá nhân.
 */
@ExtendWith(MockitoExtension.class)
public class VoucherControllerTest {

    @InjectMocks
    private VoucherController voucherController;

    @Mock
    private VoucherService voucherService;

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
     * Test kịch bản hiển thị danh sách Voucher khuyến mãi ĐANG CÓ HIỆU LỰC.
     */
    @Test
    public void testShowAvailableVouchers_LoggedIn() {
        // --- 1. ARRANGE ---
        String email = "test@example.com";
        Account mockAccount = new Account();
        mockAccount.setAccountID(1);
        
        when(principal.getName()).thenReturn(email);
        when(accountService.findByEmail(email)).thenReturn(mockAccount);
        
        // Giả lập danh sách voucher đang active
        List<Voucher> activeVouchers = Arrays.asList(new Voucher(), new Voucher());
        when(voucherService.getActiveVouchers()).thenReturn(activeVouchers);
        
        // Giả lập danh sách ID voucher mà user đã lưu trước đó
        List<Integer> savedIds = Arrays.asList(10, 20);
        when(voucherService.getSavedVoucherIds(1)).thenReturn(savedIds);

        // --- 2. ACT ---
        String viewName = voucherController.showAvailableVouchers(model, principal);

        // --- 3. ASSERT ---
        assertEquals("user/vouchers", viewName);
        verify(model).addAttribute("account", mockAccount);
        verify(model).addAttribute("vouchers", activeVouchers);
        verify(model).addAttribute("savedVoucherIds", savedIds);
    }

    @Test
    public void testShowAvailableVouchers_NotLoggedIn() {
        // --- 2. ACT --- (principal null)
        String viewName = voucherController.showAvailableVouchers(model, null);

        // --- 3. ASSERT ---
        assertEquals("user/vouchers", viewName);
        verify(voucherService).getActiveVouchers();
        // Không được gọi đến accountService vì chưa login
        verify(accountService, never()).findByEmail(any());
    }

    /**
     * Test kịch bản khách hàng nhấn "LƯU VOUCHER" vào ví.
     */
    @Test
    public void testSaveToLibrary_Success() {
        // --- 1. ARRANGE ---
        int voucherId = 100;
        String email = "user@example.com";
        Account account = new Account();
        account.setAccountID(1);

        when(principal.getName()).thenReturn(email);
        when(accountService.findByEmail(email)).thenReturn(account);
        // Giả lập lưu thành công
        when(voucherService.collectVoucher(1, voucherId)).thenReturn(true);

        // --- 2. ACT ---
        String result = voucherController.saveToLibrary(voucherId, principal);

        // --- 3. ASSERT ---
        assertEquals("success", result);
        verify(voucherService).collectVoucher(1, voucherId);
    }

    @Test
    public void testSaveToLibrary_AlreadyExists() {
        // --- 1. ARRANGE ---
        when(principal.getName()).thenReturn("user@example.com");
        Account account = new Account();
        account.setAccountID(1);
        when(accountService.findByEmail(anyString())).thenReturn(account);
        // Giả lập voucher đã tồn tại trong ví
        when(voucherService.collectVoucher(1, 100)).thenReturn(false);

        // --- 2. ACT ---
        String result = voucherController.saveToLibrary(100, principal);

        // --- 3. ASSERT ---
        assertEquals("exists", result);
    }

    /**
     * Test kịch bản xem VÍ VOUCHER CÁ NHÂN (My Vouchers).
     * Chỉ hiển thị các voucher CÒN HẠN.
     */
    @Test
    public void testShowMyVouchers_Success() {
        // --- 1. ARRANGE ---
        int accountId = 1;
        Account account = new Account();
        account.setAccountID(accountId);

        when(principal.getName()).thenReturn("user@example.com");
        when(accountService.findByEmail(anyString())).thenReturn(account);

        // Tạo 1 voucher còn hạn và 1 voucher đã hết hạn
        Voucher validVoucher = new Voucher();
        validVoucher.setExpiryDate(LocalDateTime.now().plusDays(5));
        
        Voucher expiredVoucher = new Voucher();
        expiredVoucher.setExpiryDate(LocalDateTime.now().minusDays(1));

        List<Voucher> allMyVouchers = Arrays.asList(validVoucher, expiredVoucher);
        when(voucherService.getVouchersByAccountId(accountId)).thenReturn(allMyVouchers);

        // --- 2. ACT ---
        String viewName = voucherController.showMyVouchers(model, principal);

        // --- 3. ASSERT ---
        assertEquals("user/my_vouchers", viewName);
        
        // Kiểm tra xem danh sách truyền ra model đã được lọc (chỉ còn 1 voucher còn hạn)
        verify(model).addAttribute(eq("myVouchers"), argThat(list -> {
            List<Voucher> vouchers = (List<Voucher>) list;
            return vouchers.size() == 1 && vouchers.contains(validVoucher);
        }));
    }
}
