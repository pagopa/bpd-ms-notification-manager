package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import it.gov.pagopa.bpd.notification_manager.connector.WinnersSftpConnector;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.NotificationRestConnector;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationDTO;
import it.gov.pagopa.bpd.notification_manager.connector.io_backend.model.NotificationResource;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @see NotificationService
 */
@Service
@Slf4j
class NotificationServiceImpl extends BaseService implements NotificationService {

    private final CitizenDAO citizenDAO;
    private final NotificationDtoMapper notificationDtoMapper;
    private final NotificationRestConnector notificationRestConnector;
    private final AwardPeriodRestClient awardPeriodRestClient;
    private final WinnersSftpConnector winnersSftpConnector;
    private final String outputFilePath;
    private final Long timeToLive;
    private final String subject;
    private final String markdown;
    private final String DELIMITER;
    private final Long maxRow;
    private final String serviceName;
    private final String fileType;

    @Autowired
    NotificationServiceImpl(
            CitizenDAO citizenDAO, NotificationDtoMapper notificationDtoMapper,
            NotificationRestConnector notificationRestConnector,
            AwardPeriodRestClient awardPeriodRestClient, WinnersSftpConnector winnersSftpConnector,
            @Value("${core.NotificationService.updateRankingAndFindWinners.outputFilePath}") String outputFilePath,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.ttl}") Long timeToLive,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.subject}") String subject,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.markdown}") String markdown,
            @Value("${core.NotificationService.notifyWinners.delimiter}") String delimiter,
            @Value("${core.NotificationService.notifyWinners.maxRow}") Long maxRow,
            @Value("${core.NotificationService.notifyWinners.serviceName}") String serviceName,
            @Value("${core.NotificationService.notifyWinners.fileType}") String fileType) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestConnector = notificationRestConnector;
        this.awardPeriodRestClient = awardPeriodRestClient;
        this.winnersSftpConnector = winnersSftpConnector;
        this.outputFilePath = outputFilePath;
        this.timeToLive = timeToLive;
        this.subject = subject;
        this.markdown = markdown;
        DELIMITER = delimiter;
        this.maxRow = maxRow;
        this.serviceName = serviceName;
        this.fileType = fileType;
    }


    @Override
    @Scheduled(cron = "${core.NotificationService.notifyUnsetPayoffInstr.scheduler}")
    public void notifyUnsetPayoffInstr() {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.notifyUnsetPayoffInstr");
            }
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
        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.updateRankingAndWinners");
            logger.info("Executing procedure: updateRankingAndWinners");
        }
        citizenDAO.updateRankingAndWinners();
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.findWinners.scheduler}")
    public void findWinners() {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.findWinners");
            }
            List<AwardPeriod> activePeriods = awardPeriodRestClient.findActiveAwardPeriods();
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
//                        TODO definire accountHolder
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
                        if (dataLines.size() == maxRow || n == winnersForCSV.size()) {
                            m++;
                            DecimalFormat twoDigits = new DecimalFormat("00");
                            String currentFileNumber = twoDigits.format(m);
                            String totalFileNumber = twoDigits.format((int) Math.ceil((double) winnersForCSV.size() / maxRow));
//                            TODO definire fileType
                            File csvOutputFile = new File(serviceName + "."
                                    + fileType + "."
                                    + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "."
                                    + LocalTime.now().format(DateTimeFormatter.ofPattern("hhmmss"))+ "."
                                    + currentFileNumber + "_" + totalFileNumber
                                    + ".csv");
//                            TODO rimuovere commento per produrre il csv
//                            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
//                                dataLines.forEach(pw::println);
//                            }

//                            TODO cifrare csv e inviare a sftp (decommentare)
//                            winnersSftpConnector.sendFile(csvOutputFile);
                            dataLines.clear();
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}

