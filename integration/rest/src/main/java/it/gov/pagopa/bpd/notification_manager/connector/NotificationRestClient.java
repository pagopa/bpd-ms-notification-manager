package it.gov.pagopa.bpd.notification_manager.connector;

import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;

/**
 * Notification Rest Client
 */
public interface NotificationRestClient {

    NotificationResource notify(NotificationDTO notificationDTO);

}
