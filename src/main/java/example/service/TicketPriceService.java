package example.service;

import example.entity.TicketPrice;
import example.repository.TicketPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TicketPriceService {

    @Autowired
    private TicketPriceRepository ticketPriceRepository;

    public List<TicketPrice> getAll() {
        return ticketPriceRepository.findAll();
    }

    public void save(TicketPrice ticketPrice) {
        // Logic giữ nguyên active cũ nếu là Update
        if (ticketPrice.getId() != null) {
            TicketPrice existing = ticketPriceRepository.findById(ticketPrice.getId());

            // Nếu tìm thấy bản ghi cũ và bản ghi mới chưa có trạng thái active
            if (existing != null && ticketPrice.getActive() == null) {
                ticketPrice.setActive(existing.getActive());
            }
        }
        ticketPriceRepository.save(ticketPrice);
    }

    public void delete(Integer id) {
        ticketPriceRepository.deleteById(id);
    }

    // API update status (cho cái nút Switch)
    public void updateStatus(Integer id, boolean isActive) {
        TicketPrice tp = ticketPriceRepository.findById(id);
        if (tp != null) {
            tp.setActive(isActive);
            ticketPriceRepository.save(tp);
        }
    }
}