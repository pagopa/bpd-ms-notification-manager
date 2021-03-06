package it.gov.pagopa.bpd.notification_manager.controller;

import eu.sia.meda.core.controller.StatelessController;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
class BpdNotificationManagerControllerImpl extends StatelessController implements BpdNotificationManagerController {

    private final NotificationService notificationService;
    private final NotificationRestClient notificationRestClient;

    @Autowired
    BpdNotificationManagerControllerImpl(NotificationService notificationService, NotificationRestClient notificationRestClient) {
        this.notificationService = notificationService;
        this.notificationRestClient = notificationRestClient;
    }

    @Override
    public void sendWinners(Long awardPeriodId) throws IOException {
        notificationService.sendWinners(awardPeriodId);
    }

    @Override
    public void updateWinners(Long awardPeriodId) throws IOException {
        notificationService.updateWinners(awardPeriodId);
    }

    @Override
    public void notifyUnset() {
        notificationService.notifyUnsetPayoffInstr();
    }

    @Override
    public void testConnection() throws IOException {
        notificationService.testConnection();
    }

    @Override
    public void notifyAwardWinnerPayments() throws IOException {
        notificationService.notifyWinnersPayments();
    }
}
