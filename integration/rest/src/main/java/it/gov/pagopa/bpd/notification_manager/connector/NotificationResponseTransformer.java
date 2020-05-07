package it.gov.pagopa.bpd.notification_manager.connector;

import eu.sia.meda.connector.rest.transformer.response.SimpleRest2xxResponseTransformer;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import org.springframework.stereotype.Service;

@Service
class NotificationResponseTransformer
        extends SimpleRest2xxResponseTransformer<NotificationResource> {
}
