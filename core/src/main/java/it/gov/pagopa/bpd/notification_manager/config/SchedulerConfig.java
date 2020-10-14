package it.gov.pagopa.bpd.notification_manager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@PropertySource("classpath:config/notificationService.properties")
@EnableScheduling
public class SchedulerConfig {
}
