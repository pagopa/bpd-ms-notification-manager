package it.gov.pagopa.bpd.notification_manager.service;


import java.io.IOException;
import java.nio.file.Path;

public interface WinnersService {

    int sendWinners(Long endingPeriodId, int fileChunkCount, Path tempDir);

    void testConnection() throws IOException;

}
