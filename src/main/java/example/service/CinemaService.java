package example.service;

import example.entity.Cinema;
import example.repository.CinemaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CinemaService {

    @Autowired
    private CinemaRepository cinemaRepository;

    public List<Cinema> getAllCinemas() {
        return cinemaRepository.findAll();
    }

    @Transactional
    public void saveCinema(Cinema cinema) {
        // Đảm bảo rạp luôn có trạng thái mặc định nếu người dùng không chọn
        if (cinema.getStatus() == null || cinema.getStatus().isEmpty()) {
            cinema.setStatus("Hoạt động");
        }

        // Bạn có thể thêm logic kiểm tra dữ liệu tại đây (ví dụ: số điện thoại hợp lệ)
        cinemaRepository.save(cinema);
    }

    public List<Cinema> searchCinemas(String name, String city, String status,
                                      String address, Integer minRooms, String phone) {
        // Thực hiện chuẩn hóa chuỗi tìm kiếm (loại bỏ khoảng trắng thừa) trước khi gọi Repo
        String trimmedName = (name != null) ? name.trim() : null;
        String trimmedAddress = (address != null) ? address.trim() : null;
        String trimmedPhone = (phone != null) ? phone.trim() : null;

        return cinemaRepository.searchCinemas(trimmedName, city, status, trimmedAddress, minRooms, trimmedPhone);
    }

    public Cinema getCinemaById(int id) {
        return cinemaRepository.findById(id);
    }

    @Transactional
    public boolean deleteCinema(int id) {
        // Trước khi xóa rạp, kiểm tra xem rạp có phòng chiếu nào không
        if (cinemaRepository.hasDependencies(id)) {
            return false;
        }
        cinemaRepository.delete(id);
        return true;
    }

    public List<Cinema> getAllCinemasPaged(int page, int pageSize) {
        if (page < 1) page = 1;
        return cinemaRepository.findAllPaged(page, pageSize);
    }

    public long getTotalCount() {
        return cinemaRepository.getTotalCount();
    }
}