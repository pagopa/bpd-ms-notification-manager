package it.gov.pagopa.bpd.notification_manager.service;


import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface WinnersService {

    int sendWinners(Long endingPeriodId, int fileChunkCount, Path tempDir, LocalDateTime timestamp, int recordTotCount);

    void testConnection() throws IOException;

}
