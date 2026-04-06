package example.service;

import example.entity.Voucher;
import example.repository.VoucherRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoucherServiceImpl implements VoucherService {
    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    public Voucher saveVoucher(Voucher voucher) {
        if (voucher.getId() == 0) {
            // --- TRƯỜNG HỢP TẠO MỚI ---
            voucher.setCreatedAt(LocalDateTime.now());

            // Kiểm tra trùng mã khi tạo mới
            if (voucherRepository.findByCode(voucher.getCode()) != null) {
                throw new RuntimeException("Mã Voucher này đã tồn tại trên hệ thống!");
            }
        } else {
            // --- TRƯỜNG HỢP CẬP NHẬT (SỬA) ---
            // BƯỚC 1: Tìm bản ghi cũ đang tồn tại trong Database
            Voucher existingVoucher = voucherRepository.findById(voucher.getId());

            if (existingVoucher != null) {
                // BƯỚC 2: Lấy ngày tạo từ bản ghi cũ gán vào đối tượng đang sửa
                voucher.setCreatedAt(existingVoucher.getCreatedAt());

                // BƯỚC 3: Giữ nguyên trạng thái active cũ nếu form không gửi lên
                voucher.setActive(existingVoucher.isActive());

                // BƯỚC 3.5: Giữ nguyên cờ isPersonal từ DB - tránh manager vô tình expose voucher VIP
                voucher.setPersonal(existingVoucher.isPersonal());

                // BƯỚC 4: Kiểm tra trùng mã (Nhưng phải loại trừ chính nó)
                Voucher voucherWithSameCode = voucherRepository.findByCode(voucher.getCode());
                if (voucherWithSameCode != null && voucherWithSameCode.getId() != voucher.getId()) {
                    throw new RuntimeException("Mã Voucher này đã được sử dụng bởi một voucher khác!");
                }

                // BƯỚC 5: Nếu thay đổi ngày hết hạn, gỡ bỏ liên kết với mọi Account
                // Điều này giúp tránh việc voucher cũ tự động "hồi sinh" trong ví khách hàng
                if (existingVoucher.getExpiryDate() != null &&
                        !existingVoucher.getExpiryDate().isEqual(voucher.getExpiryDate())) {
                    clearVoucherFromAllAccounts(voucher.getId());
                }
            }
        }

        // Cuối cùng mới thực hiện merge (lưu hoặc cập nhật)
        return voucherRepository.saveOrUpdate(voucher);
    }

    /**
     * Hàm bổ trợ xóa toàn bộ liên kết giữa Voucher và các Account (cả myVouchers và usedVouchers)
     */
    private void clearVoucherFromAllAccounts(int voucherId) {
        Session session = sessionFactory.getCurrentSession();
        Voucher voucher = voucherRepository.findById(voucherId);
        if (voucher == null) return;

        // 1. Gỡ khỏi myVouchers
        String hqlMy = "SELECT a FROM Account a JOIN a.myVouchers v WHERE v.id = :vId";
        List<example.entity.Account> accountsWithMy = session.createQuery(hqlMy, example.entity.Account.class)
                .setParameter("vId", voucherId)
                .getResultList();
        for (example.entity.Account acc : accountsWithMy) {
            acc.getMyVouchers().remove(voucher);
        }

        // 2. Gỡ khỏi usedVouchers
        String hqlUsed = "SELECT a FROM Account a JOIN a.usedVouchers v WHERE v.id = :vId";
        List<example.entity.Account> accountsWithUsed = session.createQuery(hqlUsed, example.entity.Account.class)
                .setParameter("vId", voucherId)
                .getResultList();
        for (example.entity.Account acc : accountsWithUsed) {
            acc.getUsedVouchers().remove(voucher);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> findAllPaged(int page, int size) {
        return voucherRepository.findAllPaged(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotal() {
        return voucherRepository.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> findAll() {
        return voucherRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher findById(int id) {
        return voucherRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher findByCode(String code) {
        return voucherRepository.findByCode(code);
    }

    @Override
    @Transactional
    public void deleteVoucher(int id) {
        Voucher voucher = voucherRepository.findById(id);
        if (voucher != null) {
            // Sử dụng hàm bổ trợ để dọn dẹp quan hệ
            clearVoucherFromAllAccounts(id);
            // Sau đó mới xóa thực thể
            voucherRepository.delete(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> getActiveVouchers() {
        return voucherRepository.findActiveVouchers();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValid(String code) {
        Voucher v = voucherRepository.findByCode(code);
        if (v == null) return false;

        // Hợp lệ khi: Đang active và Ngày hết hạn sau thời điểm hiện tại
        return v.isActive() && v.getExpiryDate().isAfter(LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean collectVoucher(int accountId, int voucherId) {
        Session session = sessionFactory.getCurrentSession();
        example.entity.Account account = session.get(example.entity.Account.class, accountId);
        Voucher voucher = voucherRepository.findById(voucherId);

        if (account != null && voucher != null) {
            // Từ chối nếu đây là voucher cá nhân (VIP reward) - không cho phép collect bởi bất kỳ ai
            if (voucher.isPersonal()) {
                return false;
            }

            // Kiểm tra xem voucher đã có trong bộ sưu tập hoặc đã sử dụng chưa
            boolean alreadySaved = account.getMyVouchers().stream().anyMatch(v -> v.getId() == voucher.getId());
            boolean alreadyUsed = account.getUsedVouchers().stream().anyMatch(v -> v.getId() == voucher.getId());
            if (alreadySaved || alreadyUsed) {
                return false; // Đã tồn tại rồi
            }

            account.getMyVouchers().add(voucher);
            session.merge(account);
            return true;
        } else {
            throw new RuntimeException("Không tìm thấy tài khoản hoặc mã giảm giá!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getSavedVoucherIds(int accountId) {
        Session session = sessionFactory.getCurrentSession();
        // Lấy danh sách ID voucher từ myVouchers
        String hqlSaved = "SELECT v.id FROM Account a JOIN a.myVouchers v WHERE a.accountID = :accId";
        List<Integer> saved = session.createQuery(hqlSaved, Integer.class)
                .setParameter("accId", accountId).getResultList();

        // Lấy danh sách ID voucher từ usedVouchers
        String hqlUsed = "SELECT v.id FROM Account a JOIN a.usedVouchers v WHERE a.accountID = :accId";
        List<Integer> used = session.createQuery(hqlUsed, Integer.class)
                .setParameter("accId", accountId).getResultList();

        saved.addAll(used);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> getVouchersByAccountId(int accountId) {
        Session session = sessionFactory.getCurrentSession();
        // Lấy toàn bộ thực thể Voucher của người dùng
        String hql = "SELECT v FROM Account a JOIN a.myVouchers v WHERE a.accountID = :accId";
        return session.createQuery(hql, Voucher.class)
                .setParameter("accId", accountId)
                .getResultList();
    }

    @Override
    @Transactional
    public void markVoucherAsUsed(int accountId, String voucherCode) {
        if (voucherCode == null || voucherCode.trim().isEmpty()) return;
        Session session = sessionFactory.getCurrentSession();
        example.entity.Account account = session.get(example.entity.Account.class, accountId);
        Voucher voucherFromDb = voucherRepository.findByCode(voucherCode);

        if (account != null && voucherFromDb != null) {
            // Cẩn thận tìm element đúng Proxy của Hibernate trong Set bằng ID
            Voucher exactVoucher = null;
            for (Voucher v : account.getMyVouchers()) {
                if (v.getId() == voucherFromDb.getId()) {
                    exactVoucher = v;
                    break;
                }
            }
            if (exactVoucher != null) {
                // Xóa khỏi danh sách Voucher hiện đang sở hữu
                account.getMyVouchers().remove(exactVoucher);

                if (exactVoucher.isPersonal()) {
                    // NẾU LÀ VOUCHER VIP: Thay vì bỏ vào "túi đã dùng", ta xóa sổ luôn khỏi Hệ thống!
                    // 1. Đảm bảo gỡ liên kết từ tất cả các account để không lỗi khóa ngoại
                    clearVoucherFromAllAccounts(exactVoucher.getId());
                    
                    // 2. Xóa sạch khỏi CSDL rễ (Mất luôn ở trang Admin)
                    voucherRepository.delete(exactVoucher.getId());
                } else {
                    // VOUCHER THƯỜNG: Chuyển sang túi "đã dùng" và giữ nguyên trong CSDL
                    account.getUsedVouchers().add(exactVoucher);
                }
                session.merge(account);
            }
        }
    }
}