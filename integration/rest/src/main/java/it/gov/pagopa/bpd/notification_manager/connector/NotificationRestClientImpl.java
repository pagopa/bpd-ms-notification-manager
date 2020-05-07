package it.gov.pagopa.bpd.notification_manager.connector;

import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

public class NotificationRestClientImpl implements NotificationRestClient {

    static final String TOKEN = "token";

    private final NotificationRestConnector restConnector;
    private final NotificationRequestTransformer requestTransformer;
    private final NotificationResponseTransformer responseTransformer;

    @Autowired
    public NotificationRestClientImpl(NotificationRestConnector restConnector,
                                      NotificationRequestTransformer requestTransformer,
                                      NotificationResponseTransformer responseTransformer) {
        this.restConnector = restConnector;
        this.requestTransformer = requestTransformer;
        this.responseTransformer = responseTransformer;
    }

    @Override
    public NotificationResource notify(NotificationDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        final HashMap<String, Object> queryParams = new HashMap<>();
        //TODO risolvere problema token
        queryParams.put(TOKEN, "token");

        return restConnector.call(request, requestTransformer, responseTransformer, null, queryParams);
    }
}
