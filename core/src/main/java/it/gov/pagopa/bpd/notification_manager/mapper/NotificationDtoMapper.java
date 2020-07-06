package it.gov.pagopa.bpd.notification_manager.mapper;

import it.gov.pagopa.bpd.notification_manager.connector.model.ContentPaymentData;
import it.gov.pagopa.bpd.notification_manager.connector.model.MessageContent;
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


    public NotificationDTO NotificationDtoMapper(
            String fiscalCode, Long timeToLive, String subject, String markdown) {

        final NotificationDTO notification = new NotificationDTO();
        notification.setFiscalCode(fiscalCode);
        notification.setTimeToLive(timeToLive);

        MessageContent messageContent = new MessageContent();
        messageContent.setSubject(subject);
        messageContent.setMarkdown(markdown);

        notification.setContent(messageContent);

        return notification;
    }
}
