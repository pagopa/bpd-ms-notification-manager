package it.gov.pagopa.bpd.notification_manager.connector.io_backend;

import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.ProfileDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.ProfileResource;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

public interface NotificationRestConnector {

    NotificationResource notify(@RequestBody @Valid NotificationDTO notificationDTO);

    ProfileResource profiles(@Valid String fiscalCode);

}
