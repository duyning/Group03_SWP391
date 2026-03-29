package example.config;

import example.service.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            AccountService accountService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(accountService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CustomAuthenticationSuccessHandler successHandler,
            CustomAuthenticationFailureHandler failureHandler,
            CustomAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. CÁC ĐƯỜNG DẪN CÔNG KHAI (Mọi người đều vào được)
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/home"),
                                new AntPathRequestMatcher("/movie/detail/**"),
                                new AntPathRequestMatcher("/register"),
                                new AntPathRequestMatcher("/login"),
                                new AntPathRequestMatcher("/forgot-password"),
                                new AntPathRequestMatcher("/reset-password"),
                                new AntPathRequestMatcher("/blog"),
                                new AntPathRequestMatcher("/blog/{id}"),
                                new AntPathRequestMatcher("/vnpay/**"),
                                new AntPathRequestMatcher("/api/booking/showtimes"),
                                new AntPathRequestMatcher("/resources/**"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**")
                        ).permitAll()

                        // 2. CÁC TÍNH NĂNG BLOG (Yêu cầu phải Đăng nhập)
                        .requestMatchers(
                            new AntPathRequestMatcher("/blog/create"),
                            new AntPathRequestMatcher("/blog/*/edit"),
                            new AntPathRequestMatcher("/blog/comment/*/edit"),
                            new AntPathRequestMatcher("/blog/*/comment"),
                            new AntPathRequestMatcher("/blog/*/like"),
                            new AntPathRequestMatcher("/blog/*/delete")
                        ).authenticated()

                        // 3. TRANG DASHBOARD CHUNG (Cả Admin và Manager đều vào được)
                        .requestMatchers(
                                new AntPathRequestMatcher("/admin/dashboard"))
                        .hasAnyRole("ADMIN", "MANAGER")

                        // 4. CÁC TRANG DÀNH RIÊNG CHO ADMIN (Quản lý tài khoản)
                        .requestMatchers(
                                new AntPathRequestMatcher("/admin/accounts"),
                                new AntPathRequestMatcher("/admin/accounts/**"),
                                new AntPathRequestMatcher("/admin/create-manager"))
                        .hasRole("ADMIN")

                        // 5. CÁC TRANG DÀNH RIÊNG CHO MANAGER (Quản lý nghiệp vụ rạp phim)
                        .requestMatchers(
                                new AntPathRequestMatcher("/admin/blogs"),
                                new AntPathRequestMatcher("/admin/blogs/**"),
                                new AntPathRequestMatcher("/admin/manager_movie"),
                                new AntPathRequestMatcher("/admin/movie/**"),
                                new AntPathRequestMatcher("/admin/manager_schedule"),
                                new AntPathRequestMatcher("/admin/manager_ticket_price"),
                                new AntPathRequestMatcher("/admin/ticket/**"),
                                new AntPathRequestMatcher("/admin/manager_combo"),
                                new AntPathRequestMatcher("/admin/combo/**"),
                                new AntPathRequestMatcher("/admin/manager_cinema"),
                                new AntPathRequestMatcher("/admin/cinema/**"),
                                new AntPathRequestMatcher("/admin/manager_room"),
                                new AntPathRequestMatcher("/admin/room/**"),
                                new AntPathRequestMatcher("/admin/manager_seat"),
                                new AntPathRequestMatcher("/admin/seat/**"),
                                new AntPathRequestMatcher("/admin/manager_showtime"),
                                new AntPathRequestMatcher("/admin/showtime/**"),
                                new AntPathRequestMatcher("/admin/manager_news"),
                                new AntPathRequestMatcher("/admin/news/**"),
                                new AntPathRequestMatcher("/admin/manager_vouchers"),
                                new AntPathRequestMatcher("/admin/vouchers/**"))
                        .hasRole("MANAGER")

                        // Các route bắt đầu bằng /manager/ cũng dành cho MANAGER
                        .requestMatchers(new AntPathRequestMatcher("/manager/**"))
                        .hasRole("MANAGER")

                        // 6. CÁC TRANG CÁ NHÂN (Yêu cầu Đăng nhập bất kỳ role nào)
                        .requestMatchers(
                                new AntPathRequestMatcher("/profile/**"),
                                new AntPathRequestMatcher("/edit-profile/**"),
                                new AntPathRequestMatcher("/change-password/**"))
                        .authenticated()

                        // Mọi yêu cầu khác chưa cấu hình đều phải Đăng nhập
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/home")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}