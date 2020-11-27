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
import it.gov.pagopa.bpd.notification_manager.encryption.EncryptUtil;
import it.gov.pagopa.bpd.notification_manager.mapper.NotificationDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final String authorityType;
    private final String fileType;
    private final String publicKey;

    @Autowired
    NotificationServiceImpl(
            CitizenDAO citizenDAO,
            NotificationDtoMapper notificationDtoMapper,
            NotificationRestConnector notificationRestConnector,
            AwardPeriodRestClient awardPeriodRestClient,
            WinnersSftpConnector winnersSftpConnector,
            @Value("${core.NotificationService.updateRankingAndFindWinners.outputFilePath}") String outputFilePath,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.ttl}") Long timeToLive,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.subject}") String subject,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.markdown}") String markdown,
            @Value("${core.NotificationService.notifyWinners.delimiter}") String delimiter,
            @Value("${core.NotificationService.notifyWinners.maxRow}") Long maxRow,
            @Value("${core.NotificationService.notifyWinners.serviceName}") String serviceName,
            @Value("${core.NotificationService.notifyWinners.authorityType}") String authorityType,
            @Value("${core.NotificationService.notifyWinners.fileType}") String fileType,
            @Value("${core.NotificationService.notifyWinners.publicKey}") String publicKey) {
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
        this.authorityType = authorityType;
        this.fileType = fileType;
        this.publicKey = publicKey;
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
            //List<AwardPeriod> activePeriods = awardPeriodRestClient.findActiveAwardPeriods();
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
                    Path tempDir = Files.createTempDirectory("csv_directory");
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
//                                TODO codice fiscale del titolare del conto o del vincitore?''
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

                            File csvOutputFile = new File(tempDir + "\\"
                                    + serviceName + "."
                                    + authorityType + "."
                                    + fileType + "."
                                    + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "."
                                    + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + "."
                                    + currentFileNumber + "_" + totalFileNumber + "."
                                    + dataLines.size()
                                    + ".csv");

                            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                                dataLines.forEach(pw::println);
                            }
                            String publicKeyWithLineBreaks = publicKey.replace("\\n", System.lineSeparator());
                            InputStream publicKeyIS = new ByteArrayInputStream(publicKeyWithLineBreaks.getBytes());
                            FileOutputStream outputFOS = new FileOutputStream(csvOutputFile.getAbsolutePath().concat(".pgp"));
                            EncryptUtil.encryptFile(outputFOS,
                                    csvOutputFile.getAbsolutePath(),
                                    EncryptUtil.readPublicKey(publicKeyIS),
                                    false, true);

                            File csvPgpFile = new File(csvOutputFile.getAbsolutePath().concat(".pgp"));
                            winnersSftpConnector.sendFile(csvPgpFile);
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

