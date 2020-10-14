package it.gov.pagopa.bpd.notification_manager.connector.jpa.config;


import it.gov.pagopa.bpd.common.connector.jpa.config.BaseJpaConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/JpaConnectionConfig.properties")
public class JpaConfig extends BaseJpaConfig {
}
