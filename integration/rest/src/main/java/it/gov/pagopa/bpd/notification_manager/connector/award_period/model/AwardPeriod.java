package it.gov.pagopa.bpd.notification_manager.connector.award_period.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Resource model (output)
 */
@Data
@EqualsAndHashCode(of = "awardPeriodId", callSuper = false)
public class AwardPeriod {

    private Long awardPeriodId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long minTransactionNumber;
    private Long maxAmount;
    private Long minPosition;
    private Long maxTransactionCashback;
    private Long maxPeriodCashback;
    private Long cashbackPercentage;
    private Long gracePeriod;
    private String status;
    private Long maxTransactionEvaluated;

}