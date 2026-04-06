package example.controller;

import example.entity.Cinema;
import example.entity.CinemaRoom;
import example.service.CinemaRoomService;
import example.service.CinemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/room")
public class CinemaRoomController {

    @Autowired
    private CinemaRoomService roomService;

    @Autowired
    private CinemaService cinemaService;

    // Đảm bảo Form luôn được khởi tạo để tránh lỗi bind dữ liệu trong Modal
    @ModelAttribute("roomForm")
    public CinemaRoom setupRoomForm() {
        return new CinemaRoom();
    }

    // Hiển thị danh sách phòng theo Rạp
    @GetMapping("/manager")
    public String listRooms(@RequestParam("cinemaId") int cinemaId, Model model) {
        Cinema cinema = cinemaService.getCinemaById(cinemaId);
        if (cinema == null) {
            return "redirect:/admin/manager_cinema";
        }

        model.addAttribute("cinema", cinema);
        // roomService.getRoomsByCinemaId nên được cấu hình JOIN FETCH seats để hiện số ghế chính xác
        model.addAttribute("rooms", roomService.getRoomsByCinemaId(cinemaId));
        return "admin/manager_room";
    }

    // Thêm phòng mới
    @PostMapping("/add")
    public String addRoom(@RequestParam("cinemaId") int cinemaId,
                          @ModelAttribute("roomForm") CinemaRoom room,
                          RedirectAttributes ra) {
        if (roomService.isRoomNameExists(room.getRoomName(), cinemaId, null)) {
            ra.addFlashAttribute("error", "Tên phòng chiếu đã tồn tại trong rạp này!");
            return "redirect:/admin/room/manager?cinemaId=" + cinemaId;
        }

        Cinema cinema = cinemaService.getCinemaById(cinemaId);
        room.setCinema(cinema);

        // XÓA DÒNG NÀY: room.setTotalSeats(0);
        // Lý do: Entity CinemaRoom hiện tại không còn phương thức này.
        // Số lượng ghế sẽ tự động là 0 cho đến khi bạn vào trang "Thiết kế ghế".

        roomService.saveRoom(room);

        ra.addFlashAttribute("message", "Thêm phòng chiếu thành công!");
        return "redirect:/admin/room/manager?cinemaId=" + cinemaId;
    }

    // Mở Modal sửa phòng (Load lại trang kèm biến isEdit)
    @GetMapping("/edit/{id}")
    public String editRoom(@PathVariable("id") int id, Model model) {
        CinemaRoom room = roomService.getRoomById(id);
        if (room == null) return "redirect:/admin/manager_cinema";

        Cinema cinema = room.getCinema();

        model.addAttribute("roomForm", room);
        model.addAttribute("cinema", cinema);
        model.addAttribute("rooms", roomService.getRoomsByCinemaId(cinema.getId()));
        model.addAttribute("isEdit", true);

        return "admin/manager_room";
    }

    // Cập nhật thông tin phòng
    @PostMapping("/update")
    public String updateRoom(@ModelAttribute("roomForm") CinemaRoom room,
                             @RequestParam("cinemaId") int cinemaId,
                             RedirectAttributes ra) {
        if (roomService.isRoomNameExists(room.getRoomName(), cinemaId, room.getId())) {
            ra.addFlashAttribute("error", "Tên phòng chiếu đã tồn tại trong rạp này!");
            return "redirect:/admin/room/manager?cinemaId=" + cinemaId;
        }

        Cinema cinema = cinemaService.getCinemaById(cinemaId);
        room.setCinema(cinema);

        // Cập nhật thông tin (Service sẽ xử lý việc giữ nguyên số ghế cũ hoặc cập nhật)
        roomService.updateRoom(room);

        ra.addFlashAttribute("message", "Cập nhật thông tin phòng thành công!");
        return "redirect:/admin/room/manager?cinemaId=" + cinemaId;
    }

    // Xóa phòng chiếu
    @GetMapping("/delete")
    public String deleteRoom(@RequestParam("id") int id,
                             @RequestParam("cinemaId") int cinemaId,
                             RedirectAttributes ra) {
        try {
            boolean success = roomService.deleteRoom(id);
            if (success) {
                ra.addFlashAttribute("message", "Đã xóa phòng chiếu thành công!");
            } else {
                ra.addFlashAttribute("error", "Không thể xóa phòng này do đã có dữ liệu lịch chiếu hoặc vé liên quan.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Đã có lỗi hệ thống xảy ra khi xóa phòng chiếu.");
        }
        return "redirect:/admin/room/manager?cinemaId=" + cinemaId;
    }

    // Tìm kiếm và lọc phòng chiếu
    @GetMapping("/search")
    public String searchRooms(
            @RequestParam("cinemaId") int cinemaId,
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer minSeats,
            @RequestParam(required = false) String status,
            Model model) {

        Cinema cinema = cinemaService.getCinemaById(cinemaId);
        List<CinemaRoom> results = roomService.searchRooms(cinemaId, roomName, roomType, minSeats, status);

        model.addAttribute("cinema", cinema);
        model.addAttribute("rooms", results);

        // Đẩy lại các giá trị tìm kiếm để hiển thị trên UI (Keep state)
        model.addAttribute("searchRoomName", roomName);
        model.addAttribute("searchRoomType", roomType);
        model.addAttribute("searchMinSeats", minSeats);
        model.addAttribute("searchStatus", status);

        // Form rỗng cho Modal "Thêm mới"
        model.addAttribute("roomForm", new CinemaRoom());

        return "admin/manager_room";
    }
}