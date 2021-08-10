package it.gov.pagopa.bpd.notification_manager.controller;

import eu.sia.meda.core.controller.StatelessController;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestClient;
import it.gov.pagopa.bpd.notification_manager.model.AwardWinnersRestoreDto;
import it.gov.pagopa.bpd.notification_manager.model.SendWinnersDto;
import it.gov.pagopa.bpd.notification_manager.service.NotificationService;
import it.gov.pagopa.bpd.notification_manager.service.WinnersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
class BpdNotificationManagerControllerImpl extends StatelessController implements BpdNotificationManagerController {

    private final NotificationService notificationService;
    private final NotificationRestClient notificationRestClient;
    private final WinnersService winnersService;

    @Autowired
    BpdNotificationManagerControllerImpl(NotificationService notificationService,
                                         NotificationRestClient notificationRestClient,
                                         WinnersService winnersService) {
        this.notificationService = notificationService;
        this.notificationRestClient = notificationRestClient;
        this.winnersService = winnersService;
    }


    @Override
    public void sendWinners(SendWinnersDto request) {
        winnersService.sendWinners(request.getAwardPeriodId(), request.getLastChunkFilenameSent());
    }

    @Override
    public void updateWinners(Long awardPeriodId) {
        winnersService.updateWinners(awardPeriodId);
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

    @Deprecated
    @Override
    public void updateRanking() throws IOException {
        notificationService.updateRanking();
    }

    @Deprecated
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
    public void notifyEndPeriodOrGracePeriod() {
        notificationService.notifyEndPeriodOrEndGracePeriod();
    }

    @Override
    public void restoreWinners(AwardWinnersRestoreDto request) {
        if (log.isDebugEnabled()) {
            log.trace("BpdNotificationManagerControllerImpl.restoreWinners");
            log.debug("request = " + request);
        }
        winnersService.restoreWinners(request.getAwardPeriodId(), request.getStatus(), request.getChunkFilename());
    }

}
