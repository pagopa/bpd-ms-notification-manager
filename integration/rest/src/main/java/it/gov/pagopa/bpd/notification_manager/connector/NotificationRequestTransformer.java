package it.gov.pagopa.bpd.notification_manager.connector;

import eu.sia.meda.connector.rest.model.RestConnectorRequest;
import eu.sia.meda.connector.rest.transformer.IRestRequestTransformer;
import eu.sia.meda.connector.rest.transformer.request.BaseSimpleRestRequestTransformer;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
class NotificationRequestTransformer
        extends BaseSimpleRestRequestTransformer
        implements IRestRequestTransformer<NotificationDTO, NotificationDTO> {

    @Override
    public RestConnectorRequest<NotificationDTO> transform(NotificationDTO om, Object... args) {
        RestConnectorRequest<NotificationDTO> out = new RestConnectorRequest<>();
        out.setMethod(HttpMethod.POST);
        out.setRequest(om);
        readArgs(out, args);

        return out;
    }
}
