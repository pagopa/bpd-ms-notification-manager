package it.gov.pagopa.bpd.notification_manager.connector.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class CitizenRankingId implements Serializable {

    private String fiscalCode;
    private Long awardPeriodId;

}
