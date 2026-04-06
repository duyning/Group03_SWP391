package example.service;

import example.entity.Voucher;

import java.util.List;

public interface VoucherService {
    Voucher saveVoucher(Voucher voucher);

    List<Voucher> findAllPaged(int page, int size);
    long countTotal();
    List<Voucher> findAll();

    Voucher findById(int id);

    Voucher findByCode(String code);

    void deleteVoucher(int id);

    // Tìm các voucher còn hạn và đang kích hoạt
    List<Voucher> getActiveVouchers();

    // Kiểm tra mã voucher có hợp lệ để sử dụng hay không
    boolean isValid(String code);

    boolean collectVoucher(int accountId, int voucherId);

    List<Integer> getSavedVoucherIds(int accountId);

    // 3. Lấy danh sách Voucher thực thể của User (để hiển thị trang My Vouchers)
    List<Voucher> getVouchersByAccountId(int accountId);

    void markVoucherAsUsed(int accountId, String voucherCode);
}
