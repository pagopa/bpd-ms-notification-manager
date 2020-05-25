package it.gov.pagopa.bpd.notification_manager.connector;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import it.gov.pagopa.bpd.common.connector.BaseFeignRestClientTest;
import it.gov.pagopa.bpd.notification_manager.connector.config.BpdNotificationManagerRestConnectorConfig;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import lombok.SneakyThrows;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@TestPropertySource(
        locations = "classpath:config/rest-client.properties",
        properties = "spring.application.name=bpd-ms-notification-manager-integration-rest")
@ContextConfiguration(initializers = NotificationRestClientTest.RandomPortInitializer.class,
        classes = BpdNotificationManagerRestConnectorConfig.class)
public class NotificationRestClientTest extends BaseFeignRestClientTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig()
            .dynamicPort()
            .usingFilesUnderClasspath("stubs/notification")
    );

    @Test
    public void notify_test() {
        final String token = "token";
        final NotificationDTO.Content content = new NotificationDTO.Content();
        content.setSubject("subject");
        content.setMarkdown("markdown");
        final NotificationDTO.NotificationMessage message = new NotificationDTO.NotificationMessage();
        message.setId("id");
        message.setFiscal_code("test");
        message.setCreated_at("2020-04-17T12:23:00.749+02:00");
        message.setSender_service_id("serviceId");
        message.setContent(content);
        final NotificationDTO.SenderMetadata senderMetadata = new NotificationDTO.SenderMetadata();
        senderMetadata.setService_name("serviceName");
        senderMetadata.setOrganization_name("organizationName");
        senderMetadata.setDepartment_name("departmentName");
        final NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setMessage(message);
        notificationDTO.setSender_metadata(senderMetadata);

        final NotificationResource actualResponse = restClient.notify(notificationDTO, token);

        assertNotNull(actualResponse);
        assertEquals("ok", actualResponse.getMessage());
    }

    @Autowired
    private NotificationRestClient restClient;

    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils
                    .addInlinedPropertiesToEnvironment(applicationContext,
                            String.format("rest-client.notification.base-url=http://%s:%d",
                                    wireMockRule.getOptions().bindAddress(),
                                    wireMockRule.port())
                    );
        }
    }
}