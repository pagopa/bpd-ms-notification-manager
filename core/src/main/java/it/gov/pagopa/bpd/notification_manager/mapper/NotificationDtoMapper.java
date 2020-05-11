package it.gov.pagopa.bpd.notification_manager.mapper;

import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Mapper to create NotificationDTO
 */
@Service
public class NotificationDtoMapper {


    public NotificationDTO NotificationDtoMapper(String fiscalCode) {
        final NotificationDTO notification = new NotificationDTO();
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

        notification.setMessage(message);
        notification.setSender_metadata(senderMetadata);

        return notification;
    }
}
