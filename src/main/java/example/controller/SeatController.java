package example.controller;

import example.entity.CinemaRoom;
import example.entity.Seat;
import example.service.CinemaRoomService;
import example.service.SeatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/seat")
public class SeatController {

    @Autowired
    private CinemaRoomService roomService;

    @Autowired
    private SeatService seatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===================== DESIGN PAGE =====================
    @GetMapping("/design")
    public String designPage(@RequestParam("roomId") int roomId, Model model) {

        CinemaRoom room = roomService.getRoomById(roomId);

        List<Seat> seats = room.getSeats() == null ? List.of() : room.getSeats();
        String jsonSeats = "[]";

        try {
            jsonSeats = objectMapper.writeValueAsString(seats);
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("room", room);
        model.addAttribute("existingSeats", jsonSeats);

        return "admin/manager_seat";
    }

    // ===================== SAVE LAYOUT =====================
    @PostMapping("/save-layout")
    @ResponseBody
    public ResponseEntity<?> saveLayout(@RequestBody Map<String, Object> payload) {

        try {
            int roomId = Integer.parseInt(payload.get("roomId").toString());

            List<Seat> seats = objectMapper.convertValue(
                    payload.get("seatList"),
                    new TypeReference<List<Seat>>() {}
            );

            CinemaRoom room = roomService.getRoomById(roomId);

            // GỌI SERVICE (Service sẽ tự gán room + xử lý logic)
            seatService.saveSeatLayout(roomId, room, seats);

            return ResponseEntity.ok(
                    Map.of("success", true, "message", "Lưu sơ đồ ghế thành công!")
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "Lỗi: " + e.getMessage())
            );
        }
    }
}
