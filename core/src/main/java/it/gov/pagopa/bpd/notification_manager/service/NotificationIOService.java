package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;

import java.io.IOException;

public interface NotificationIOService {

    public int notifyWinnersPayments() throws IOException;

    public void notifyEndPeriodOrEndGracePeriod(AwardPeriod awardPeriod, Boolean isEndPeriod) throws IOException;
}
