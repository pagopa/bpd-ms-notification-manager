package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import it.gov.pagopa.bpd.notification_manager.connector.WinnersSftpConnector;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.encryption.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchProviderException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @see WinnersService
 */
@Service
@Slf4j
class WinnersServiceImpl extends BaseService implements WinnersService {

    private static final String PAYMENT_REASON_DELIMITER = " - ";
    private static final DecimalFormat NINE_DIGITS_FORMAT = new DecimalFormat("000000000");
    private static final DecimalFormat TWO_DIGITS_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat SIX_DIGITS_FORMAT = new DecimalFormat("000000");
    private static final BigDecimal CENTS_MULTIPLICAND = BigDecimal.valueOf(100);
    private static final DateTimeFormatter ONLY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CSV_NAME_ONLY_DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter CSV_NAME_ONLY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final CitizenDAO citizenDAO;
    private final WinnersSftpConnector winnersSftpConnector;
    private final String CSV_DELIMITER;
    private final Long maxRow;
    private final String serviceName;
    private final String authorityType;
    private final String fileType;
    private final String publicKey;
    private final boolean sftpEnable;
    private final boolean updateStatusEnabled;

    @Autowired
    WinnersServiceImpl(
            CitizenDAO citizenDAO,
            WinnersSftpConnector winnersSftpConnector,
            @Value("${core.NotificationService.findWinners.delimiter}") String delimiter,
            @Value("${core.NotificationService.findWinners.maxRow}") Long maxRow,
            @Value("${core.NotificationService.findWinners.serviceName}") String serviceName,
            @Value("${core.NotificationService.findWinners.authorityType}") String authorityType,
            @Value("${core.NotificationService.findWinners.fileType}") String fileType,
            @Value("${core.NotificationService.findWinners.publicKey}") String publicKey,
            @Value("${core.NotificationService.findWinners.sftp.enable}") boolean sftpEnable,
            @Value("${core.NotificationService.findWinners.updateStatus.enable}") boolean updateStatusEnabled) {
        this.citizenDAO = citizenDAO;
        this.winnersSftpConnector = winnersSftpConnector;
        CSV_DELIMITER = delimiter;
        this.maxRow = maxRow;
        this.serviceName = serviceName;
        this.authorityType = authorityType;
        this.fileType = fileType;
        this.publicKey = publicKey;
        this.sftpEnable = sftpEnable;
        this.updateStatusEnabled = updateStatusEnabled;
    }


    @Override
    @Transactional
    public int sendWinners(Long endingPeriodId, int fileChunkCount, Path tempDir, LocalDateTime timestamp, int recordTotCount) {
        if (logger.isDebugEnabled()) {
            logger.debug("WinnersServiceImpl.sendWinners start");
        }
        if (tempDir == null) {
            throw new IllegalArgumentException("tempDir cannot be null");
        }

        int result = -1;

        List<WinningCitizen> winners;
        if (updateStatusEnabled) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Starting findWinners query with limit = %d", maxRow));
            }
            winners = citizenDAO.findWinners(endingPeriodId, maxRow);
        } else {
            long offset = maxRow * fileChunkCount;
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Starting findWinners query with offset %d and limit = %d", offset, maxRow));
            }
            winners = citizenDAO.findWinners(endingPeriodId, offset, maxRow);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Search for winners finished");
        }

        if (winners != null && !winners.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Winners found");
            }

            String filenamePrefix = tempDir + File.separator
                    + serviceName + "."
                    + authorityType + "."
                    + fileType + "."
                    + timestamp.format(CSV_NAME_ONLY_DATE_FORMATTER) + "."
                    + timestamp.format(CSV_NAME_ONLY_TIME_FORMATTER) + ".";

            String totalFileNumber = TWO_DIGITS_FORMAT.format((int) Math.ceil((double) recordTotCount / maxRow));
            String currentFileNumber = TWO_DIGITS_FORMAT.format(fileChunkCount+1);

            String fileName = filenamePrefix
                    + currentFileNumber + "_" + totalFileNumber + "."
                    + (recordTotCount <= maxRow ? recordTotCount :
                    ((int) Math.ceil(((double) recordTotCount / maxRow)) > fileChunkCount+1 ? maxRow : recordTotCount % maxRow))
                    + ".csv";

            if (logger.isInfoEnabled()) {
                logger.info(String.format("Creating file %s", fileName));
            }

            try {
                File csvOutputFile = new File(fileName);
                PrintWriter csvPrintWriter = new PrintWriter(csvOutputFile);

                for (WinningCitizen winner : winners) {
                    String csvRow = generateCsvRow(winner);
                    csvPrintWriter.println(csvRow);

                    if (updateStatusEnabled) {
                        winner.setChunkFilename(fileName);
                        winner.setStatus(WinningCitizen.Status.SENT);
                    }
                }

                csvPrintWriter.close();
                File csvChecksumOutputFile = createChecksumFile(csvOutputFile);
                File csvPgpFile = cryptFile(csvOutputFile);
                if (updateStatusEnabled) {
                    citizenDAO.saveAll(winners);
                }
                if (sftpEnable) {
                    sendFiles(csvPgpFile, csvChecksumOutputFile);
                }

            } catch (IOException | NoSuchProviderException | PGPException e) {
                throw new RuntimeException(e);
            }

            result = winners.size();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("WinnersServiceImpl.sendWinners end with %d processed records", result));
        }

        return result;
    }


    private String generateCsvRow(WinningCitizen winner) {
        StringBuilder paymentReasonBuilder = new StringBuilder()
                .append(NINE_DIGITS_FORMAT.format(winner.getId()))
                .append(PAYMENT_REASON_DELIMITER)
                .append("Cashback di Stato")
                .append(PAYMENT_REASON_DELIMITER)
                .append("dal ").append(winner.getAwardPeriodStart().format(ONLY_DATE_FORMATTER))
                .append(" al ").append(winner.getAwardPeriodEnd().format(ONLY_DATE_FORMATTER));

        if (!winner.getFiscalCode().equals(winner.getAccountHolderFiscalCode())) {
            paymentReasonBuilder.append(PAYMENT_REASON_DELIMITER)
                    .append(winner.getFiscalCode());
        }


        if (winner.getIssuerCardId()!=null) {
            paymentReasonBuilder.append(PAYMENT_REASON_DELIMITER)
                    .append(winner.getIssuerCardId());
        }

        String ticketId = winner.getTicketId()!=null ? winner.getTicketId().toString() : new String("");
        String relatedId= winner.getRelatedUniqueId() != null ? winner.getRelatedUniqueId().toString() : new String("");

        return NINE_DIGITS_FORMAT.format(winner.getId()) +
                CSV_DELIMITER +
                winner.getAccountHolderFiscalCode() +
                CSV_DELIMITER +
                winner.getPayoffInstr() +
                CSV_DELIMITER +
                winner.getAccountHolderName() +
                CSV_DELIMITER +
                winner.getAccountHolderSurname() +
                CSV_DELIMITER +
                SIX_DIGITS_FORMAT.format(winner.getAmount().multiply(CENTS_MULTIPLICAND)) +
                CSV_DELIMITER +
                paymentReasonBuilder.toString() +
                CSV_DELIMITER +
                winner.getTypology() +
                CSV_DELIMITER +
                TWO_DIGITS_FORMAT.format(winner.getAwardPeriodId()) +
                CSV_DELIMITER +
                winner.getAwardPeriodStart().format(ONLY_DATE_FORMATTER) +
                CSV_DELIMITER +
                winner.getAwardPeriodEnd().format(ONLY_DATE_FORMATTER) +
                CSV_DELIMITER +
                SIX_DIGITS_FORMAT.format(winner.getCashback().multiply(CENTS_MULTIPLICAND)) +
                CSV_DELIMITER +
                SIX_DIGITS_FORMAT.format(winner.getJackpot().multiply(CENTS_MULTIPLICAND)) +
                CSV_DELIMITER +
                winner.getCheckInstrStatus() +
                CSV_DELIMITER +
                winner.getTechnicalAccountHolder()+
                CSV_DELIMITER +
                ticketId +
                CSV_DELIMITER +
                relatedId;
    }

    private File cryptFile(File csvOutputFile) throws IOException, NoSuchProviderException, PGPException {
        if (logger.isDebugEnabled()) {
            logger.debug("WinnersServiceImpl.cryptFile start");
        }

        String publicKeyWithLineBreaks = publicKey.replace("\\n", System.lineSeparator());
        InputStream publicKeyIS = new ByteArrayInputStream(publicKeyWithLineBreaks.getBytes());
        File csvPgpFile = new File(csvOutputFile.getAbsolutePath().concat(".pgp"));

        try (FileOutputStream outputFOS = new FileOutputStream(csvPgpFile)) {
            EncryptUtil.encryptFile(outputFOS,
                    csvOutputFile.getAbsolutePath(),
                    EncryptUtil.readPublicKey(publicKeyIS),
                    false, true);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("WinnersServiceImpl.cryptFile end");
        }

        return csvPgpFile;
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
        if (logger.isDebugEnabled()) {
            logger.debug("WinnersServiceImpl.createChecksumFile start");
        }

        String checksum;
        try (InputStream csvFileInputStram = new FileInputStream(csvOutputFile)) {
            checksum = DigestUtils.sha256Hex(csvFileInputStram);
        }

        File csvChecksumOutputFile = new File(csvOutputFile.getAbsolutePath().replace(".csv", ".sha256sum"));
        try (PrintWriter pw = new PrintWriter(csvChecksumOutputFile)) {
            pw.println(checksum);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("WinnersServiceImpl.createChecksumFile end");
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

