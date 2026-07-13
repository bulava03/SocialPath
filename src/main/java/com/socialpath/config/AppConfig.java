package com.socialpath.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

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
}
