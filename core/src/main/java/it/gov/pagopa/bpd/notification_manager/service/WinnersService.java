package it.gov.pagopa.bpd.notification_manager.service;


import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public interface WinnersService {

    void sendWinners(List<WinningCitizen> winners, Long endingPeriodId, int fileChunkCount, Path tempDir, LocalDateTime timestamp, int recordTotCount);

    void testConnection() throws IOException;

}
