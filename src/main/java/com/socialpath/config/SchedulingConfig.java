package com.socialpath.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled methods (the video conversion worker). Spring runs them
 * on a single scheduler thread, so at most one conversion runs per instance.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
