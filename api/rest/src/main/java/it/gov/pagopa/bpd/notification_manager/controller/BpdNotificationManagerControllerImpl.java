package it.gov.pagopa.bpd.notification_manager.controller;

import eu.sia.meda.core.controller.StatelessController;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

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

    @Override
    public void updateRanking() throws IOException {
        notificationService.updateRanking();
    }

    @Override
    public void updateRankingMilestone() throws IOException {
        log.trace("Controller - execute updateRankingMilestone");
        notificationService.updateRankingMilestone();
    }

    @Override
    public void updateBonificaRecesso() throws IOException {
        log.trace("Controller - execute updateBonificaRecesso");
        notificationService.updateBonificaRecesso();
    }

    @Override
    public void notifyEndPeriodOrGracePeriod() throws IOException {
        notificationService.notifyEndPeriodOrEndGracePeriod();
    }
}
