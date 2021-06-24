package it.gov.pagopa.bpd.notification_manager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import lombok.Data;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

/**
 * Data Transfer Object (input) for {@link it.gov.pagopa.bpd.notification_manager.controller.BpdNotificationManagerController#restoreWinners(AwardWinnersRestoreDto)}
 */

@Data
public class AwardWinnersRestoreDto {

    @NotNull
    @JsonProperty(required = true)
    Long awardPeriodId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JsonProperty(required = true)
    private WinningCitizen.Status status;

    @JsonProperty
    private String chunkFilename;

}
