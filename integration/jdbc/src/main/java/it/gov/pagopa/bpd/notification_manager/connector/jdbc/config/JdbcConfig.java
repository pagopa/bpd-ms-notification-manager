package it.gov.pagopa.bpd.notification_manager.connector.jdbc.config;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

//@EnableJpaRepositories(
//        excludeFilters = @ComponentScan.Filter(Repository.class)
//)
@Configuration
@EnableAutoConfiguration(exclude = {JdbcRepositoriesAutoConfiguration.class})
@PropertySource("classpath:config/jdbcConfig.properties")
class JdbcConfig {

    //    @Bean
//    @Primary
//    public JdbcTemplate citizenJdbcTemplate(DataSource dataSource) {
//        return new JdbcTemplate(dataSource);
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
