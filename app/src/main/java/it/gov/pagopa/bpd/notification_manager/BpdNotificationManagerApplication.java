package it.gov.pagopa.bpd.notification_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class, SessionAutoConfiguration.class})
@ComponentScan(basePackages = {"eu.sia.meda", "it.gov.pagopa.bpd"})
public class BpdNotificationManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BpdNotificationManagerApplication.class, args);
	}

}
