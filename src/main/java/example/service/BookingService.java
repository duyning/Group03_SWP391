package example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.entity.Combo;
import example.entity.ComboBookingDTO;
import example.entity.Seat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BookingService {

        @Autowired
        private ShowtimeService showtimeService;

        @Autowired
        private SeatService seatService;

        @Autowired
        private ComboService comboService;

        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * Hàm tổng hợp dữ liệu để hiển thị trang Payment
         * @param seatIds Chuỗi ID ghế (VD: "12,13")
         * @param comboDataJson Chuỗi JSON combo từ FE
         * @param showtimeId ID suất chiếu
         */
        public Double calculateTotalAmount(String seatIds, String comboDataJson, int showtimeId,
                                           List<ComboBookingDTO> outSelectedCombos,
                                           List<Seat> outSelectedSeats) {
            double total = 0;

            // 1. Lấy thông tin suất chiếu và sơ đồ ghế (đã có tính giá động bên trong)
            List<Map<String, Object>> seatMap = showtimeService.getSeatMap(showtimeId);

            // 2. Tính tiền ghế dựa trên danh sách ID được chọn
            if (seatIds != null && !seatIds.isEmpty()) {
                String[] selectedIdArray = seatIds.split(",");
                for (String idStr : selectedIdArray) {
                    int id = Integer.parseInt(idStr.trim());

                    // Tìm thông tin ghế và giá trong seatMap của ShowtimeService
                    for (Map<String, Object> seatInfo : seatMap) {
                        if ((int)seatInfo.get("id") == id) {
                            total += (double) seatInfo.get("price");

                            // Add vào list output để hiển thị ở Controller/View
                            Seat s = new Seat();
                            s.setId(id);
                            s.setSeatNumber((String) seatInfo.get("code"));
                            outSelectedSeats.add(s);
                            break;
                        }
                    }
                }
            }

            // 3. Giải mã JSON Combo và tính tiền Combo
            try {
                if (comboDataJson != null && !comboDataJson.equals("[]") && !comboDataJson.isEmpty()) {
                    List<ComboBookingDTO> combos = objectMapper.readValue(comboDataJson,
                            new TypeReference<List<ComboBookingDTO>>() {});

                    for (ComboBookingDTO dto : combos) {
                        Combo combo = comboService.findById(dto.getComboId());
                        if (combo != null && combo.getActive()) {
                            dto.setComboName(combo.getComboName());
                            dto.setPrice(combo.getPrice());
                            total += (combo.getPrice() * dto.getQuantity());
                            outSelectedCombos.add(dto);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return total;
        }
}
