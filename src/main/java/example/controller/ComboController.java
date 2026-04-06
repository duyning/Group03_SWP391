package example.controller;

import example.entity.Combo;
import example.service.ComboService;
import jakarta.servlet.http.HttpServletRequest; // Import quan trọng
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class ComboController {

    @Autowired
    private ComboService comboService;

    // --- VIEW ---
    @GetMapping("/admin/manager_combo")
    public String showPage(Model model) {
        model.addAttribute("combos", comboService.getAll());
        return "admin/manager_combo";
    }

    // --- API ADD ---
    @PostMapping("/admin/combo/add")
    @ResponseBody
    public ResponseEntity<?> add(
            @RequestParam("comboName") String comboName,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            HttpServletRequest request // Thêm biến này để lấy đường dẫn thực
    ) {
        try {
            Combo combo = new Combo();
            combo.setComboName(comboName);
            combo.setDescription(description);
            combo.setPrice(price);
            combo.setActive(true);

            // Gọi hàm xử lý file chung
            handleFileUpload(combo, imageFile, request);

            comboService.save(combo);
            return ResponseEntity.ok(Map.of("success", true, "message", "Thêm thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- API UPDATE ---
    @PostMapping("/admin/combo/update")
    @ResponseBody
    public ResponseEntity<?> update(
            @RequestParam("id") Integer id,
            @RequestParam("comboName") String comboName,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String oldImageUrl,
            HttpServletRequest request // Thêm biến này
    ) {
        try {
            Combo combo = new Combo();
            combo.setId(id);
            combo.setComboName(comboName);
            combo.setDescription(description);
            combo.setPrice(price);

            // Logic xử lý ảnh:
            if (imageFile != null && !imageFile.isEmpty()) {
                // Nếu có ảnh mới -> Gọi hàm upload đè lên
                handleFileUpload(combo, imageFile, request);
            } else {
                // Nếu không có ảnh mới -> Giữ nguyên link cũ
                combo.setImageUrl(oldImageUrl);
            }

            comboService.save(combo);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    // --- API DELETE ---
    @PostMapping("/admin/combo/delete")
    @ResponseBody
    public ResponseEntity<?> delete(@RequestBody Map<String, Integer> payload) {
        try {
            comboService.delete(payload.get("id"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa Combo!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/combo/update-status")
    @ResponseBody
    public ResponseEntity<?> updateStatus(@RequestBody Map<String, Object> payload) {
        try {
            Integer id = Integer.parseInt(payload.get("id").toString());
            boolean active = Boolean.parseBoolean(payload.get("active").toString());
            comboService.updateStatus(id, active);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    // ========================================================
    // --- HÀM XỬ LÝ FILE (Đã chỉnh sửa cho Combo) ---
    // ========================================================
    private void handleFileUpload(Combo combo, MultipartFile file, HttpServletRequest request) {
        try {
            if (file != null && !file.isEmpty()) {
                // Lấy đường dẫn thực tế (Real Path) nơi Tomcat đang chạy
                String uploadDir = request.getServletContext().getRealPath("/resources/images/");

                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                // Tạo tên file duy nhất
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

                // Lưu file vào thư mục đó
                file.transferTo(new File(dir, fileName));

                // Cập nhật đường dẫn vào đối tượng Combo
                // Lưu ý: Combo dùng setImageUrl, Movie dùng setImgUrl
                combo.setImageUrl("resources/images/" + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- THÊM API TÌM KIẾM ---
    @GetMapping("/admin/combo/search")
    public String search(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            // Status gửi lên là "1" hoặc "0", ta nhận dưới dạng String rồi ép kiểu
            @RequestParam(value = "status", required = false) String statusStr,
            Model model) {

        // Chuyển đổi status sang Boolean
        Boolean status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            status = statusStr.equals("1");
        }

        // Gọi Service lấy danh sách
        List<Combo> combos = comboService.search(name, description, maxPrice, status);

        // Đẩy dữ liệu ra view
        model.addAttribute("combos", combos);

        // Đẩy lại các giá trị tìm kiếm để hiển thị (giữ form không bị trắng)
        model.addAttribute("searchName", name);
        model.addAttribute("searchDescription", description);
        model.addAttribute("searchMaxPrice", maxPrice);
        model.addAttribute("searchStatus", statusStr);

        return "admin/manager_combo";
    }
}