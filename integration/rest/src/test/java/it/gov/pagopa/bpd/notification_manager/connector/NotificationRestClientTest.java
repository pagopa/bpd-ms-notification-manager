package it.gov.pagopa.bpd.notification_manager.connector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.bpd.common.connector.BaseFeignRestClientTest;
import it.gov.pagopa.bpd.notification_manager.connector.config.BpdNotificationManagerRestConnectorConfig;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;


@TestPropertySource(
        locations = "classpath:config/rest-client.properties",
        properties = "spring.application.name=bpd-ms-notification-manager-integration-rest")
@Import({BpdNotificationManagerRestConnectorConfig.class})
public class NotificationRestClientTest extends BaseFeignRestClientTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private NotificationRestClient restClient;

    static {
        SERIVICE_PORT_ENV_VAR_NAME = "URL_BACKEND_IO_PORT";
    }

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
}