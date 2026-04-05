package example.controller;

import example.config.VNPayConfig;
import example.entity.*;
import example.repository.BookingRepository;
import example.repository.SeatRepository;
import example.repository.ShowtimeRepository;
import example.repository.TicketRepository;
import example.service.AccountService;
import example.service.ComboService;
import example.service.ShowtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class PaymentController {

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ComboService comboService;

    @Autowired
    private example.service.VoucherService voucherService;

    @Autowired
    private example.service.EmailService emailService;

    @Autowired
    private example.service.MembershipService membershipService;

    @PostMapping("/vnpay/create-payment")
    public String createPayment(
            @RequestParam("showtimeId") int showtimeId,
            @RequestParam(value = "voucherCode", required = false) String voucherCode,
            @RequestParam("seatNames") String seatNames,
            @RequestParam("comboData") String comboData,
            @RequestParam("totalPrice") Double totalPrice,
            HttpServletRequest request,
            HttpSession session, Principal principal) throws Exception {

        if (totalPrice == null) {
            totalPrice = 100000.0;
        }

        // 1. Tạo Booking và Ticket với trạng thái PENDING để Khóa ghế
        String email = principal.getName();
        Account currentAccount = accountService.findByEmail(email);
        String seatIds = (String) session.getAttribute("booking_seatIds");
        
        // Kiểm tra xem ghế đã bị khóa bởi người khác chưa
        if (seatIds != null && !seatIds.isEmpty()) {
            boolean isLocked = false;
            String[] idArray = seatIds.split(",");
            List<Integer> bookedSeats = ticketRepository.getBookedSeatIds(showtimeId);
            for (String idStr : idArray) {
                if (bookedSeats.contains(Integer.parseInt(idStr.trim()))) {
                    isLocked = true;
                    break;
                }
            }
            if (isLocked) {
                // Ghế đã bị lấy, báo lỗi và quay lại trang chọn ghế
                session.setAttribute("error_message", "Ghế bạn chọn vừa được khách hàng khác thanh toán. Vui lòng chọn ghế khác.");
                return "redirect:/booking/seat?showtimeId=" + showtimeId;
            }
        }

        // Xử lý chuỗi JSON Combo
        StringBuilder comboSummary = new StringBuilder();
        if (comboData != null && !comboData.isEmpty() && !comboData.equals("[]")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, Object>> comboList = mapper.readValue(comboData, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> item : comboList) {
                    Integer comboId = (Integer) item.get("comboId");
                    Integer quantity = (Integer) item.get("quantity");
                    Combo combo = comboService.findById(comboId);
                    if (combo != null) {
                        if (comboSummary.length() > 0) comboSummary.append(", ");
                        comboSummary.append(quantity).append("x ").append(combo.getComboName());
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi parse JSON Combo: " + e.getMessage());
                comboSummary.append(comboData);
            }
        }

        Booking booking = new Booking();
        booking.setAccount(currentAccount);
        booking.setShowtime(showtimeService.getShowtimeById(showtimeId));
        booking.setSeatNumbers(seatNames);
        booking.setComboDetails(comboSummary.toString());
        booking.setTotalAmount(totalPrice);
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus("PENDING"); // Khóa ghế tạm thời

        if (seatNames != null && !seatNames.isEmpty()) {
            booking.setTicketQuantity(seatNames.split(",").length);
        } else {
            booking.setTicketQuantity(0);
        }

        booking = bookingRepository.save(booking);

        if (seatIds != null && !seatIds.isEmpty()) {
            String[] idArray = seatIds.split(",");
            for (String idStr : idArray) {
                try {
                    int sId = Integer.parseInt(idStr.trim());
                    Seat seat = seatRepository.findById(sId);
                    if (seat != null) {
                        Ticket ticket = new Ticket();
                        ticket.setShowtime(booking.getShowtime());
                        ticket.setSeat(seat);
                        ticket.setPrice(0);
                        ticket.setBooking(booking); // Liên kết Ticket với Booking
                        ticket.setBookingTime(LocalDateTime.now());
                        ticketRepository.save(ticket);
                    }
                } catch (Exception ex) {
                    System.err.println("Lỗi lưu Ticket khóa ghế: " + ex.getMessage());
                }
            }
        }

        session.setAttribute("booking_voucherCode", voucherCode);

        // 2. Tạo URL VNPay
        long amount = (long) (totalPrice * 100);
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = String.valueOf(booking.getId()); // Dùng ID Booking làm mã giao dịch
        String vnp_IpAddr = VNPayConfig.getIpAddress(request);
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan ve xem phim cho ma GD: " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");

        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        String returnUrl = baseUrl + "/vnpay/payment-return";
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;

        return "redirect:" + paymentUrl;
    }

    @Transactional
    @GetMapping("/vnpay/payment-return")
    public String paymentReturn(HttpServletRequest request, HttpSession session, Principal principal, Model model) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }
        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String signValue = VNPayConfig.hashAllFields(fields);
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        
        Long bookingId = null;
        try {
            bookingId = Long.parseLong(vnp_TxnRef);
        } catch (NumberFormatException e) { }

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_ResponseCode")) && bookingId != null) {
                try {
                    Booking booking = bookingRepository.findById(bookingId);
                    if (booking != null && "PENDING".equals(booking.getStatus())) {
                        booking.setStatus("PAID");
                        bookingRepository.save(booking);

                        Account currentAccount = booking.getAccount();
                        
                        try {
                            membershipService.addPointsAndUpgrade(currentAccount, booking.getTotalAmount());
                        } catch(Exception e) {
                            System.err.println("Lỗi tích điểm: " + e.getMessage());
                        }

                        String voucherCode = (String) session.getAttribute("booking_voucherCode");
                        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                            voucherService.markVoucherAsUsed(currentAccount.getAccountID(), voucherCode);
                        }

                        try {
                            emailService.sendBookingConfirmation(currentAccount.getEmail(), booking);
                        } catch (Exception ex) {
                            System.err.println("Không thể gửi email: " + ex.getMessage());
                        }

                        model.addAttribute("message", "Thanh toán thành công!");
                        model.addAttribute("booking", booking);
                    }
                } catch (Exception e) {
                    model.addAttribute("message", "Lỗi khi xử lý trả về thanh toán.");
                    e.printStackTrace();
                } finally {
                    session.removeAttribute("booking_seatIds");
                    session.removeAttribute("booking_voucherCode");
                }
                return "user/payment_success";
            } else {
                // Xóa Booking và Ticket để nhả ghế
                if (bookingId != null) {
                    Booking booking = bookingRepository.findById(bookingId);
                    if (booking != null && "PENDING".equals(booking.getStatus())) {
                        ticketRepository.deleteByBookingId(bookingId);
                        bookingRepository.deleteById(bookingId);
                    }
                }
                model.addAttribute("message", "Giao dịch thất bại / Đã hủy.");
                return "user/payment_failure";
            }
        } else {
            // Chữ ký sai, nên hủy booking luôn
            if (bookingId != null) {
                Booking booking = bookingRepository.findById(bookingId);
                if (booking != null && "PENDING".equals(booking.getStatus())) {
                    ticketRepository.deleteByBookingId(bookingId);
                    bookingRepository.deleteById(bookingId);
                }
            }
            model.addAttribute("message", "Chữ ký không hợp lệ.");
            return "user/payment_failure";
        }
    }
}
