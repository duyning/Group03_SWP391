package example.service;

import example.entity.Combo;
import example.repository.ComboRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ComboService {

    @Autowired
    private ComboRepository comboRepository;

    public List<Combo> getAll() {
        return comboRepository.findAll();
    }

    public void save(Combo combo) {
        // Nếu là update (có ID), mà active gửi lên là null thì giữ nguyên active cũ
        if (combo.getId() != null) {
            Combo old = comboRepository.findById(combo.getId());
            if (old != null && combo.getActive() == null) {
                combo.setActive(old.getActive());
            }
        } else {
            // Nếu thêm mới thì mặc định active = true
            if (combo.getActive() == null) combo.setActive(true);
        }
        comboRepository.save(combo);
    }

    public void delete(Integer id) {
        comboRepository.deleteById(id);
    }

    // Hàm xử lý riêng cho nút switch Active
    public void updateStatus(Integer id, boolean isActive) {
        Combo combo = comboRepository.findById(id);
        if (combo != null) {
            combo.setActive(isActive);
            comboRepository.save(combo);
        }
    }

    // --- THÊM HÀM TÌM KIẾM ---
    public List<Combo> search(String name, String description, Double maxPrice, Boolean status) {
        return comboRepository.search(name, description, maxPrice, status);
    }

    public Combo findById(Integer id) {
        return comboRepository.findById(id);
    }
}