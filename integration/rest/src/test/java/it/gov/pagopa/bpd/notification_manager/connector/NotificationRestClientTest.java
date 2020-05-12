package it.gov.pagopa.bpd.notification_manager.connector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import eu.sia.meda.DummyConfiguration;
import it.gov.pagopa.bpd.notification_manager.connector.config.BpdNotificationManagerRestConnectorConfig;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)

@ContextConfiguration(initializers = NotificationRestClientTest.RandomPortInitializer.class)
@TestPropertySource(
        locations = "classpath:config/rest-client.properties",
        properties = "spring.application.name=bpd-ms-notification-manager-integration-rest")
@Import({DummyConfiguration.class, BpdNotificationManagerRestConnectorConfig.class})
public class NotificationRestClientTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(
            wireMockConfig().dynamicPort()
    );
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private NotificationRestClient restClient;

    @Test
    public void notify_test() throws JsonProcessingException {
        final NotificationResource expectedResponse = new NotificationResource();
        expectedResponse.setMessage("ok");
        final String fiscalCode = "test";
        final String TOKEN = "token";
        final NotificationDTO notificationDTO = new NotificationDTO();
        final NotificationDTO.NotificationMessage message = new NotificationDTO.NotificationMessage();
        final NotificationDTO.SenderMetadata senderMetadata = new NotificationDTO.SenderMetadata();
        final NotificationDTO.Content content = new NotificationDTO.Content();
        content.setSubject("subject");
        content.setMarkdown("markdown");
        Instant nowUtc = Instant.now();
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        ZonedDateTime nowDateTime = ZonedDateTime.ofInstant(nowUtc, zoneId);
        message.setId("id");
        message.setFiscal_code(fiscalCode);
        message.setCreated_at(nowDateTime.toString());
        message.setContent(content);
        message.setSender_service_id("serviceId");
        senderMetadata.setService_name("serviceName");
        senderMetadata.setOrganization_name("organizationName");
        senderMetadata.setDepartment_name("departmentName");
        notificationDTO.setMessage(message);
        notificationDTO.setSender_metadata(senderMetadata);
        stubFor(post(urlEqualTo("/notify?TOKEN=token"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .withBody(objectMapper.writeValueAsString(expectedResponse))));

        final NotificationResource actualResponse = restClient.notify(notificationDTO, TOKEN);
        assertEquals(expectedResponse, actualResponse);
    }

    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Autowired
        private Environment env;

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils
                    .addInlinedPropertiesToEnvironment(applicationContext,
                            "rest-client.notification.base-url=" + "http://localhost:" + wireMockRule.port()
                    );
        }
    }

    @Configuration
    @ImportAutoConfiguration(FeignAutoConfiguration.class)
    static class ContextConfiguration {
    }
}