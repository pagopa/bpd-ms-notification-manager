package it.gov.pagopa.bpd.notification_manager.connector.io_backend;

import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

@Service
class NotificationRestConnectorImpl implements NotificationRestConnector {

    private final String subscriptionKey;
    private final NotificationRestClient notificationRestClient;

    public NotificationRestConnectorImpl(
            @Value("rest-client.notification.notify.token-value") String subscriptionKey,
            NotificationRestClient notificationRestClient) {
        this.subscriptionKey = subscriptionKey;
        this.notificationRestClient = notificationRestClient;
    }

    @Override
    public NotificationResource notify(@Valid NotificationDTO notificationDTO) {
        return notificationRestClient.notify(notificationDTO, subscriptionKey);
    }

}
