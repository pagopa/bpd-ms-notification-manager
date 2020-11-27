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
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.ttl}") Long timeToLive,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.subject}") String subject,
            @Value("${core.NotificationService.notifyUnsetPayoffInstr.markdown}") String markdown,
            @Value("${core.NotificationService.findWinners.delimiter}") String delimiter,
            @Value("${core.NotificationService.findWinners.maxRow}") Long maxRow,
            @Value("${core.NotificationService.findWinners.serviceName}") String serviceName,
            @Value("${core.NotificationService.findWinners.authorityType}") String authorityType,
            @Value("${core.NotificationService.findWinners.fileType}") String fileType,
            @Value("${core.NotificationService.findWinners.publicKey}") String publicKey) {
        this.citizenDAO = citizenDAO;
        this.notificationDtoMapper = notificationDtoMapper;
        this.notificationRestConnector = notificationRestConnector;
        this.awardPeriodRestClient = awardPeriodRestClient;
        this.winnersSftpConnector = winnersSftpConnector;
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
    @Scheduled(cron = "${core.NotificationService.updateRankingAndWinners.scheduler}")
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
            //TODO rimuovere blocco --->
//            List<AwardPeriod> activePeriods = new ArrayList<AwardPeriod>();
//            AwardPeriod awp1 = new AwardPeriod();
//            awp1.setAwardPeriodId(2L);
//            awp1.setEndDate(LocalDate.now().minus(Period.ofDays(15)));
//            awp1.setGracePeriod(15L);
//            awp1.setStartDate(LocalDate.now().minus(Period.ofDays(50)));
//            activePeriods.add(awp1);
//            AwardPeriod awp2 = new AwardPeriod();
//            awp2.setAwardPeriodId(1L);
//            awp2.setEndDate(LocalDate.now().minus(Period.ofDays(5)));
//            awp2.setGracePeriod(15L);
//            awp2.setStartDate(LocalDate.now().minus(Period.ofDays(40)));
//            activePeriods.add(awp2);
//            AwardPeriod awp3 = new AwardPeriod();
//            awp3.setAwardPeriodId(3L);
//            awp3.setEndDate(LocalDate.now().plus(Period.ofDays(5)));
//            awp3.setGracePeriod(15L);
//            awp3.setStartDate(LocalDate.now().minus(Period.ofDays(15)));
//            activePeriods.add(awp3);
            // <---

//            Ricerca dell'award period in conclusione
            Long endingPeriodId = null;
            for (AwardPeriod activePeriod : activePeriods) {
                if (LocalDate.now().equals(activePeriod.getEndDate()
                        .plus(Period.ofDays(activePeriod.getGracePeriod().intValue())))) {
                    endingPeriodId = activePeriod.getAwardPeriodId();
                }
            }

//            Se è presente un award period in conclusione si creano i csv con i vincitori e si inoltrano a SFG SIA
            if (endingPeriodId != null) {
                List<WinningCitizen> winners = citizenDAO.findWinners(endingPeriodId);

//                Posso essere inviati solo i vincitori con strumento di pagamento valorizzato
                if (winners != null && !winners.isEmpty()) {
                    List<WinningCitizen> winnersForCSV = new ArrayList<>();
                    for (WinningCitizen winningCitizen : winners) {
                        if (winningCitizen.getPayoffInstr() != null && !winningCitizen.getPayoffInstr().isEmpty())
                            winnersForCSV.add(winningCitizen);
                    }

                    List<String> dataLines = new ArrayList<>();
                    int m = 0;
                    int n = 0;
                    Path tempDir = Files.createTempDirectory("csv_directory");

//                    Viene creata una riga nel csv per ogni vincitore
                    for (WinningCitizen winnerForCSV : winnersForCSV) {
                        n++;

//                        La causale varia a seconda dell'esito sul controllo dell'intestatario dello strumento di pagamento
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

//                        Se il csv ha raggiunto il numero massimo di righe stabilito si procede con la fase di
//                        encrypt e invio, i vincitori restanti verranno registrati su altri csv
                        if (dataLines.size() == maxRow || n == winnersForCSV.size()) {
                            m++;
                            DecimalFormat twoDigits = new DecimalFormat("00");
                            String currentFileNumber = twoDigits.format(m);
                            String totalFileNumber = twoDigits.format((int) Math.ceil((double) winnersForCSV.size() / maxRow));

//                            Il file verrà creato in una directory temporanea
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

//                            Il file CSV viene criptato e depositato nella stessa directory temporanea
                            String publicKeyWithLineBreaks = publicKey.replace("\\n", System.lineSeparator());
                            InputStream publicKeyIS = new ByteArrayInputStream(publicKeyWithLineBreaks.getBytes());
                            FileOutputStream outputFOS = new FileOutputStream(csvOutputFile.getAbsolutePath().concat(".pgp"));
                            EncryptUtil.encryptFile(outputFOS,
                                    csvOutputFile.getAbsolutePath(),
                                    EncryptUtil.readPublicKey(publicKeyIS),
                                    false, true);

//                            Il file viene infine inviato su SFTP SIA
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

