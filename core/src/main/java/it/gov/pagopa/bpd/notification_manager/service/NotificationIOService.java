package it.gov.pagopa.bpd.notification_manager.service;

import java.io.IOException;

public interface NotificationIOService {

    public int notifyWinnersPayments() throws IOException;
}
