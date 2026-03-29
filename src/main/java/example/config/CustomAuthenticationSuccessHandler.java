package example.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        // 1. Lấy danh sách quyền (Roles) của người dùng vừa đăng nhập thành công
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // 2. Mặc định sau khi Login sẽ về trang chủ /home
        String redirectUrl = "/home";

        // 3. Kiểm tra Role để điều hướng sang trang quản trị nếu cần
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            
            // Nếu là ADMIN hoặc MANAGER -> Điều hướng vào Dashboard quản trị
            if (role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER")) {
                redirectUrl = "/admin/dashboard";
                break;
            }
        }

        // 4. Thực hiện lệnh chuyển hướng
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
