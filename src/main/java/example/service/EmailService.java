package example.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Gửi email chứa đường dẫn đổi lại mật khẩu cho người dùng
    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Beta Cinemas - Yêu cầu đặt lại mật khẩu");

            String htmlContent =
                    "<!DOCTYPE html>" +
                            "<html lang=\"vi\"><head><meta charset=\"UTF-8\"></head>" +
                            "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',sans-serif;\">" +
                            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:40px 0;\">" +
                            "<tr><td align=\"center\">" +
                            "<table width=\"580\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);\">" +
                            "<tr><td style=\"background:#1e293b;padding:30px;text-align:center;\">" +
                            "<h1 style=\"color:#fff;margin:0;font-size:22px;font-weight:800;\">🎬 BETA CINEMAS</h1>" +
                            "<p style=\"color:#94a3b8;margin:6px 0 0;font-size:13px;\">Hệ thống quản lý rạp chiếu phim</p>" +
                            "</td></tr>" +
                            "<tr><td style=\"padding:40px 50px;\">" +
                            "<h2 style=\"color:#1e293b;margin:0 0 16px;font-size:20px;\">Đặt lại mật khẩu</h2>" +
                            "<p style=\"color:#475569;line-height:1.6;margin:0 0 24px;\">Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản gắn với địa chỉ email này. Nhấn vào nút bên dưới để tiếp tục.</p>" +
                            "<div style=\"text-align:center;margin:32px 0;\">" +
                            "<a href=\"" + resetLink + "\" style=\"display:inline-block;background:#0054a6;color:#fff;text-decoration:none;padding:14px 36px;border-radius:10px;font-size:15px;font-weight:700;\">🔑 Đặt lại mật khẩu</a>" +
                            "</div>" +
                            "<p style=\"color:#94a3b8;font-size:13px;line-height:1.6;margin:0;\">Link này sẽ hết hạn sau <strong>1 giờ</strong>.<br>Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>" +
                            "</td></tr>" +
                            "<tr><td style=\"background:#f8fafc;padding:20px;text-align:center;border-top:1px solid #f1f5f9;\">" +
                            "<p style=\"color:#94a3b8;font-size:12px;margin:0;\">© 2026 Beta Cinemas.</p>" +
                            "</td></tr>" +
                            "</table></td></tr></table>" +
                            "</body></html>";

            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.", e);
        }
    }
    public void sendBookingConfirmation(String to, example.entity.Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Beta Cinemas - Xác nhận đặt vé thành công (#BK" + booking.getId() + ")");

            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

            // Tạo mã QR an toàn
            String qrInfo = "VÉ ĐIỆN TỬ - BETA CINEMAS\n" +
                    "Mã Đặt Vé: #" + booking.getId() + "\n" +
                    "Khách hàng: " + (booking.getAccount() != null ? booking.getAccount().getName() : "Khách") + "\n" +
                    "Phim: " + booking.getShowtime().getMovie().getMovieName() + "\n" +
                    "Rạp: " + booking.getShowtime().getRoom().getCinema().getCinemaName() + " - Ph: " + booking.getShowtime().getRoom().getRoomName() + "\n" +
                    "Thời gian: " + booking.getShowtime().getStartTime().format(timeFormatter) + " ngày " + booking.getShowtime().getStartDate().format(dateFormatter) + "\n" +
                    "Ghế: " + (booking.getSeatNumbers() != null ? booking.getSeatNumbers() : "N/A");

            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&color=0054a6&bgcolor=ffffff&qzone=1&data=" +
                    java.net.URLEncoder.encode(qrInfo, "UTF-8");

            String comboHtml = (booking.getComboDetails() != null && !booking.getComboDetails().isEmpty()) ?
                    "<tr><td style=\"padding:12px 0;border-bottom:1px dashed #cbd5e1;color:#475569;\"><strong>Combo</strong></td><td style=\"padding:12px 0;text-align:right;border-bottom:1px dashed #cbd5e1;color:#0f172a;\">" + booking.getComboDetails() + "</td></tr>" : "";

            String htmlContent =
                    "<!DOCTYPE html>" +
                            "<html lang=\"vi\"><head><meta charset=\"UTF-8\"></head>" +
                            "<body style=\"margin:0;padding:20px;background:#f8fafc;font-family:'Segoe UI',sans-serif;\">" +
                            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.05);\">" +
                            "<tr><td style=\"background:linear-gradient(135deg, #0054a6, #003b75);padding:30px;text-align:center;\">" +
                            "<h1 style=\"color:#fff;margin:0;font-size:24px;letter-spacing:1px;\">🎬 BETA CINEMAS</h1>" +
                            "<p style=\"color:#93c5fd;margin:8px 0 0;font-size:14px;\">Xác nhận đặt vé điện tử</p>" +
                            "</td></tr>" +
                            "<tr><td style=\"padding:40px;\">" +
                            "<h2 style=\"color:#1e293b;margin:0 0 10px;font-size:18px;\">Xin chào " + (booking.getAccount() != null ? booking.getAccount().getName() : "") + ",</h2>" +
                            "<p style=\"color:#64748b;line-height:1.6;margin:0 0 30px;\">Giao dịch thanh toán vé xem phim của bạn đã thành công. Dưới đây là thông tin chi tiết vé điện tử của bạn:</p>" +

                            "<div style=\"background:#f1f5f9;border-radius:12px;padding:25px;margin-bottom:30px;\">" +
                            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:15px;\">" +
                            "<tr><td style=\"padding:0 0 12px;border-bottom:1px dashed #cbd5e1;color:#475569;\"><strong>Mã giao dịch</strong></td><td style=\"padding:0 0 12px;text-align:right;border-bottom:1px dashed #cbd5e1;color:#0054a6;font-weight:700;\">#BK" + booking.getId() + "</td></tr>" +
                            "<tr><td style=\"padding:12px 0;border-bottom:1px dashed #cbd5e1;color:#475569;\"><strong>Tên phim</strong></td><td style=\"padding:12px 0;text-align:right;border-bottom:1px dashed #cbd5e1;color:#0f172a;font-weight:600;\">" + booking.getShowtime().getMovie().getMovieName() + "</td></tr>" +
                            "<tr><td style=\"padding:12px 0;border-bottom:1px dashed #cbd5e1;color:#475569;\"><strong>Rạp chiếu</strong></td><td style=\"padding:12px 0;text-align:right;border-bottom:1px dashed #cbd5e1;color:#0f172a;\">" + booking.getShowtime().getRoom().getCinema().getCinemaName() + " - " + booking.getShowtime().getRoom().getRoomName() + "</td></tr>" +
                            "<tr><td style=\"padding:12px 0;border-bottom:1px dashed #cbd5e1;color:#475569;\"><strong>Suất chiếu</strong></td><td style=\"padding:12px 0;text-align:right;border-bottom:1px dashed #cbd5e1;color:#0f172a;\">" + booking.getShowtime().getStartTime().format(timeFormatter) + " - " + booking.getShowtime().getStartDate().format(dateFormatter) + "</td></tr>" +
                            "<tr><td style=\"padding:12px 0;border-bottom:1px dashed #cbd5e1;color:#475569;\"><strong>Ghế ngồi</strong></td><td style=\"padding:12px 0;text-align:right;border-bottom:1px dashed #cbd5e1;color:#0f172a;font-weight:600;color:#0054a6;\">" + booking.getSeatNumbers() + "</td></tr>" +
                            comboHtml +
                            "<tr><td style=\"padding:12px 0 0;color:#475569;\"><strong>Tổng thanh toán</strong></td><td style=\"padding:12px 0 0;text-align:right;color:#e11d48;font-weight:800;font-size:18px;\">" + String.format("%,.0f", booking.getTotalAmount()) + " đ</td></tr>" +
                            "</table>" +
                            "</div>" +

                            "<div style=\"text-align:center;margin-top:20px;\">" +
                            "<h3 style=\"color:#1e293b;margin:0 0 15px;font-size:16px;\">MÃ QR VÀO RẠP</h3>" +
                            "<img src=\"" + qrUrl + "\" alt=\"QR Code\" style=\"width:220px;height:220px;border:5px solid #e2e8f0;border-radius:12px;\">" +
                            "<p style=\"color:#64748b;font-size:13px;line-height:1.5;margin:15px 0 0;\">Vui lòng xuất trình mã QR này tại quầy vé trực tiếp ở rạp.<br>Không chia sẻ mã QR cho người khác để tránh mất vé.</p>" +
                            "</div>" +

                            "</td></tr>" +
                            "<tr><td style=\"background:#1e293b;padding:20px;text-align:center;\">" +
                            "<p style=\"color:#94a3b8;font-size:12px;margin:0;\">Cảm ơn bạn đã lựa chọn Beta Cinemas!</p>" +
                            "</td></tr>" +
                            "</table>" +
                            "</body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận đặt vé: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
