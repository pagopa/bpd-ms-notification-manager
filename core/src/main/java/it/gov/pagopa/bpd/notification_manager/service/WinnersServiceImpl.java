package it.gov.pagopa.bpd.notification_manager.service;


import eu.sia.meda.service.BaseService;
import it.gov.pagopa.bpd.notification_manager.connector.WinnersSftpConnector;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.AwardPeriodRestClient;
import it.gov.pagopa.bpd.notification_manager.connector.award_period.model.AwardPeriod;
import it.gov.pagopa.bpd.notification_manager.connector.jdbc.CitizenJdbcDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jdbc.DaoHelper;
import it.gov.pagopa.bpd.notification_manager.connector.jdbc.model.WinningCitizenDto;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.CitizenDAO;
import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;
import it.gov.pagopa.bpd.notification_manager.encryption.EncryptUtil;
import it.gov.pagopa.bpd.notification_manager.exception.UpdateWinnerStatusException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.NoSuchProviderException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final DateTimeFormatter CSV_NAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy.HHmmss");
    private static final String CSV_NAME_SEPARATOR = ".";
    static final String ERROR_MESSAGE_TEMPLATE = "updateWinnersStatus: affected %d rows of %d";

    private final CitizenDAO citizenDAO;
    private final CitizenJdbcDAO citizenJdbcDAO;
    private final WinnersSftpConnector winnersSftpConnector;
    private final AwardPeriodRestClient awardPeriodRestClient;
    private final String CSV_DELIMITER;
    private final Long maxRow;
    private final String serviceName;
    private final String authorityType;
    private final String fileType;
    private final String publicKey;
    private final boolean sftpEnable;
    private final boolean updateStatusEnabled;
    private final boolean deleteTmpFilesEnable;
    private final String CONSAP_TWICE_START_DATE;
    private final int CONSAP_TWICE_DAYS_FREQUENCY;

    @Autowired
    WinnersServiceImpl(
            CitizenDAO citizenDAO,
            CitizenJdbcDAO citizenJdbcDAO,
            WinnersSftpConnector winnersSftpConnector,
            AwardPeriodRestClient awardPeriodRestClient,
            @Value("${core.NotificationService.findWinners.delimiter}") String delimiter,
            @Value("${core.NotificationService.findWinners.maxRow}") Long maxRow,
            @Value("${core.NotificationService.findWinners.serviceName}") String serviceName,
            @Value("${core.NotificationService.findWinners.authorityType}") String authorityType,
            @Value("${core.NotificationService.findWinners.fileType}") String fileType,
            @Value("${core.NotificationService.findWinners.publicKey}") String publicKey,
            @Value("${core.NotificationService.findWinners.sftp.enable}") boolean sftpEnable,
            @Value("${core.NotificationService.findWinners.updateStatus.enable}") boolean updateStatusEnabled,
            @Value("${core.NotificationService.findWinners.deleteTmpFiles.enable}") boolean deleteTmpFilesEnable,
            @Value("${core.NotificationService.sendWinnersTwiceWeeks.start.date}") String CONSAP_TWICE_START_DATE,
            @Value("${core.NotificationService.sendWinnersTwiceWeeks.days.frequency}") int CONSAP_TWICE_DAYS_FREQUENCY) {
        this.citizenDAO = citizenDAO;
        this.citizenJdbcDAO = citizenJdbcDAO;
        this.winnersSftpConnector = winnersSftpConnector;
        this.awardPeriodRestClient = awardPeriodRestClient;
        CSV_DELIMITER = delimiter;
        this.maxRow = maxRow;
        this.serviceName = serviceName;
        this.authorityType = authorityType;
        this.fileType = fileType;
        this.publicKey = publicKey;
        this.sftpEnable = sftpEnable;
        this.updateStatusEnabled = updateStatusEnabled;
        this.deleteTmpFilesEnable = deleteTmpFilesEnable;
        this.CONSAP_TWICE_START_DATE = CONSAP_TWICE_START_DATE;
        this.CONSAP_TWICE_DAYS_FREQUENCY = CONSAP_TWICE_DAYS_FREQUENCY;
    }


    @Scheduled(cron = "${core.NotificationService.updateAndSendWinners.scheduler}")
    public void updateAndSendWinners() {

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.updateAndSendWinners start");
        }

        List<AwardPeriod> awardPeriods = awardPeriodRestClient.findAllAwardPeriods();

        Long endingPeriodId = null;
        for (AwardPeriod awardPeriod : awardPeriods) {
            if (LocalDate.now().equals(awardPeriod.getEndDate()
                    .plus(Period.ofDays(awardPeriod.getGracePeriod().intValue() + 1)))) {
                endingPeriodId = awardPeriod.getAwardPeriodId();
            }
        }
        if (endingPeriodId != null) {
            if (logger.isInfoEnabled()) {
                logger.info("NotificationManagerServiceImpl.updateAndSendWinners: ending award period found");
            }
            updateWinners(endingPeriodId);
            sendWinners(endingPeriodId, null);
        }

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.updateAndSendWinners end");
        }
    }


    @Override
    public void updateWinners(Long awardPeriodId) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Executing procedure updateWinners for awardPeriod %s", awardPeriodId));
        }
        citizenDAO.updateWinners(awardPeriodId);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Executed procedure updateWinners for awardPeriod %s", awardPeriodId));
        }
    }


    @SneakyThrows
    @Override
    public void sendWinners(Long awardPeriodId, String lastChunkFilenameSent) {
        logger.info("NotificationManagerServiceImpl.sendWinners start");

        if (citizenDAO.existsWorkingWinner(awardPeriodId)) {
            throw new IllegalStateException(String.format("There is at least one 'WIP' (work in progress) record with awardPeriodId = %d, please restore data consistency before running a new execution",
                    awardPeriodId));
        }

        int fileChunkCount;
        int totalChunkCount;
        Path tempDir = Files.createTempDirectory("csv_directory");
        logger.info("temporaryDirectoryPath = {}", tempDir.toAbsolutePath());
        String filenamePrefix;

        if (lastChunkFilenameSent != null) {
            String[] filenameParts = StringUtils.split(lastChunkFilenameSent, CSV_NAME_SEPARATOR);
            filenamePrefix = tempDir + File.separator
                    + filenameParts[0] + CSV_NAME_SEPARATOR
                    + filenameParts[1] + CSV_NAME_SEPARATOR
                    + filenameParts[2] + CSV_NAME_SEPARATOR
                    + filenameParts[3] + CSV_NAME_SEPARATOR
                    + filenameParts[4];
            String[] fileCountParts = StringUtils.split(filenameParts[5], '_');
            fileChunkCount = Integer.parseInt(fileCountParts[0]) + 1;
            totalChunkCount = Integer.parseInt(fileCountParts[1]);

        } else {
            fileChunkCount = 1;
            filenamePrefix = tempDir + File.separator
                    + serviceName + CSV_NAME_SEPARATOR
                    + authorityType + CSV_NAME_SEPARATOR
                    + fileType + CSV_NAME_SEPARATOR
                    + LocalDateTime.now().format(CSV_NAME_DATE_TIME_FORMATTER);

            int recordTotCount = citizenDAO.countFindWinners(awardPeriodId);
            totalChunkCount = (int) Math.ceil((double) recordTotCount / maxRow);
        }

        sendWinners(awardPeriodId, filenamePrefix, totalChunkCount, fileChunkCount);

        if (deleteTmpFilesEnable) {
            logger.info("Deleting {}", tempDir);
            deleteDirectoryRecursion(tempDir);
        }

        logger.info("NotificationManagerServiceImpl.sendWinners end");
    }


    private void sendWinners(Long awardPeriodId, String filenamePrefix, int totalChunkCount, int fileChunkCount) {
        if (log.isDebugEnabled()) {
            logger.debug("awardPeriodId = {}, filenamePrefix = {}, totalChunkCount = {}, fileChunkCount = {}, maxRow = {}",
                    awardPeriodId,
                    filenamePrefix,
                    totalChunkCount,
                    fileChunkCount,
                    maxRow);
        }
        int fetchedRecord;

        do {
            logger.info("Starting findWinners chunk {} of {}", fileChunkCount, totalChunkCount);
            List<WinningCitizen> winners = updateStatusEnabled
                    ? citizenDAO.findWinners(awardPeriodId, maxRow)
                    : citizenDAO.findWinners(awardPeriodId, maxRow, (fileChunkCount - 1) * maxRow);
            fetchedRecord = winners.size();
            logger.info("Fetched {} winners", fetchedRecord);

            if (updateStatusEnabled) {
                OffsetDateTime now = OffsetDateTime.now();
                List<WinningCitizenDto> winningCitizenDtos = winners.stream()
                        .map(winner -> WinningCitizenDto.builder()
                                .id(winner.getId())
                                .status(WinningCitizen.Status.WIP.name())
                                .updateDate(now)
                                .updateUser("SEND_WINNERS")
                                .build())
                        .collect(Collectors.toList());
                int[] affectedRows = citizenJdbcDAO.updateWinningCitizenStatus(winningCitizenDtos);
                checkErrors(winningCitizenDtos.size(), affectedRows);
            }

            String filename = sendWinners(winners, filenamePrefix, fileChunkCount, totalChunkCount);

            if (updateStatusEnabled) {
                OffsetDateTime now = OffsetDateTime.now();
                List<WinningCitizenDto> winningCitizenDtos = winners.stream()
                        .map(winner -> WinningCitizenDto.builder()
                                .id(winner.getId())
                                .status(WinningCitizen.Status.SENT.name())
                                .updateDate(now)
                                .updateUser("SEND_WINNERS")
                                .chunkFilename(filename)
                                .build())
                        .collect(Collectors.toList());
                int[] affectedRows = citizenJdbcDAO.updateWinningCitizenStatusAndFilename(winningCitizenDtos);
                checkErrors(winningCitizenDtos.size(), affectedRows);
            }

            logger.info("Completed sendWinners chunk {} of {}.", fileChunkCount, totalChunkCount);

            fileChunkCount++;

        } while (fetchedRecord == maxRow);
    }


    private String sendWinners(List<WinningCitizen> winners, String filenamePrefix, int fileChunkCount, int totalChunkCount) {
        String fileName = null;
        if (winners != null && !winners.isEmpty()) {

            String totalFileNumber = TWO_DIGITS_FORMAT.format(totalChunkCount);
            String currentFileNumber = TWO_DIGITS_FORMAT.format(fileChunkCount);

            fileName = filenamePrefix + CSV_NAME_SEPARATOR
                    + currentFileNumber + "_" + totalFileNumber + CSV_NAME_SEPARATOR
                    + winners.size()
                    + ".csv";

            try {
                logger.info("Creating file {}", fileName);
                File csvOutputFile = new File(fileName);
                PrintWriter csvPrintWriter = new PrintWriter(csvOutputFile);

                for (WinningCitizen winner : winners) {
                    String csvRow = generateCsvRow(winner);
                    csvPrintWriter.println(csvRow);
                }

                csvPrintWriter.close();
                File csvChecksumOutputFile = createChecksumFile(csvOutputFile);
                File csvPgpFile = cryptFile(csvOutputFile);

                if (sftpEnable) {
                    sendFiles(csvPgpFile, csvChecksumOutputFile);
                }

            } catch (IOException | NoSuchProviderException | PGPException e) {
                throw new RuntimeException(e);
            }
        }

        return fileName;
    }


    private void checkErrors(int statementsCount, int[] affectedRows) {
        if (log.isTraceEnabled()) {
            log.trace("WinnersServiceImpl.checkErrors");
        }
        if (log.isDebugEnabled()) {
            log.debug("statementsCount = {}, affectedRows = {}", statementsCount, Arrays.toString(affectedRows));
        }

        if (affectedRows.length != statementsCount) {
            String message = String.format(ERROR_MESSAGE_TEMPLATE, affectedRows.length, statementsCount);
            throw new UpdateWinnerStatusException(message);

        } else {
            long failedUpdateCount = Arrays.stream(affectedRows)
                    .filter(DaoHelper.isStatementResultKO)
                    .count();

            if (failedUpdateCount > 0) {
                String message = String.format(ERROR_MESSAGE_TEMPLATE, statementsCount - failedUpdateCount, statementsCount);
                throw new UpdateWinnerStatusException(message);
            }
        }
    }


    private String generateCsvRow(WinningCitizen winner) {
        StringBuilder paymentReasonBuilder = new StringBuilder()
                .append(NINE_DIGITS_FORMAT.format(winner.getId()))
                .append(PAYMENT_REASON_DELIMITER)
                .append("Cashback di Stato")
                .append(PAYMENT_REASON_DELIMITER)
                .append("dal ").append(winner.getAwardPeriodStart().format(ONLY_DATE_FORMATTER))
                .append(" al ").append(winner.getAwardPeriodEnd().format(ONLY_DATE_FORMATTER));

        if (!winner.getFiscalCode().equals(winner.getAccountHolderFiscalCode()) ||
                winner.getTechnicalAccountHolder() != null) {
            paymentReasonBuilder.append(PAYMENT_REASON_DELIMITER)
                    .append(winner.getFiscalCode());
        }


        if (winner.getTechnicalAccountHolder() != null
                && winner.getIssuerCardId() != null) {
            paymentReasonBuilder.append(PAYMENT_REASON_DELIMITER)
                    .append(winner.getIssuerCardId());
        }

        String ticketId = winner.getTicketId() != null ? winner.getTicketId().toString() : StringUtils.EMPTY;
        String relatedId = winner.getRelatedUniqueId() != null ? winner.getRelatedUniqueId().toString() : StringUtils.EMPTY;

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
                winner.getTechnicalAccountHolder() +
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
            logger.info("Sending File: " + file.getName());
            winnersSftpConnector.sendFile(file);
            logger.info("Sent File: " + file.getName());
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


    private void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }


    @Override
    public void restoreWinners(Long awardPeriodId, WinningCitizen.Status status, String chunkFilename) {
        if (chunkFilename == null) {
            citizenDAO.restoreWinners(awardPeriodId, status);
        } else {
            citizenDAO.restoreWinners(awardPeriodId, status, chunkFilename);
        }
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


    @Scheduled(cron = "${core.NotificationService.sendWinnersTwiceWeeks.scheduler}")
    public void sendWinnersTwiceWeeks() {

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.sendWinnersTwiceWeeks start");
        }

        Boolean isTwiceWeeks = Boolean.FALSE;
        LocalDate now = LocalDate.now();

        LocalDate startDate = null;
        try {
            startDate = LocalDate.parse(CONSAP_TWICE_START_DATE, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("NotificationManagerServiceImpl.sendWinnersTwiceWeeks - Unable to format startDate from: " + CONSAP_TWICE_START_DATE);
            }
        }

        if (startDate != null
                && (now.isEqual(startDate)
                || (now.isAfter(startDate)
                && (now.toEpochDay() - startDate.toEpochDay()) % CONSAP_TWICE_DAYS_FREQUENCY == 0))
        ) {
            isTwiceWeeks = Boolean.TRUE;
        }

        if (isTwiceWeeks) {
            List<AwardPeriod> awardPeriods = awardPeriodRestClient.findAllAwardPeriods();

            List<Long> endingPeriodId = new ArrayList<>();
            for (AwardPeriod awardPeriod : awardPeriods) {
                if (now.isAfter(awardPeriod.getEndDate()
                        .plus(Period.ofDays(awardPeriod.getGracePeriod().intValue() + 1)))) {
                    endingPeriodId.add(awardPeriod.getAwardPeriodId());
                }
            }
            if (!endingPeriodId.isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("NotificationManagerServiceImpl.sendWinnersTwiceWeeks: ending award period found");
                }
                for (Long aw : endingPeriodId) {
                    sendWinners(aw, null);
                }

            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("NotificationManagerServiceImpl.sendWinnersTwiceWeeks end");
        }
    }

}

