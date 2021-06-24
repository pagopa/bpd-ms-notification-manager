package it.gov.pagopa.bpd.notification_manager.connector.jdbc.config;


import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

//@EnableJpaRepositories(
//        excludeFilters = @ComponentScan.Filter(Repository.class)
//)
@Configuration
@PropertySource("classpath:config/jdbcConfig.properties")
class JdbcConfig {

//    @Bean("citizenJdbcTemplate")
////    @Primary
//    public JdbcTemplate citizenJdbcTemplate() {
//        return new JdbcTemplate(citizenJdbcDataSource());
//    }
//
//    @Bean("citizenJdbcDataSource")
////    @Primary
//    @ConfigurationProperties(prefix = "citizen.spring.datasource.hikari")
//    public DataSource citizenJdbcDataSource() {
//        return citizenJdbcDataSourceProperties().initializeDataSourceBuilder().build();
//    }
//
//    @Bean("citizenJdbcDataSourceProperties")
////    @Primary
//    @ConfigurationProperties("citizen.spring.datasource")
//    public DataSourceProperties citizenJdbcDataSourceProperties() {
//        return new DataSourceProperties();
//    }

}
