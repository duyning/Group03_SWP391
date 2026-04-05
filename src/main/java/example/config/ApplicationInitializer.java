package example.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class ApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        // GỘP CHUNG TẤT CẢ VÀO ĐÂY:
        // Giờ đây SecurityConfig đã có thể "nhìn thấy" AccountService từ WebMvcConfig
        return new Class<?>[] { HibernateConfig.class, WebMvcConfig.class, SecurityConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        // Trả về null vì chúng ta đã chuyển hết lên Root
        return null;
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    @Override
    protected jakarta.servlet.Filter[] getServletFilters() {
        org.springframework.web.filter.CharacterEncodingFilter encodingFilter =
                new org.springframework.web.filter.CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new jakarta.servlet.Filter[]{encodingFilter};
    }

    // HỖ TRỢ UPLOAD ẢNH
    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
                "",
                5242880,      // 5MB max cho 1 file
                10485760,     // 10MB max cho toàn bộ request
                0
        );
        registration.setMultipartConfig(multipartConfigElement);
    }
}