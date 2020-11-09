package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import io.micrometer.core.instrument.util.TimeUtils;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * @see NotificationService
 */
@Service
@Slf4j
class NotificationServiceImpl extends BaseService implements NotificationService {

    private final CitizenDAO citizenDAO;
    //    private final AwardWinnerDAO awardWinnerDAO;
    private final NotificationDtoMapper notificationDtoMapper;
    private final NotificationRestConnector notificationRestConnector;
    private final AwardPeriodRestClient awardPeriodRestClient;
    private final String outputFilePath;
    private final Long timeToLive;
    private final String subject;
    private final String markdown;
    private static String DELIMITER = ";";
    private final int maxSize = 3;

    @Autowired
    NotificationServiceImpl(
            CitizenDAO citizenDAO, NotificationDtoMapper notificationDtoMapper,
            NotificationRestConnector notificationRestConnector,
            AwardPeriodRestClient awardPeriodRestClient, @Value("${core.NotificationService.updateRankingAndFindWinners.outputFilePath}") String outputFilePath,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.ttl}") Long timeToLive,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.subject}") String subject,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.markdown}") String markdown) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestConnector = notificationRestConnector;
        this.awardPeriodRestClient = awardPeriodRestClient;
        this.outputFilePath = outputFilePath;
        this.timeToLive = timeToLive;
        this.subject = subject;
        this.markdown = markdown;
    }


    @Override
    @Scheduled(cron = "${core.NotificationService.notifyUnsetPayoffInstr.scheduler}")
    public void notifyUnsetPayoffInstr() {
        try {
            List<String> citizensFC = citizenDAO.findFiscalCodesWithUnsetPayoffInstr();
            for (String citizenCf : citizensFC) {
                NotificationDTO dto = notificationDtoMapper.NotificationDtoMapper(
                        citizenCf, timeToLive, subject, markdown);
                NotificationResource resource = notificationRestConnector.notify(dto);
                if (logger.isDebugEnabled()) {
                    logger.debug("Notified citizen, notification id: " + resource.getId());
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.updateRankingAndFindWinners.scheduler}")
    public void updateRankingAndWinners() {
        citizenDAO.updateRankingAndWinners();
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.updateRankingAndFindWinners.scheduler}")
    public void findWinners() {
        try {
            //TODO rimuovere commento
//            List<AwardPeriod> activePeriods = awardPeriodRestClient.findActiveAwardPeriods();
            //TODO rimuovere blocco --->
            List<AwardPeriod> activePeriods = new ArrayList<AwardPeriod>();
            AwardPeriod awp1 = new AwardPeriod();
            awp1.setAwardPeriodId(2L);
            awp1.setEndDate(LocalDate.now().minus(Period.ofDays(15)));
            awp1.setGracePeriod(15L);
            awp1.setStartDate(LocalDate.now().minus(Period.ofDays(50)));
            activePeriods.add(awp1);
            AwardPeriod awp2 = new AwardPeriod();
            awp2.setAwardPeriodId(1L);
            awp2.setEndDate(LocalDate.now().minus(Period.ofDays(5)));
            awp2.setGracePeriod(15L);
            awp2.setStartDate(LocalDate.now().minus(Period.ofDays(40)));
            activePeriods.add(awp2);
            AwardPeriod awp3 = new AwardPeriod();
            awp3.setAwardPeriodId(3L);
            awp3.setEndDate(LocalDate.now().plus(Period.ofDays(5)));
            awp3.setGracePeriod(15L);
            awp3.setStartDate(LocalDate.now().minus(Period.ofDays(15)));
            activePeriods.add(awp3);
            // <---
            Long endingPeriodId = null;
            for (AwardPeriod activePeriod : activePeriods) {
                if (activePeriod.getEndDate().plus(Period.ofDays(activePeriod.getGracePeriod().intValue()))
                        .equals(LocalDate.now())) {
                    endingPeriodId = activePeriod.getAwardPeriodId();
                }
            }
            if (endingPeriodId != null) {
                List<WinningCitizen> winners = citizenDAO.findWinners(endingPeriodId);
                if (!winners.isEmpty()) {
                    List<WinningCitizen> winnersForCSV = new ArrayList<>();
                    for (WinningCitizen winningCitizen : winners)
                        if (winningCitizen.getPayoffInstr() != null && !winningCitizen.getPayoffInstr().isEmpty())
                            winnersForCSV.add(winningCitizen);

                    List<String> dataLines = new ArrayList<>();
                    int m = 0;
                    int n = 0;
                    String fileName = "testCSV_";
                    for (WinningCitizen winnerForCSV : winnersForCSV) {
                        n++;
                        String paymentReason = (winnerForCSV.getAccountHolderFiscalCode().equals(winnerForCSV.getFiscalCode())) ?
                                (winnerForCSV.getId().toString() +
                                        " - Cashback di Stato - dal "
                                        + winnerForCSV.getAwardPeriodStart().toString() +
                                        " al " + winnerForCSV.getAwardPeriodEnd().toString()) :
                                (winnerForCSV.getId().toString() +
                                        " - Cashback di Stato - dal " +
                                        winnerForCSV.getAwardPeriodStart().toString() +
                                        " al " + winnerForCSV.getAwardPeriodEnd().toString() +
                                        " - " + winnerForCSV.getFiscalCode());
//                    TODO definire accountHolder
                        String accountHolder = "";
                        String sb = winnerForCSV.getId().toString() + DELIMITER +
                                winnerForCSV.getAccountHolderFiscalCode() + DELIMITER +
                                winnerForCSV.getPayoffInstr() + DELIMITER +
                                winnerForCSV.getAccountHolderName() + DELIMITER +
                                winnerForCSV.getAccountHolderSurname() + DELIMITER +
                                winnerForCSV.getAmount().toString() + DELIMITER +
                                winnerForCSV.getCashback().toString() + DELIMITER +
                                winnerForCSV.getJackpot().toString() + DELIMITER +
                                paymentReason + DELIMITER +
                                winnerForCSV.getTypology() + DELIMITER +
                                winnerForCSV.getAwardPeriodId().toString() + DELIMITER +
                                winnerForCSV.getAwardPeriodStart().toString() + DELIMITER +
                                winnerForCSV.getAwardPeriodEnd().toString() + DELIMITER +
                                winnerForCSV.getCheckInstrStatus() + DELIMITER +
                                winnerForCSV.getAccountHolderFiscalCode();
                        dataLines.add(sb);
                        if (dataLines.size() == maxSize || n == winnersForCSV.size()) {
                            m++;
                            File csvOutputFile = new File(fileName + String.valueOf(m)
                                    + "_" + String.valueOf((int) Math.ceil((double) winnersForCSV.size() / maxSize))
                                    + ".csv");
                            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                                dataLines.forEach(pw::println);
                            }
                            dataLines.clear();
                        }
                    }
                }
            }
            //TODO: Send results through SFTP channel
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}

