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
    private final Long maxRecordToSave;
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
            @Value("${core.NotificationService.findWinners.maxRecordToSave}") Long maxRecordToSave,
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
        this.maxRecordToSave = maxRecordToSave;
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

                    DecimalFormat nineDigits = new DecimalFormat("000000000");
                    DecimalFormat twoDigits = new DecimalFormat("00");
                    DecimalFormat sixDigits = new DecimalFormat("000000");

                    List<WinningCitizen> winnersForCSV = new ArrayList<>();
                    for (WinningCitizen winningCitizen : winners) {
                        if (winningCitizen.getPayoffInstr() != null && !winningCitizen.getPayoffInstr().isEmpty())
                            winnersForCSV.add(winningCitizen);
                    }

                    List<String> dataLines = new ArrayList<>();
                    int m = 0;
                    int n = 0;
                    Path tempDir = Files.createTempDirectory("csv_directory");
                    if (logger.isDebugEnabled()) {
                        logger.debug("NotificationServiceImpl.findWinners");
                        logger.debug("temporaryDirectoryPath = " + tempDir.toAbsolutePath().toString());
                    }
                    String filenamePrefix = tempDir + File.separator
                            + serviceName + "."
                            + authorityType + "."
                            + fileType + "."
                            + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "."
                            + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + ".";

                    String totalFileNumber = twoDigits.format((int) Math.ceil((double) winnersForCSV.size() / maxRow));
                    String currentFileNumber = null;

                    String fileName = null;

                    boolean initLoop = false;

                    List<WinningCitizen> winnersToUpdate = new ArrayList<>();

//                    Viene creata una riga nel csv per ogni vincitore
                    for (WinningCitizen winnerForCSV : winnersForCSV) {

                        if (!initLoop) {
                            m++;
                            currentFileNumber = twoDigits.format(m);

                            fileName = filenamePrefix
                                    + currentFileNumber + "_" + totalFileNumber + "."
                                    + String.valueOf((winnersForCSV.size() <= maxRow ? winnersForCSV.size() :
                                    ((int) Math.ceil(((double) winnersForCSV.size() / maxRow)) > m ? maxRow : winnersForCSV.size() % maxRow)))
                                    + ".csv";
                            initLoop = true;
                        }

                        n++;

//                        La causale varia a seconda dell'esito sul controllo dell'intestatario dello strumento di pagamento
                        String paymentReason = (winnerForCSV.getAccountHolderFiscalCode().equals(winnerForCSV.getFiscalCode())) ?
                                (nineDigits.format(winnerForCSV.getId()) +
                                        " - Cashback di Stato - dal "
                                        + winnerForCSV.getAwardPeriodStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                        " al " + winnerForCSV.getAwardPeriodEnd().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) :
                                (nineDigits.format(winnerForCSV.getId()) +
                                        " - Cashback di Stato - dal " +
                                        winnerForCSV.getAwardPeriodStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                        " al " + winnerForCSV.getAwardPeriodEnd().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                        " - " + winnerForCSV.getFiscalCode());

                        String sb = nineDigits.format(winnerForCSV.getId()) + DELIMITER +
                                winnerForCSV.getAccountHolderFiscalCode() + DELIMITER +
                                winnerForCSV.getPayoffInstr() + DELIMITER +
                                winnerForCSV.getAccountHolderName() + DELIMITER +
                                winnerForCSV.getAccountHolderSurname() + DELIMITER +
                                sixDigits.format(winnerForCSV.getAmount()) + DELIMITER +
                                paymentReason + DELIMITER +
                                winnerForCSV.getTypology() + DELIMITER +
                                twoDigits.format(winnerForCSV.getAwardPeriodId()) + DELIMITER +
                                winnerForCSV.getAwardPeriodStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + DELIMITER +
                                winnerForCSV.getAwardPeriodEnd().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + DELIMITER +
                                sixDigits.format(winnerForCSV.getCashback()) + DELIMITER +
                                sixDigits.format(winnerForCSV.getJackpot()) + DELIMITER +
                                winnerForCSV.getCheckInstrStatus() + DELIMITER +
                                winnerForCSV.getTechnicalAccountHolder();
                        dataLines.add(sb);

                        winnerForCSV.setChunkFilename(fileName);
                        winnerForCSV.setStatus(WinningCitizen.Status.SEND);

                        winnersToUpdate.add(winnerForCSV);
                        if (winnersToUpdate.size() == maxRecordToSave || n == winnersForCSV.size()) {
                            citizenDAO.saveAll(winnersToUpdate);
                            winnersToUpdate.clear();
                        }


//                        Se il csv ha raggiunto il numero massimo di righe stabilito si procede con la fase di
//                        encrypt e invio, i vincitori restanti verranno registrati su altri csv
                        if (dataLines.size() == maxRow || n == winnersForCSV.size()) {

//                            Il file verrà creato in una directory temporanea
                            File csvOutputFile = new File(fileName);
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
                            if (logger.isInfoEnabled()) {
                                logger.info("NotificationManagerServiceImpl.findWinners");
                                logger.info("Sent File: " + csvPgpFile.getName());
                            }
                            dataLines.clear();

                            initLoop = false;
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

