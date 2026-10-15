package com.ceos24.mashup2backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

// 스케줄러는 운영에서만 돌린다
@Configuration
@EnableScheduling
@Profile("prod")
public class SchedulingConfig {
}
