package example.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // Redirect back to admin dashboard with an error message
        request.getSession().setAttribute("accessDeniedMessage", "Bạn không có thẩm quyền truy cập trang này.");
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
    }
}
