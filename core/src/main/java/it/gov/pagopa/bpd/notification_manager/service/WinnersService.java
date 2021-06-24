package it.gov.pagopa.bpd.notification_manager.service;


import it.gov.pagopa.bpd.notification_manager.connector.jpa.model.WinningCitizen;

import java.io.IOException;

public interface WinnersService {

    void updateWinners(Long awardPeriodId);

    void sendWinners(Long awardPeriodId, String lastChunkFilenameSent);

    void restoreWinners(Long awardPeriodId, WinningCitizen.Status status, String chunkFilename);

    void testConnection() throws IOException;

}
