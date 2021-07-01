package it.gov.pagopa.bpd.notification_manager.service;

import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;

import java.io.IOException;
import java.util.List;

public interface NotificationIOService {

    public int notifyWinnersPayments() throws IOException;

    public void notifyEndPeriodOrEndGracePeriod(AwardPeriod awardPeriod, Boolean isEndPeriod) throws IOException;
}
