package it.gov.pagopa.bpd.notification_manager.controller;

import io.swagger.annotations.Api;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

/**
 * Controller to expose MicroService
 */
@Api(tags = "Bonus Pagamenti Digitali notification-manager Controller")
@RequestMapping("/bpd/notification-manager")
public interface BpdNotificationManagerController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void update() throws IOException;
}
