package it.gov.pagopa.bpd.notification_manager.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to expose MicroService
 */
@Api(tags = "Bonus Pagamenti Digitali notification-manager Controller")
@RequestMapping("/bpd/notification-manager")
public interface BpdNotificationManagerController {

//    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    @ResponseStatus(HttpStatus.OK)
//    List<String> find();
}
