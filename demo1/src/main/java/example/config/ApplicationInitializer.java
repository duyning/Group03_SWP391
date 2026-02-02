package example.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class ApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[] { HibernateConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] { WebMvcConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    // THÊM ĐOẠN NÀY ĐỂ HỖ TRỢ UPLOAD ẢNH
    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // Cấu hình vị trí tạm thời, kích thước tối đa của file (ví dụ: 5MB)
        // MultipartConfigElement(location, maxFileSize, maxRequestSize, fileSizeThreshold)
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
                "",
                5242880,      // 5MB max cho 1 file
                10485760,     // 10MB max cho toàn bộ request
                0
        );
        registration.setMultipartConfig(multipartConfigElement);
    }
}