package com.empresa.tickets.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration(proxyBeanMethods = false)
@EnableRetry
public class ResilienceConfig {
}
