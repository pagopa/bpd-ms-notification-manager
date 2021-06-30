package it.gov.pagopa.bpd.notification_manager.connector.jdbc.config;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableAutoConfiguration(exclude = {JdbcRepositoriesAutoConfiguration.class})
@PropertySource("classpath:config/jdbcConfig.properties")
class JdbcConfig {

}
