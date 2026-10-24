package com.socialpath.config;

import com.socialpath.security.JwtAuthenticationEntryPoint;
import com.socialpath.security.JwtTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtTokenFilter jwtTokenFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /**
     * Keeps the JWT filter out of the plain servlet filter chain. Spring Boot
     * registers every Filter bean there automatically, so without this the
     * filter would run twice per request - once there and once inside the
     * security chain below - and load the user from the database twice.
     * @param filter the JWT filter bean
     * @return a disabled registration for it
     */
    @Bean
    public FilterRegistrationBean<JwtTokenFilter> jwtTokenFilterRegistration(JwtTokenFilter filter) {
        FilterRegistrationBean<JwtTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF filter is disabled deliberately: the session token lives in an
        // HttpOnly SameSite=Strict cookie (see AuthController), so the browser
        // never attaches it to cross-site requests, which is what the CSRF
        // token would otherwise protect against.
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/auth/login",
                                "/registration",
                                "/registration/registration",
                                "/pictures/**",
                                "/script/**",
                                "/style/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
