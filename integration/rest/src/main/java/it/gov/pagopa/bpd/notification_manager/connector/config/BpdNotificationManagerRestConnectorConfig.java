package it.gov.pagopa.bpd.notification_manager.connector.config;

import it.gov.pagopa.bpd.common.connector.config.RestConnectorConfig;
import it.gov.pagopa.bpd.notification_manager.connector.NotificationRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(RestConnectorConfig.class)
@EnableFeignClients(clients = NotificationRestClient.class)
@PropertySource("classpath:config/rest-client.properties")
public class BpdNotificationManagerRestConnectorConfig {
}
