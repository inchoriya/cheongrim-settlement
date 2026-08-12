package com.settlehub.config;

import com.settlehub.auth.jwt.JwtProperties;
import com.settlehub.auth.security.JwtAuthenticationFilter;
import com.settlehub.auth.security.RestAccessDeniedHandler;
import com.settlehub.auth.security.RestAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthEntryPoint restAuthEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    /**
     * 운영자용 정산서 인쇄 화면(Thymeleaf SSR) 전용 체인.
     *
     * <p>고객용 SPA는 stateless JWT를 쓰지만, 브라우저가 주소창으로 여는 인쇄 화면은
     * Authorization 헤더를 실을 수 없다. 그래서 이 경로만 폼 로그인 + 세션으로 분리하고
     * CSRF 보호를 켠 채로 둔다. API 체인은 아래에서 기존 방식 그대로 유지된다.
     */
    @Bean
    @Order(1)
    SecurityFilterChain printSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/print/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/print/login").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/print/login")
                        .loginProcessingUrl("/print/login")
                        .defaultSuccessUrl("/print/settlements", true)
                        .failureUrl("/print/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/print/logout")
                        .logoutSuccessUrl("/print/login?logout")
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/actuator/health",
                            "/api/v1/auth/login"
                    ).permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/register").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/**").authenticated();
                    if (h2ConsoleEnabled) {
                        // local 프로필에서만 콘솔이 켜짐
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.anyRequest().denyAll();
                })
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
