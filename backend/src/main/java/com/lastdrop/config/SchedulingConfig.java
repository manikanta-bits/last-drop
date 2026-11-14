package com.lastdrop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Switches on @Scheduled methods (the auction timer in the scheduler package). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
