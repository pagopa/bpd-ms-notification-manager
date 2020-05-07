package it.gov.pagopa.bpd.notification_manager.connector;

import eu.sia.meda.connector.meda.MedaInternalConnector;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import org.springframework.stereotype.Service;

/**
 * Rest Connector for IO Back End Notify service
 */
@Service
class NotificationRestConnector
        extends MedaInternalConnector<NotificationDTO, NotificationResource, NotificationDTO, NotificationResource> {

}
