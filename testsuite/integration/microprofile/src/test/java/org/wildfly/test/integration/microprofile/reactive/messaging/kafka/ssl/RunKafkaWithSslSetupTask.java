/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.test.EmbeddedKafkaBroker;
import org.apache.kafka.common.Endpoint;
import org.apache.kafka.common.config.SslConfigs;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.smallrye.reactive.messaging.kafka.companion.test.EmbeddedKafkaBroker.endpoint;
import static org.apache.kafka.common.security.auth.SecurityProtocol.PLAINTEXT;
import static org.apache.kafka.common.security.auth.SecurityProtocol.SSL;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunKafkaWithSslSetupTask implements ServerSetupTask {
    private static final String SERVER_KEYSTORE = "target/reactive-messaging-kafka/server.keystore.p12";
    private static final String SERVER_CER = "target/reactive-messaging-kafka/server.cer";
    private static final String SERVER_TRUSTSTORE = "target/reactive-messaging-kafka/server.truststore.p12";
    public static final String CLIENT_TRUSTSTORE = "target/reactive-messaging-kafka/client.truststore.p12";
    private static final String KEYSTORE_PWD = "serverks";
    private static final String SERVER_TRUESTSTORE_PWD = "serverts";
    public static final String CLIENT_TRUESTSTORE_PWD = "clientts";
    volatile EmbeddedKafkaBroker broker;
    volatile KafkaCompanion companion;
    private static final Logger log = Logger.getLogger(RunKafkaWithSslSetupTask.class);
    private File configDir;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        try {
            configDir = new File("target", "reactive-messaging-kafka");
            configDir.mkdir();

            //keytool -genkeypair -alias localhost -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore server.keystore.p12 -validity 3650  -ext SAN=DNS:localhost,IP:127.0.0.1
            final List<String> createKeyStoreWithCertificateCommand = new ArrayList<>(List.of("keytool", "-genkeypair"));
            createKeyStoreWithCertificateCommand.addAll(List.of("-alias", "localhost"));
            createKeyStoreWithCertificateCommand.addAll(List.of("-keyalg", "RSA"));
            createKeyStoreWithCertificateCommand.addAll(List.of("-keysize", "2048"));
            createKeyStoreWithCertificateCommand.addAll(List.of("-storetype", "PKCS12"));
            createKeyStoreWithCertificateCommand.addAll(List.of("-keystore", SERVER_KEYSTORE));
            createKeyStoreWithCertificateCommand.addAll(List.of("-storepass", KEYSTORE_PWD));
            createKeyStoreWithCertificateCommand.addAll(List.of("-keypass", KEYSTORE_PWD));
            createKeyStoreWithCertificateCommand.addAll(
                    List.of("-dname", "CN=localhost, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown"));
            createKeyStoreWithCertificateCommand.addAll(List.of("-validity", "3650"));
            createKeyStoreWithCertificateCommand.addAll(List.of("-ext", "SAN=DNS:localhost,IP:127.0.0.1"));
            runKeytoolCommand(createKeyStoreWithCertificateCommand);

            //keytool -exportcert -alias localhost -keystore server.keystore.p12 -file server.cer -storetype pkcs12 -noprompt -storepass serverks
            final List<String> exportCertificateCommand = new ArrayList<>(List.of("keytool", "-exportcert"));
            exportCertificateCommand.addAll(List.of("-alias", "localhost"));
            exportCertificateCommand.addAll(List.of("-keystore", SERVER_KEYSTORE));
            exportCertificateCommand.addAll(List.of("-file", SERVER_CER));
            exportCertificateCommand.addAll(List.of("-storetype", "PKCS12"));
            exportCertificateCommand.addAll(List.of("-noprompt"));
            exportCertificateCommand.addAll(List.of("-storepass", KEYSTORE_PWD));
            runKeytoolCommand(exportCertificateCommand);

            //keytool -keystore server.truststore.p12 -alias localhost -importcert -file server.cer -storetype pkcs12
            final List<String> importServerTrustStoreCommand = new ArrayList<>(List.of("keytool"));
            importServerTrustStoreCommand.addAll(List.of("-keystore", SERVER_TRUSTSTORE));
            importServerTrustStoreCommand.addAll(List.of("-alias", "localhost"));
            importServerTrustStoreCommand.addAll(List.of("-importcert"));
            importServerTrustStoreCommand.addAll(List.of("-file", SERVER_CER));
            importServerTrustStoreCommand.addAll(List.of("-storetype", "PKCS12"));
            importServerTrustStoreCommand.addAll(List.of("-storepass", SERVER_TRUESTSTORE_PWD));
            importServerTrustStoreCommand.addAll(List.of("-keypass", SERVER_TRUESTSTORE_PWD));
            importServerTrustStoreCommand.addAll(List.of("-noprompt"));
            runKeytoolCommand(importServerTrustStoreCommand);

            //keytool -keystore client.truststore.p12 -alias localhost -importcert -file server.cer -storetype pkcs12
            final List<String> importClientTrustStoreCommand = new ArrayList<>(List.of("keytool"));
            importClientTrustStoreCommand.addAll(List.of("-keystore", CLIENT_TRUSTSTORE));
            importClientTrustStoreCommand.addAll(List.of("-alias", "localhost"));
            importClientTrustStoreCommand.addAll(List.of("-importcert"));
            importClientTrustStoreCommand.addAll(List.of("-file", SERVER_CER));
            importClientTrustStoreCommand.addAll(List.of("-storetype", "PKCS12"));
            importClientTrustStoreCommand.addAll(List.of("-storepass", CLIENT_TRUESTSTORE_PWD));
            importClientTrustStoreCommand.addAll(List.of("-keypass", CLIENT_TRUESTSTORE_PWD));
            importClientTrustStoreCommand.addAll(List.of("-noprompt"));
            runKeytoolCommand(importClientTrustStoreCommand);

            Endpoint external = endpoint("EXTERNAL", SSL, "localhost", 9092);
            Endpoint internal = endpoint("INTERNAL", PLAINTEXT, "localhost", 19002);
            broker = new EmbeddedKafkaBroker()
                    .withAdvertisedListeners(external, internal)
                    .withAdditionalProperties(properties -> {
                        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, SERVER_KEYSTORE);
                        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, KEYSTORE_PWD);
                        properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, KEYSTORE_PWD);
                        properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
                        properties.put(SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG, "SHA1PRNG");
                    })
                    .withDeleteLogDirsOnClose(true);
            broker.start();

            companion = new KafkaCompanion(EmbeddedKafkaBroker.toListenerString(internal));
            companion.topics().createAndWait("testing", 1, Duration.of(10, ChronoUnit.SECONDS));
        } catch (Exception e) {
            try {
                if (companion != null) {
                    companion.close();
                }
                if (broker != null) {
                    broker.close();
                }
            } finally {
                throw e;
            }
        }
    }

    private void runKeytoolCommand(List<String> commandParameters) throws IOException {
        ProcessBuilder keytool = new ProcessBuilder().command(commandParameters);
        final Process process = keytool.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            log.errorf(e, "Keytool execution error");
        }

        log.debugf("Generating certificate using `keytool` using command: %s, parameters: %s", process.info(), commandParameters);

        if (process.exitValue() > 0) {
            final String processError = (new BufferedReader(new InputStreamReader(process.getErrorStream()))).lines()
                    .collect(Collectors.joining(" \\ "));
            final String processOutput = (new BufferedReader(new InputStreamReader(process.getInputStream()))).lines()
                    .collect(Collectors.joining(" \\ "));
            log.errorf("Error generating certificate, error output: %s, normal output: %s, commandline parameters: %s", processError, processOutput, commandParameters);
        }
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        if (companion != null) {
            companion.close();
        }
        if (broker != null) {
            broker.close();
        }
        if (configDir != null && Files.exists(Paths.get(configDir.getPath()))) {
            boolean deleted = deleteDirectory(configDir);
            if (!deleted) {
                log.warnf("Deleting config directory %s was not successful", configDir.toString());
            }
        }
    }
}