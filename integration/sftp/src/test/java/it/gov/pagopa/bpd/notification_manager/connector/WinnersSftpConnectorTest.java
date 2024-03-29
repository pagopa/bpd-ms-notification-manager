package it.gov.pagopa.bpd.notification_manager.connector;

import it.gov.pagopa.bpd.notification_manager.connector.config.WinnersSftpChannelConfig;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(initializers = WinnersSftpConnectorTest.RandomPortInitializer.class,
        classes = {
                WinnersSftpChannelConfig.class,
                WinnersSftpChannelConfig.WinnersSftpGateway.class,
                WinnersSftpConnector.class
        })
@EnableIntegration
public class WinnersSftpConnectorTest {

    static SshServer server;

    static Path folderPath;

    @Autowired
    WinnersSftpConnector winnersSftpConnector;

    @BeforeClass
    public static void serverStartup() throws Exception {
        PublicKey publicKey = decodePublicKey();
        folderPath = Files.createTempDirectory("SFTP_TEMP");
        server = SshServer.setUpDefaultServer();
        server.setPort(10022);
        server.setPublickeyAuthenticator(
                (username, key, session) -> key.equals(publicKey)
        );
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
                new File(Files.createTempDirectory("SFTP_TEMP_KEY").toFile()
                        .getAbsolutePath() + "/hostkey.ser").toPath()));
        server.setSubsystemFactories(Collections.singletonList(
                new SftpSubsystemFactory()));
        server.setFileSystemFactory(new VirtualFileSystemFactory(folderPath));
        server.start();
    }

    private static PublicKey decodePublicKey() throws Exception {
        InputStream stream = new ClassPathResource("test_sftp/test.pub").getInputStream();
        byte[] decodeBuffer = Base64.decodeBase64(StreamUtils.copyToByteArray(stream));
        ByteBuffer buffer = ByteBuffer.wrap(decodeBuffer);
        int len = buffer.getInt();
        byte[] type = new byte[len];
        buffer.get(type);
        if ("ssh-rsa".equals(new String(type))) {
            BigInteger publicExponent = recoverBigInteger(buffer);
            BigInteger modulus = recoverBigInteger(buffer);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, publicExponent);
            return KeyFactory.getInstance("RSA").generatePublic(spec);

        } else {
            throw new IllegalArgumentException("Only supports RSA");
        }
    }

    private static BigInteger recoverBigInteger(ByteBuffer buffer) {
        int len = buffer.getInt();
        byte[] bytes = new byte[len];
        buffer.get(bytes);
        return new BigInteger(bytes);
    }

    @SneakyThrows
    @Before
    @After
    public void cleanSftpFolder() {
        Files.walk(folderPath)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @SneakyThrows
    @Test
    public void test_sftp_ok() {
        Path tempFile = Files.createTempFile("test", ".csv");
        assertEquals(0, Files.list(folderPath).count());
        winnersSftpConnector.sendFile(tempFile.toFile());
        List<Path> paths = Files.list(folderPath).collect(Collectors.toList());
        assertEquals(1, paths.size());
        assertEquals(tempFile.getFileName(), paths.get(0).getFileName());
    }

    @SneakyThrows
    @AfterClass
    public static void stopServer() {
        if (server.isStarted()) {
            server.close();
        }
    }


    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            InputStream inputStream = WinnersSftpConnectorTest.class.getClassLoader().getResourceAsStream("test_sftp/test");
            String privateKey = new BufferedReader(
                    new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            TestPropertySourceUtils
                    .addInlinedPropertiesToEnvironment(applicationContext,
                            String.format("NOTIFICATION_SFTP_PRIVATE_KEY=%s",
                                    privateKey.replaceAll("\n", "\\\\n"))
                    );
        }
    }

}