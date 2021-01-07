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
import org.apache.commons.codec.digest.DigestUtils;
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
            if (citizensFC != null && !citizensFC.isEmpty()) {
                for (String citizenCf : citizensFC) {
                    NotificationDTO dto = notificationDtoMapper.NotificationDtoMapper(
                            citizenCf, timeToLive, subject, markdown);
                    NotificationResource resource = notificationRestConnector.notify(dto);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Notified citizen, notification id: " + resource.getId());
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    @Scheduled(cron = "${core.NotificationService.updateRanking.scheduler}")
    public void updateRanking() {
        if (logger.isInfoEnabled()) {
            logger.info("Executing procedure: updateRanking");
        }
        citizenDAO.updateRanking();
        if (logger.isInfoEnabled()) {
            logger.info("Executed procedure: updateRanking");
        }

    }

    @Override
    @Scheduled(cron = "${core.NotificationService.updateWinners.scheduler}")
    public void updateWinners() {
        if (logger.isInfoEnabled()) {
            logger.info("Executing procedure: updateWinners");
        }
        citizenDAO.updateWinners();
        if (logger.isInfoEnabled()) {
            logger.info("Executed procedure: updateWinners");
        }

    }

    @Override
    @Scheduled(cron = "${core.NotificationService.findWinners.scheduler}")
    public void findWinners() {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.findWinners");
            }

            List<AwardPeriod> awardPeriods = awardPeriodRestClient.findAllAwardPeriods();

//            Ricerca dell'award period in conclusione
            Long endingPeriodId = null;
            for (AwardPeriod awardPeriod : awardPeriods) {
                if (LocalDate.now().equals(awardPeriod.getEndDate()
                        .plus(Period.ofDays(awardPeriod.getGracePeriod().intValue() + 1)))) {
                    endingPeriodId = awardPeriod.getAwardPeriodId();
                }
            }


//            Se è presente un award period in conclusione si creano i csv con i vincitori e si inoltrano a SFG SIA
            if (endingPeriodId != null) {

                logger.info("Starting findWinners query");
                List<WinningCitizen> winners = citizenDAO.findWinners(endingPeriodId);
                logger.info("Search for winners finished");

//                Posso essere inviati solo i vincitori con strumento di pagamento valorizzato
                if (winners != null && !winners.isEmpty()) {

                    logger.info("Winners found");
                    DecimalFormat nineDigits = new DecimalFormat("000000000");
                    DecimalFormat twoDigits = new DecimalFormat("00");
                    DecimalFormat sixDigits = new DecimalFormat("000000");

                    List<String> dataLines = new ArrayList<>();
                    int m = 0;
                    int n = 0;
                    Path tempDir = Files.createTempDirectory("csv_directory");

                    logger.info("temporaryDirectoryPath = " + tempDir.toAbsolutePath().toString());

                    String filenamePrefix = tempDir + File.separator
                            + serviceName + "."
                            + authorityType + "."
                            + fileType + "."
                            + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "."
                            + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + ".";

                    String totalFileNumber = twoDigits.format((int) Math.ceil((double) winners.size() / maxRow));
                    String currentFileNumber = null;

                    String fileName = null;

                    boolean initLoop = false;

                    List<WinningCitizen> winnersToUpdate = new ArrayList<>();

//                    Viene creata una riga nel csv per ogni vincitore
                    for (WinningCitizen winner : winners) {

                        if (!initLoop) {
                            m++;
                            currentFileNumber = twoDigits.format(m);

                            fileName = filenamePrefix
                                    + currentFileNumber + "_" + totalFileNumber + "."
                                    + String.valueOf((winners.size() <= maxRow ? winners.size() :
                                    ((int) Math.ceil(((double) winners.size() / maxRow)) > m ? maxRow : winners.size() % maxRow)))
                                    + ".csv";
                            initLoop = true;
                        }

                        n++;

                        // La causale varia a seconda dell'esito sul controllo dell'intestatario dello strumento di pagamento
                        String paymentReason = winner.getFiscalCode().equals(winner.getAccountHolderFiscalCode()) ?
                                nineDigits.format(winner.getId()) +
                                        " - Cashback di Stato - dal "
                                        + winner.getAwardPeriodStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                        " al " + winner.getAwardPeriodEnd().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                                nineDigits.format(winner.getId()) +
                                        " - Cashback di Stato - dal " +
                                        winner.getAwardPeriodStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                        " al " + winner.getAwardPeriodEnd().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                        " - " + winner.getFiscalCode();

                        StringBuilder sb = new StringBuilder()
                                .append(nineDigits.format(winner.getId()))
                                .append(DELIMITER)
                                .append(winner.getAccountHolderFiscalCode())
                                .append(DELIMITER)
                                .append(winner.getPayoffInstr())
                                .append(DELIMITER)
                                .append(winner.getAccountHolderName())
                                .append(DELIMITER)
                                .append(winner.getAccountHolderSurname())
                                .append(DELIMITER)
                                .append(sixDigits.format(winner.getAmount()))
                                .append(DELIMITER)
                                .append(paymentReason)
                                .append(DELIMITER)
                                .append(winner.getTypology())
                                .append(DELIMITER)
                                .append(twoDigits.format(winner.getAwardPeriodId()))
                                .append(DELIMITER)
                                .append(winner.getAwardPeriodStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                .append(DELIMITER)
                                .append(winner.getAwardPeriodEnd().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                .append(DELIMITER)
                                .append(sixDigits.format(winner.getCashback()))
                                .append(DELIMITER)
                                .append(sixDigits.format(winner.getJackpot()))
                                .append(DELIMITER)
                                .append(winner.getCheckInstrStatus())
                                .append(DELIMITER)
                                .append(winner.getTechnicalAccountHolder());
                        dataLines.add(sb.toString());

                        winner.setChunkFilename(fileName);
                        winner.setStatus(WinningCitizen.Status.SEND);

                        winnersToUpdate.add(winner);
                        if (winnersToUpdate.size() == maxRecordToSave || n == winners.size()) {
                            citizenDAO.saveAll(winnersToUpdate);
                            winnersToUpdate.clear();
                        }

//                        Se il csv ha raggiunto il numero massimo di righe stabilito si procede con la fase di
//                        encrypt e invio, i vincitori restanti verranno registrati su altri csv
                        if (dataLines.size() == maxRow || n == winners.size()) {

//                            Il file verrà creato in una directory temporanea
                            File csvOutputFile = new File(fileName);
                            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                                dataLines.forEach(pw::println);
                            }

                            File csvChecksumOutputFile = createChecksumFile(fileName);

//                            Il file CSV viene criptato e depositato nella stessa directory temporanea
                            String publicKeyWithLineBreaks = publicKey.replace("\\n", System.lineSeparator());
                            InputStream publicKeyIS = new ByteArrayInputStream(publicKeyWithLineBreaks.getBytes());
                            String csvPgpFileName = fileName.concat(".pgp");
                            FileOutputStream outputFOS = new FileOutputStream(csvPgpFileName);
                            EncryptUtil.encryptFile(outputFOS,
                                    csvOutputFile.getAbsolutePath(),
                                    EncryptUtil.readPublicKey(publicKeyIS),
                                    false, true);
                            File csvPgpFile = new File(csvPgpFileName);

                            sendFiles(csvPgpFile, csvChecksumOutputFile);
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


    private void sendFiles(File... files) {
        for (File file : files) {

            if (logger.isInfoEnabled()) {
                logger.info("Sending File: " + file.getName());
            }

            winnersSftpConnector.sendFile(file);

            if (logger.isInfoEnabled()) {
                logger.info("Sent File: " + file.getName());
            }
        }
    }


    private File createChecksumFile(String fileName) throws IOException {
        String checksum = DigestUtils.sha256Hex(new FileInputStream(fileName));
        File csvChecksumOutputFile = new File(fileName.replace(".csv", ".sha256sum"));
        try (PrintWriter pw = new PrintWriter(csvChecksumOutputFile)) {
            pw.println(checksum);
        }

        return csvChecksumOutputFile;
    }


    @Override
    public void testConnection() throws IOException {
        Path tempDir = Files.createTempDirectory("test_temp");
        File testFile = new File(tempDir + File.separator + "test.txt");
        FileOutputStream outputFOS = new FileOutputStream(testFile.getAbsolutePath());
        List<String> dataLines = new ArrayList<>();
        dataLines.add("test");
        try (PrintWriter pw = new PrintWriter(testFile)) {
            dataLines.forEach(pw::println);
        }

        File fileToSend = new File(testFile.getAbsolutePath());

        logger.info("Sending test file");
        winnersSftpConnector.sendFile(fileToSend);
        logger.info("Sent test file");
    }

}

