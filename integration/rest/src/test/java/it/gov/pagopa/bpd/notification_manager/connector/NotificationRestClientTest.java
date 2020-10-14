package it.gov.pagopa.bpd.notification_manager.connector;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import it.gov.pagopa.bpd.common.connector.BaseFeignRestClientTest;
import it.gov.pagopa.bpd.notification_manager.connector.config.BpdNotificationManagerRestConnectorConfig;
import it.gov.pagopa.bpd.notification_manager.connector.model.MessageContent;
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
        classes = {NotificationRestConnectorImpl.class, BpdNotificationManagerRestConnectorConfig.class})
public class NotificationRestClientTest extends BaseFeignRestClientTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig()
            .dynamicPort()
            .usingFilesUnderClasspath("stubs/notification")
    );

    @Test
    public void notify_test() {
        final NotificationDTO notification = new NotificationDTO();
        notification.setFiscalCode("test");
        notification.setTimeToLive(3600L);

        MessageContent messageContent = new MessageContent();
        messageContent.setSubject("subject");
        messageContent.setMarkdown("markdown");

        notification.setContent(messageContent);

        final NotificationResource actualResponse = restConnector.notify(notification);

        assertNotNull(actualResponse);
        assertEquals("ok", actualResponse.getId());
    }

    @Autowired
    private NotificationRestClient restClient;

    @Autowired
    private NotificationRestConnector restConnector;

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