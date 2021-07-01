package it.gov.pagopa.bpd.notification_manager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Data Transfer Object (input) for {@link it.gov.pagopa.bpd.notification_manager.controller.BpdNotificationManagerController#sendWinners(SendWinnersDto)}
 */

@Data
public class SendWinnersDto {

    @NotNull
    @JsonProperty(required = true)
    Long awardPeriodId;

    @JsonProperty
    private String lastChunkFilenameSent;

}
