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
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/home"),
                                new AntPathRequestMatcher("/movie/detail/**"),
                                new AntPathRequestMatcher("/register"),
                                new AntPathRequestMatcher("/login"),
                                new AntPathRequestMatcher("/forgot-password"),   // <-- THÊM MỚI
                                new AntPathRequestMatcher("/reset-password"),    // <-- THÊM MỚI
                                new AntPathRequestMatcher("/vnpay/**"),          // <-- THÊM MỚI CHO VNPAY
                                new AntPathRequestMatcher("/api/booking/showtimes"), // <-- Cho phép xem lịch chiếu không cần đăng nhập
                                new AntPathRequestMatcher("/api/booking/movies-with-showtimes"), // <-- API mới
                                new AntPathRequestMatcher("/api/search/**"),     // <-- Cho phép tìm kiếm phim
                                new AntPathRequestMatcher("/lich-chieu"),        // <-- Trang lịch chiếu
                                new AntPathRequestMatcher("/resources/**"),
                                new AntPathRequestMatcher("/assets/**"),
                                new AntPathRequestMatcher("/uploads/**"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**")
                        ).permitAll()

                        // Blog: tạo bài, sửa bài, comment, like — Cần đăng nhập
                        .requestMatchers(
                                new AntPathRequestMatcher("/blog/create"),
                                new AntPathRequestMatcher("/blog/*/edit"),
                                new AntPathRequestMatcher("/blog/comment/*/edit"),
                                new AntPathRequestMatcher("/blog/*/comment"),
                                new AntPathRequestMatcher("/blog/*/like"),
                                new AntPathRequestMatcher("/blog/*/delete")
                        ).authenticated()

                        // === Dashboard: cả ADMIN và MANAGER được vào ===
                        .requestMatchers(
                                new AntPathRequestMatcher("/admin/dashboard"))
                        .hasAnyRole("ADMIN", "MANAGER")

                        // === ADMIN-only pages (MANAGER không được vào) ===
                        .requestMatchers(
                                new AntPathRequestMatcher("/admin/accounts"),
                                new AntPathRequestMatcher("/admin/accounts/**"),
                                new AntPathRequestMatcher("/admin/create-manager"))
                        .hasRole("ADMIN")

                        // === MANAGER-only pages ===
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

                        .requestMatchers(new AntPathRequestMatcher("/manager/**"))
                        .hasRole("MANAGER")

                        .requestMatchers(
                                new AntPathRequestMatcher("/profile/**"),
                                new AntPathRequestMatcher("/edit-profile/**"),
                                new AntPathRequestMatcher("/change-password/**"))
                        .authenticated()

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