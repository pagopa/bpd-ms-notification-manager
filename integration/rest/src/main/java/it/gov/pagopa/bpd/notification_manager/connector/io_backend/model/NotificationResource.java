package it.gov.pagopa.bpd.notification_manager.connector.io_backend.model;

import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestClient;
import lombok.Data;

/**
 * Resource model (output) for {@link NotificationRestClient}
 */
@Data
public class NotificationResource {
    private String id;
}
