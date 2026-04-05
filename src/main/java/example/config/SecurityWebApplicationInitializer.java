package example.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {
    // Để trống class này. Spring sẽ tự động tìm thấy và áp dụng DelegatingFilterProxy.
    // Việc bạn đã thêm SecurityConfig.class vào ApplicationInitializer là đủ.
}