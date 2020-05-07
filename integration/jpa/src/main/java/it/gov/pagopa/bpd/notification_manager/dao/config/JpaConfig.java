package it.gov.pagopa.bpd.notification_manager.dao.config;


import eu.sia.meda.connector.jpa.config.JPAConnectorConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@ConditionalOnMissingBean(name = "JPADataSource")
@Configuration
@PropertySource("classpath:config/JpaConnectionConfig.properties")


public class JpaConfig extends JPAConnectorConfig {
}
