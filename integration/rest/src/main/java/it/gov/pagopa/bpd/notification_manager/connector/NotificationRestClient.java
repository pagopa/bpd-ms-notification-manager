package it.gov.pagopa.bpd.notification_manager.connector;

import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.model.NotificationResource;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;

/**
 * Notification Rest Client
 */
@FeignClient(name = "${rest-client.notification.serviceCode}", url = "${rest-client.notification.base-url}")
public interface NotificationRestClient {

    @PostMapping(value = "${rest-client.notification.notify.url}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    NotificationResource notify(@RequestBody @Valid NotificationDTO notificationDTO,
                                @RequestParam String TOKEN);
}
