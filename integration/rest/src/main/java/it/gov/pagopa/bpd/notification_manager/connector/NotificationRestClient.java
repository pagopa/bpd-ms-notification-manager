package it.gov.pagopa.bpd.notification_manager.connector;

import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Notification Rest Client
 */
@FeignClient(name = "${rest-client.notification.serviceCode}", url = "${rest-client.notification.base-url}")
public interface NotificationRestClient {

    @PostMapping(value = "${rest-client.notification.notify.url}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    NotificationResource notify(@RequestBody @Valid NotificationDTO notificationDTO,
                                @RequestHeader("Ocp-Apim-Subscription-Key") String token);
}
