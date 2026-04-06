package example.controller;

import example.entity.TicketPrice;
import example.service.TicketPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class TicketPriceController {

    @Autowired
    private TicketPriceService ticketPriceService;

    // ==========================================
    // 1. VIEW (Trả về giao diện HTML)
    // ==========================================

    @GetMapping("/admin/manager_ticket_price")
    public String showPage(Model model) {
        // Lấy danh sách đổ vào bảng HTML
        model.addAttribute("ticketPrices", ticketPriceService.getAll());
        return "admin/manager_ticket_price"; // Tên file HTML trong thư mục templates
    }

    // ==========================================
    // 2. API (Trả về JSON cho AJAX gọi)
    // ==========================================

    @PostMapping("/admin/ticket-price/add")
    @ResponseBody
    public ResponseEntity<?> add(@RequestBody TicketPrice ticketPrice) {
        try {
            ticketPrice.setId(null); // Đảm bảo tạo mới
            ticketPriceService.save(ticketPrice);
            return ResponseEntity.ok(Map.of("success", true, "message", "Thêm loại vé thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/ticket-price/update")
    @ResponseBody
    public ResponseEntity<?> update(@RequestBody TicketPrice ticketPrice) {
        try {
            if (ticketPrice.getId() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "ID không hợp lệ!"));
            }
            ticketPriceService.save(ticketPrice);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/ticket-price/delete")
    @ResponseBody
    public ResponseEntity<?> delete(@RequestBody Map<String, Integer> payload) {
        try {
            Integer id = payload.get("id");
            ticketPriceService.delete(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa loại vé!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/ticket-price/update-status")
    @ResponseBody
    public ResponseEntity<?> updateStatus(@RequestBody Map<String, Object> payload) {
        try {
            // Lấy dữ liệu từ JSON gửi lên
            String idStr = String.valueOf(payload.get("id"));
            Integer id = Integer.parseInt(idStr);
            Boolean active = (Boolean) payload.get("active");

            // Gọi Service xử lý
            ticketPriceService.updateStatus(id, active);

            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }
}