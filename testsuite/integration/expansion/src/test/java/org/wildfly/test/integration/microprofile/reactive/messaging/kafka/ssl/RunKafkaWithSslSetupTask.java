/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.wildfly.test.integration.microprofile.reactive.KeystoreUtil.SERVER_KEYSTORE_PATH;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.test.integration.microprofile.reactive.KeystoreUtil;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@DockerRequired
public class RunKafkaWithSslSetupTask implements ServerSetupTask {

    private static final PathElement BOOTSTRAP_SERVERS_PATH = PathElement.pathElement(SYSTEM_PROPERTY, "calculated.bootstrap.servers");
    private static final String DOCKER_HOST_PLACEHOLDER = "$DOCKER_HOST";

    volatile GenericContainer container;
    volatile KafkaCompanion companion;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        try {
            String dockerHost = DockerClientFactory.instance().dockerHostIpAddress();
            KeystoreUtil.createKeystores(dockerHost);
            String kafkaVersion = WildFlySecurityManager.getPropertyPrivileged("wildfly.test.kafka.version", null);
            if (kafkaVersion == null) {
                throw new IllegalArgumentException("Specify Kafka version with -Dwildfly.test.kafka.version");
            }

            // The KafkaContainer class doesn't play nicely when trying to make it use SSL
            container = new GenericContainer("apache/kafka-native:" + kafkaVersion);
            container.setPortBindings(Arrays.asList("9092:9092", "19092:19092"));
            container.withCopyToContainer(
                    getKafkaServerProperties(dockerHost),
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

            String bootstrapServers = String.format("%s:%s", container.getHost(), container.getMappedPort(9092));
            ModelNode op = Util.createAddOperation(PathAddress.pathAddress(BOOTSTRAP_SERVERS_PATH), Map.of(VALUE, new ModelNode(bootstrapServers)));
            ModelNode result =  managementClient.getControllerClient().execute(op);
            ModelTestUtils.checkOutcome(result);

            companion = new KafkaCompanion("INTERNAL://" + container.getHost() + ":19092");
            companion.topics().createAndWait("testing", 1, Duration.of(10, ChronoUnit.SECONDS));
        } catch (Exception e) {
            cleanupKafka(managementClient);
            throw e;
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        cleanupKafka(managementClient);
    }

    private void cleanupKafka(ManagementClient managementClient) throws IOException {
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
            ModelNode op = Util.createRemoveOperation(PathAddress.pathAddress(BOOTSTRAP_SERVERS_PATH));
            managementClient.getControllerClient().execute(op);
        } finally {
            KeystoreUtil.cleanUp();
        }
    }

    private Transferable getKafkaServerProperties(String dockerHost) {
        InputStream inputStream = Objects.requireNonNull(RunKafkaWithSslSetupTask.class.getResourceAsStream("server.properties"));
        String serverProperties = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"));
        String resolvedServerProperties = serverProperties.replace(DOCKER_HOST_PLACEHOLDER, dockerHost);
        return Transferable.of(resolvedServerProperties);
    }

}