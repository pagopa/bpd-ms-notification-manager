package it.gov.pagopa.bpd.notification_manager.connector;

import it.gov.pagopa.bpd.notification_manager.connector.config.WinnersSftpChannelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class WinnersSftpConnector {

    private final WinnersSftpChannelConfig.WinnersSftpGateway winnersSftpGateway;
    private final BeanFactory beanFactory;

    public void sendFile(File file) {
        winnersSftpGateway.sendToSftp(file);
    }

}
