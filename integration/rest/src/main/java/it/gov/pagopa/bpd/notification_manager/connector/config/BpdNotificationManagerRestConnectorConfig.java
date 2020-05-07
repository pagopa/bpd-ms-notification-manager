package it.gov.pagopa.bpd.notification_manager.connector.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/BpdNotificationManagerRestConnector.properties")
public class BpdNotificationManagerRestConnectorConfig {
}
