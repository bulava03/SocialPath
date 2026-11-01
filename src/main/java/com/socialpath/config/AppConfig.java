package com.socialpath.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
public class AppConfig {

    /**
     * Lives here rather than in SecurityConfig on purpose: UserServiceImpl
     * depends on this bean, and SecurityConfig itself depends (through the
     * JWT filter and UserDetailsService) on UserServiceImpl. Keeping the
     * encoder in a neutral configuration class avoids that circular chain.
     * @return the BCrypt encoder used for all password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * The time for expiring phone codes; a bean so tests can move it.
     * @return the system clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
