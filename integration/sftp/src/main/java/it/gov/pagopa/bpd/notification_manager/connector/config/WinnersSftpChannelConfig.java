package it.gov.pagopa.bpd.notification_manager.connector.config;

import com.jcraft.jsch.ChannelSftp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.*;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import java.io.File;
import java.util.logging.Logger;


@Configuration
//@Slf4j
@IntegrationComponentScan(value = "it.gov.pagopa.bpd")
@PropertySource("classpath:config/winnersSftpChannel.properties")
public class WinnersSftpChannelConfig {

    static Logger logger;

    @Value("${connectors.sftpConfigurations.connection.host}")
    private String host;
    @Value("${connectors.sftpConfigurations.connection.port}")
    private int port;
    @Value("${connectors.sftpConfigurations.connection.user}")
    private String user;
    @Value("${connectors.sftpConfigurations.connection.password}")
    private String password;
    @Value("${connectors.sftpConfigurations.connection.privateKey:#{null}}")
    private String privateKey;
    @Value("${connectors.sftpConfigurations.connection.passphrase}")
    private String passphrase;
    @Value("${connectors.sftpConfigurations.connection.outbound-directory}")
    private String remoteOutboundDirectory;
    @Value("${connectors.sftpConfigurations.connection.inbound-directory}")
    private String remoteInboundDirectory;
    @Value("${connectors.sftpConfigurations.connection.allowUnknownKeys}")
    private Boolean allowUnknownKeys;

    @Bean
    public SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUser(user);
        if (privateKey != null) {
            Resource privateKeyResource = new ByteArrayResource((
                    privateKey.replace("\\n", System.lineSeparator()))
                    .getBytes());
            factory.setPrivateKey(privateKeyResource);
            factory.setPrivateKeyPassphrase(passphrase);
        } else {
            factory.setPassword(password);
        }
        factory.setAllowUnknownKeys(allowUnknownKeys);
        return new CachingSessionFactory<>(factory);
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler handler() {
        SftpMessageHandler handler = new SftpMessageHandler(sftpSessionFactory());
        handler.setRemoteDirectoryExpression(new LiteralExpression(remoteOutboundDirectory));
        handler.setFileNameGenerator(message -> {
            if (message.getPayload() instanceof File) {
                return ((File) message.getPayload()).getName();
            } else {
                throw new IllegalArgumentException("File expected as payload.");
            }
        });
        return handler;
    }

    @MessagingGateway
    public interface WinnersSftpGateway {

        @Gateway(requestChannel = "sftpChannel")
        void sendToSftp(File file);

    }


    @Bean
    public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
        SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
        fileSynchronizer.setDeleteRemoteFiles(false);
        fileSynchronizer.setRemoteDirectory(remoteInboundDirectory);
//        fileSynchronizer.setFilter(new SftpSimplePatternFileListFilter("*.xml"));
        return fileSynchronizer;
    }

    @Bean
    @InboundChannelAdapter(channel = "inboundSftpChannel", poller = @Poller(fixedDelay = "2000"))
    public MessageSource<File> sftpMessageSource() {
        SftpInboundFileSynchronizingMessageSource source =
                new SftpInboundFileSynchronizingMessageSource(sftpInboundFileSynchronizer());
        source.setLocalDirectory(new File("sftp-inbound"));
        source.setAutoCreateLocalDirectory(true);
//        source.setLocalFilter(new AcceptOnceFileListFilter<File>());
//        source.setMaxFetchSize(1);
        return source;
    }

//    @ServiceActivator(inputChannel = "inboundSftpChannel")
//    public void handleIncomingFile(File file) throws IOException {
//        logger.info(String.format("handleIncomingFile BEGIN %s", file.getName()));
//        String content = FileUtils.readFileToString(file, "UTF-8");
//        logger.info(String.format("Content: %s", content));
//        logger.info(String.format("handleIncomingFile END %s", file.getName()));
//
//    }


    @Bean
    @ServiceActivator(inputChannel = "inboundSftpChannel")
    public MessageHandler inboundHandler() {
        return new MessageHandler() {

            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                System.out.println(message.getPayload());
            }

        };
    }

}
