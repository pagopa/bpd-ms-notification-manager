package it.gov.pagopa.bpd.notification_manager.connector.award_period;

import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/**
 * AwardPeriod Rest Client
 */
@FeignClient(name = "${rest-client.award-period.serviceCode}", url = "${rest-client.award-period.base-url}")
public interface AwardPeriodRestClient {

    @GetMapping(value = "${rest-client.award-period.find-all.url}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    List<AwardPeriod> findAllAwardPeriods();
}

