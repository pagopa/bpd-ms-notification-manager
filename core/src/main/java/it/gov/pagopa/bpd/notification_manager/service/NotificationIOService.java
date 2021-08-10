package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;

import java.io.IOException;

public interface NotificationIOService {

    int notifyWinnersPayments() throws IOException;

    void notifyEndPeriodOrEndGracePeriod(AwardPeriod awardPeriod, Boolean isEndPeriod);
}
