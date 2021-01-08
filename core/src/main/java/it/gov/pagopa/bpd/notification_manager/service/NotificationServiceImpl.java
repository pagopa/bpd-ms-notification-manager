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
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchProviderException;
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

    private static final String PAYMENT_REASON_DELIMITER = " - ";
    private static final DecimalFormat NINE_DIGITS_FORMAT = new DecimalFormat("000000000");
    private static final DecimalFormat TWO_DIGITS_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat SIX_DIGITS_FORMAT = new DecimalFormat("000000");

    private final CitizenDAO citizenDAO;
    private final NotificationDtoMapper notificationDtoMapper;
    private final NotificationRestConnector notificationRestConnector;
    private final AwardPeriodRestClient awardPeriodRestClient;
    private final WinnersSftpConnector winnersSftpConnector;
    private final Long timeToLive;
    private final String subject;
    private final String markdown;
    private final String CSV_DELIMITER;
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
        CSV_DELIMITER = delimiter;
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
                logger.info("NotificationManagerServiceImpl.notifyUnsetPayoffInstr start");
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
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.notifyUnsetPayoffInstr end");
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
    public void sendWinners() {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.sendWinners start");
            }

            List<AwardPeriod> awardPeriods = awardPeriodRestClient.findAllAwardPeriods();

            Long endingPeriodId = null;
            for (AwardPeriod awardPeriod : awardPeriods) {
                if (LocalDate.now().equals(awardPeriod.getEndDate()
                        .plus(Period.ofDays(awardPeriod.getGracePeriod().intValue() + 1)))) {
                    endingPeriodId = awardPeriod.getAwardPeriodId();
                }
            }

            // Se è presente un award period in conclusione si creano i csv con i vincitori e si inoltrano a SFG SIA
            if (endingPeriodId != null) {
                int fileChunkCount = 0;
                int fetchedRecord;
                Path tempDir = null;

                do {
                    fetchedRecord = sendWinners(endingPeriodId, fileChunkCount, tempDir);
                    fileChunkCount++;

                } while (fetchedRecord == maxRow);
            }

            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.sendWinners end");
            }

        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new RuntimeException(e);
        }
    }


    @Transactional
    public int sendWinners(Long endingPeriodId, int fileChunkCount, Path tempDir) {
        long offset = maxRow * fileChunkCount;
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Starting findWinners query with offset = %d and limit = %d", offset, maxRow));
        }
        List<WinningCitizen> winners = citizenDAO.findWinners(endingPeriodId, offset, maxRow);
        if (logger.isInfoEnabled()) {
            logger.info("Search for winners finished");
        }

        if (winners != null && !winners.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Winners found");
            }

            try {

                if (tempDir == null) {
                    tempDir = Files.createTempDirectory("csv_directory");
                }

                if (logger.isInfoEnabled()) {
                    logger.info("temporaryDirectoryPath = " + tempDir.toAbsolutePath().toString());
                }

                String filenamePrefix = tempDir + File.separator
                        + serviceName + "."
                        + authorityType + "."
                        + fileType + "."
                        + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "."
                        + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + ".";

                String totalFileNumber = TWO_DIGITS_FORMAT.format((int) Math.ceil((double) winners.size() / maxRow));
                String currentFileNumber = TWO_DIGITS_FORMAT.format(fileChunkCount);

                String fileName = filenamePrefix
                        + currentFileNumber + "_" + totalFileNumber + "."
                        + (winners.size() <= maxRow ? winners.size() :
                        ((int) Math.ceil(((double) winners.size() / maxRow)) > fileChunkCount ? maxRow : winners.size() % maxRow))
                        + ".csv";
                File csvOutputFile = new File(fileName);
                PrintWriter csvPrintWriter = new PrintWriter(csvOutputFile);

                for (WinningCitizen winner : winners) {
                    String csvRow = generateCsvRow(winner);
                    csvPrintWriter.println(csvRow);

                    winner.setChunkFilename(fileName);
                    winner.setStatus(WinningCitizen.Status.SENT);
                }

                csvPrintWriter.close();
                File csvChecksumOutputFile = createChecksumFile(csvOutputFile);
                File csvPgpFile = cryptFile(csvOutputFile);
                citizenDAO.saveAll(winners);
                sendFiles(csvPgpFile, csvChecksumOutputFile);

            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                throw new RuntimeException(e);
            }
        }

        return winners.size();
    }


    private String generateCsvRow(WinningCitizen winner) {
        StringBuilder paymentReasonBuilder = new StringBuilder()
                .append(NINE_DIGITS_FORMAT.format(winner.getId()))
                .append(PAYMENT_REASON_DELIMITER)
                .append("Cashback di Stato")
                .append(PAYMENT_REASON_DELIMITER)
                .append("dal ").append(winner.getAwardPeriodStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(" al ").append(winner.getAwardPeriodEnd().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        if (!winner.getFiscalCode().equals(winner.getAccountHolderFiscalCode())) {
            paymentReasonBuilder.append(PAYMENT_REASON_DELIMITER)
                    .append(winner.getFiscalCode());
        }

        StringBuilder csvRowBuilder = new StringBuilder()
                .append(NINE_DIGITS_FORMAT.format(winner.getId()))
                .append(CSV_DELIMITER)
                .append(winner.getAccountHolderFiscalCode())
                .append(CSV_DELIMITER)
                .append(winner.getPayoffInstr())
                .append(CSV_DELIMITER)
                .append(winner.getAccountHolderName())
                .append(CSV_DELIMITER)
                .append(winner.getAccountHolderSurname())
                .append(CSV_DELIMITER)
                .append(SIX_DIGITS_FORMAT.format(winner.getAmount()))
                .append(CSV_DELIMITER)
                .append(paymentReasonBuilder.toString())
                .append(CSV_DELIMITER)
                .append(winner.getTypology())
                .append(CSV_DELIMITER)
                .append(TWO_DIGITS_FORMAT.format(winner.getAwardPeriodId()))
                .append(CSV_DELIMITER)
                .append(winner.getAwardPeriodStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(CSV_DELIMITER)
                .append(winner.getAwardPeriodEnd().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(CSV_DELIMITER)
                .append(SIX_DIGITS_FORMAT.format(winner.getCashback()))
                .append(CSV_DELIMITER)
                .append(SIX_DIGITS_FORMAT.format(winner.getJackpot()))
                .append(CSV_DELIMITER)
                .append(winner.getCheckInstrStatus())
                .append(CSV_DELIMITER)
                .append(winner.getTechnicalAccountHolder());

        return csvRowBuilder.toString();
    }

    private File cryptFile(File csvOutputFile) throws IOException, NoSuchProviderException, PGPException {
        String publicKeyWithLineBreaks = publicKey.replace("\\n", System.lineSeparator());
        InputStream publicKeyIS = new ByteArrayInputStream(publicKeyWithLineBreaks.getBytes());
        File csvPgpFile = new File(csvOutputFile.getAbsolutePath().concat(".pgp"));
        FileOutputStream outputFOS = new FileOutputStream(csvPgpFile);
        EncryptUtil.encryptFile(outputFOS,
                csvOutputFile.getAbsolutePath(),
                EncryptUtil.readPublicKey(publicKeyIS),
                false, true);

        return csvOutputFile;
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


    private File createChecksumFile(File csvOutputFile) throws IOException {
        String checksum = DigestUtils.sha256Hex(new FileInputStream(csvOutputFile));
        File csvChecksumOutputFile = new File(csvOutputFile.getAbsolutePath().replace(".csv", ".sha256sum"));
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

