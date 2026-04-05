import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class UpdateUI {
    public static void main(String[] args) throws Exception {
        String[] files = {
            "c:\\Users\\USER\\OneDrive\\Desktop\\SWT301\\demo111\\src\\main\\webapp\\WEB-INF\\user\\blog\\index.html",
            "c:\\Users\\USER\\OneDrive\\Desktop\\SWT301\\demo111\\src\\main\\webapp\\WEB-INF\\user\\blog\\detail.html",
            "c:\\Users\\USER\\OneDrive\\Desktop\\SWT301\\demo111\\src\\main\\webapp\\WEB-INF\\user\\blog\\create.html",
            "c:\\Users\\USER\\OneDrive\\Desktop\\SWT301\\demo111\\src\\main\\webapp\\WEB-INF\\user\\blog\\edit.html"
        };
        
        String headerHtml = "<div class=\"top-bar\">\n" +
            "    <div class=\"top-container\">\n" +
            "        <th:block th:if=\"${account == null}\">\n" +
            "            <a th:href=\"@{/login}\" class=\"top-link\">Đăng nhập</a>\n" +
            "            <span style=\"margin-left: 10px; opacity: 0.5;\">|</span>\n" +
            "            <a th:href=\"@{/register}\" class=\"top-link\">Đăng ký</a>\n" +
            "        </th:block>\n" +
            "        <th:block th:if=\"${account != null}\">\n" +
            "            <span style=\"color: white; font-size: 13px;\">Xin chào, </span>\n" +
            "            <a th:href=\"@{/profile}\" class=\"top-link\" th:text=\"${account.name}\" style=\"margin-left: 5px; font-weight: bold;\">Tên User</a>\n" +
            "            <span style=\"margin-left: 10px; opacity: 0.5;\">|</span>\n" +
            "            <a th:href=\"@{/logout}\" class=\"top-link\">Đăng xuất</a>\n" +
            "        </th:block>\n" +
            "        <img src=\"https://flagcdn.com/w40/gb.png\" class=\"lang-icon\" alt=\"English\" style=\"margin-left: 15px;\">\n" +
            "    </div>\n" +
            "</div>\n" +
            "<header class=\"header-main\">\n" +
            "    <div class=\"header-container\">\n" +
            "        <a th:href=\"@{/home}\"><img src=\"https://www.betacinemas.vn/Assets/Common/logo/logo.png\" class=\"logo\"></a>\n" +
            "        <nav class=\"nav-menu\">\n" +
            "            <a th:href=\"@{/home}\">LỊCH CHIẾU THEO RẠP</a>\n" +
            "            <a th:href=\"@{/home}\">PHIM</a>\n" +
            "            <a th:href=\"@{/home}\">RẠP</a>\n" +
            "            <a th:href=\"@{/home}\">GIÁ VÉ</a>\n" +
            "            <a th:href=\"@{/user/vouchers}\">KHO VOUCHER</a>\n" +
            "            <a th:href=\"@{/blog}\" style=\"color: #0054a6;\">TIN TỨC</a>\n" +
            "        </nav>\n" +
            "    </div>\n" +
            "</header>\n" +
            "<div class=\"main-content-wrapper\" style=\"flex: 1; padding: 40px 20px; width: 100%; display: flex; justify-content: center;\">\n" +
            "<div style=\"width: 100%; max-width: 800px;\">\n";
            
        String footerHtml = "</div></div>\n" +
            "<footer class=\"footer\">\n" +
            "    <div class=\"footer-container\">\n" +
            "        <div>\n" +
            "            <h4>BETA CINEMAS</h4>\n" +
            "            <p>Hệ thống rạp chiếu phim hàng đầu cho giới trẻ.</p>\n" +
            "        </div>\n" +
            "        <div>\n" +
            "            <h4>LIÊN HỆ</h4>\n" +
            "            <p>Hotline: 1900 636807</p>\n" +
            "        </div>\n" +
            "        <div>\n" +
            "            <h4>KẾT NỐI</h4>\n" +
            "            <p>Facebook | Youtube | TikTok</p>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</footer>\n";
            
        String cssAppend = "\n" +
            "        /* HEADER/FOOTER CSS */\n" +
            "        .top-bar { background: #0054a6; color: white; padding: 8px 0; font-size: 13px; }\n" +
            "        .top-container { max-width: 1200px; margin: 0 auto; display: flex; justify-content: flex-end; padding: 0 15px; }\n" +
            "        .top-link { color: white; text-decoration: none; margin-left: 10px; }\n" +
            "        .header-main { background: white; padding: 15px 0; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05); position: sticky; top: 0; z-index: 1000; }\n" +
            "        .header-container { max-width: 1300px; margin: 0 auto; display: flex; align-items: center; padding: 0 15px; }\n" +
            "        .logo { height: 55px; margin-right: 25px; }\n" +
            "        .nav-menu { display: flex; justify-content: center; flex-grow: 1; gap: 25px; }\n" +
            "        .nav-menu a { text-decoration: none; color: #333; font-weight: 700; font-size: 16px; }\n" +
            "        .footer { background: #0054a6; color: white; padding: 50px 0; margin-top: auto; width: 100%; }\n" +
            "        .footer-container { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: repeat(3, 1fr); gap: 50px; padding: 0 15px; }\n" +
            "        .main-content-wrapper { display: flex; justify-content: center; align-items: flex-start; }\n";

        for (String fi : files) {
            File file = new File(fi);
            if (!file.exists()) {
                System.out.println("File not found: " + fi);
                continue;
            }
            
            String content = new String(Files.readAllBytes(Paths.get(fi)), StandardCharsets.UTF_8);
            
            // Apply Light theme CSS Variables
            content = content.replace("--primary: #4F46E5", "--primary: #0072ff");
            content = content.replace("--bg: #0f172a", "--bg: #fdfdfd");
            content = content.replace("--card: rgba(255,255,255,0.05)", "--card: #ffffff");
            content = content.replace("--border: rgba(255,255,255,0.1)", "--border: #e2e8f0");
            
            // Replace background and animation
            content = content.replace("body { background: linear-gradient(135deg, #0f172a, #1e1b4b, #312e81); min-height: 100vh; color: #f8fafc; padding: 40px 20px; background-size: 400% 400%; animation: gradientBG 15s ease infinite; }",
                                      "body { background: #fdfdfd; min-height: 100vh; color: #333; display: flex; flex-direction: column; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }");
            content = content.replace("body { background: linear-gradient(135deg, #0f172a, #1e1b4b, #312e81); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 40px 20px; background-size: 400% 400%; animation: gradientBG 15s ease infinite; color: #f8fafc; }",
                                      "body { background: #fdfdfd; min-height: 100vh; color: #333; display: flex; flex-direction: column; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }\n.card { margin-top: 50px; flex-shrink: 0; margin-bottom: 50px; margin-left: auto; margin-right: auto;}");
            
            content = content.replace("@keyframes gradientBG { 0%,100%{background-position:0% 50%} 50%{background-position:100% 50%} }", "");
            
            // Overrides for colors
            content = content.replace("color: #f8fafc", "color: #0f172a");
            content = content.replace("color: #cbd5e1", "color: #334155");
            content = content.replace("color: #94a3b8", "color: #64748b");
            content = content.replace("background: rgba(15,23,42,0.5)", "background: #f8fafc");
            content = content.replace("background: rgba(255,255,255,0.05)", "background: #ffffff");
            content = content.replace("border: 1px solid rgba(255,255,255,0.15)", "border: 1px solid #cbd5e1");
            content = content.replace("border: 1px solid rgba(255,255,255,0.1)", "border: 1px solid #e2e8f0");
            content = content.replace("background: rgba(15, 23, 42, 0.95)", "background: #ffffff");
            content = content.replace("backdrop-filter: blur(24px);", "box-shadow: 0 8px 20px rgba(0,0,0,0.06);");
            content = content.replace("backdrop-filter: blur(20px);", "box-shadow: 0 8px 20px rgba(0,0,0,0.06);");
            content = content.replace("box-shadow: 0 10px 40px rgba(0,0,0,0.5);", "box-shadow: 0 8px 30px rgba(0,0,0,0.15);");
            content = content.replace("rgba(255,255,255,0.08)", "#f1f5f9");
            content = content.replace("rgba(255,255,255,0.03)", "#f8fafc");

            // Add Header
            content = content.replace("<body>", "<body>\n" + headerHtml);
            
            // Add Footer
            if (content.contains("<script")) {
                content = content.replaceFirst("<script", footerHtml + "\n<script");
            } else {
                content = content.replace("</body>", footerHtml + "\n</body>");
            }
            
            // Add CSS
            content = content.replace("</style>", cssAppend + "\n</style>");
            
            Files.write(Paths.get(fi), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Updated " + fi);
        }
    }
}
