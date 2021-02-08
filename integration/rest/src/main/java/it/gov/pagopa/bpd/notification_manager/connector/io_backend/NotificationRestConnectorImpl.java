package it.gov.pagopa.bpd.notification_manager.connector.io_backend;

import feign.FeignException;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.exception.NotifyTooManyRequestException;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.ProfileDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.ProfileResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

@Service
class NotificationRestConnectorImpl implements NotificationRestConnector {

    private final String subscriptionKey;
    private final NotificationRestClient notificationRestClient;

    public NotificationRestConnectorImpl(
            @Value("${rest-client.notification.backend-io.token-value}") String subscriptionKey,
            NotificationRestClient notificationRestClient) {
        this.subscriptionKey = subscriptionKey;
        this.notificationRestClient = notificationRestClient;
    }

    @Override
    public NotificationResource notify(@Valid NotificationDTO notificationDTO) {
        return notificationRestClient.notify(notificationDTO, subscriptionKey);
    }

    @Override
    public ProfileResource profiles(@Valid String fiscalCode) {
        ProfileDTO request = new ProfileDTO(fiscalCode);
        try{
            return notificationRestClient.profile(request, subscriptionKey);
        }catch(FeignException fex){
            if(fex.status()==404){
                return null;
            } else if(fex.status()==429) {
                throw new NotifyTooManyRequestException(HttpStatus.TOO_MANY_REQUESTS);
            }else{
                throw fex;
            }
        }
    }

}
