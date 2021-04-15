package it.gov.pagopa.bpd.notification_manager.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Controller to expose MicroService
 */
@Api(tags = "Bonus Pagamenti Digitali notification-manager Controller")
@RequestMapping("/bpd/notification-manager")
public interface BpdNotificationManagerController {

    @PostMapping(value = "/winners/consap", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void sendWinners(
            @ApiParam(required = true)
            @RequestParam("awardPeriodId") Long awardPeriodId
    ) throws IOException;

    @PostMapping(value = "/winners", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void updateWinners(
            @ApiParam(required = true)
            @RequestParam("awardPeriodId") Long awardPeriodId
    ) throws IOException;

    @GetMapping(value = "/notify", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void notifyUnset();

    @PostMapping(value = "/test", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void testConnection() throws IOException;

    @PostMapping(value = "/notifyPayments", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void notifyAwardWinnerPayments() throws IOException;

    @GetMapping(value = "/updateRanking", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void updateRanking() throws IOException;

    @GetMapping(value = "/updateRankingMilestone", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void updateRankingMilestone() throws IOException;

    @GetMapping(value = "/updateBonificaRecesso", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void updateBonificaRecesso() throws IOException;

}
