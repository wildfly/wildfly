/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import static org.wildfly.test.integration.microprofile.reactive.KeystoreUtil.SERVER_KEYSTORE_PATH;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.test.integration.microprofile.reactive.KeystoreUtil;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@TestcontainersRequired
public class RunKafkaWithSslSetupTask implements ServerSetupTask {
    volatile GenericContainer container;
    volatile KafkaCompanion companion;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        try {
            KeystoreUtil.createKeystores();
            String kafkaVersion = WildFlySecurityManager.getPropertyPrivileged("wildfly.test.kafka.version", null);
            if (kafkaVersion == null) {
                throw new IllegalArgumentException("Specify Kafka version with -Dwildfly.test.kafka.version");
            }

            // The KafkaContainer class doesn't play nicely when trying to make it use SSL
            container = new GenericContainer("apache/kafka-native:" + kafkaVersion);
            container.setPortBindings(Arrays.asList("9092:9092", "19092:19092"));
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("src/test/resources/org/wildfly/test/integration/microprofile/reactive/messaging/kafka/ssl/server.properties")),
                    "/mnt/shared/config/server.properties"
            );
            container.waitingFor(Wait.forLogMessage(".*Transitioning from RECOVERY to RUNNING.*", 1));


            // Copy the keystore files to the expected container location
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(SERVER_KEYSTORE_PATH.getParent()),
                    "/etc/kafka/secrets/");

//            // Set env vars which don't seem to have any effect when only in server.properties
            container.addEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@localhost:29093");

            container.start();

            companion = new KafkaCompanion("INTERNAL://localhost:19092");
            companion.topics().createAndWait("testing", 1, Duration.of(10, ChronoUnit.SECONDS));
        } catch (Exception e) {
            cleanupKafka();
            throw e;
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        cleanupKafka();
    }

    private void cleanupKafka() throws IOException {
        try {
            if (companion != null) {
                try {
                    companion.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (container != null) {
                try {
                    container.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            KeystoreUtil.cleanUp();
        }
    }
}