/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.testcontainers.kafka.KafkaContainer;
import org.wildfly.security.manager.WildFlySecurityManager;

@DockerRequired
public class RunKafkaSetupTask implements ServerSetupTask {

    private static final PathElement BOOTSTRAP_SERVERS_PATH = PathElement.pathElement(SYSTEM_PROPERTY, "calculated.bootstrap.servers");

    volatile KafkaContainer container;
    volatile KafkaCompanion companion;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        String kafkaVersion = WildFlySecurityManager.getPropertyPrivileged("wildfly.test.kafka.version", null);
        if (kafkaVersion == null) {
            throw new IllegalArgumentException("Specify Kafka version with -Dwildfly.test.kafka.version");
        }
        container = new KafkaContainer("apache/kafka-native:" + kafkaVersion);
        container.setPortBindings(Arrays.asList("9092:9092", "9093:9093"));

        for (Map.Entry<String, String> entry : extraBrokerProperties().entrySet()) {
            container.addEnv(entry.getKey(), entry.getValue());
        }

        container.start();

        String bootstrapServers = container.getBootstrapServers();
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(BOOTSTRAP_SERVERS_PATH), Map.of(VALUE, new ModelNode(bootstrapServers)));
        ModelNode result =  managementClient.getControllerClient().execute(op);
        ModelTestUtils.checkOutcome(result);

        companion = new KafkaCompanion("INTERNAL://" + bootstrapServers);

        Map<String, Integer> topicsAndPartitions = getTopicsAndPartitions();
        if (topicsAndPartitions == null || topicsAndPartitions.isEmpty()) {
            companion.topics().createAndWait("testing", 1, Duration.of(10, ChronoUnit.SECONDS));
        } else {
            for (Map.Entry<String, Integer> entry : topicsAndPartitions.entrySet()) {
                companion.topics().createAndWait(entry.getKey(), entry.getValue(), Duration.of(10, ChronoUnit.SECONDS));
            }
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
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
    }



    protected Map<String, String> extraBrokerProperties() {
        return Collections.emptyMap();
    }

    protected Map<String, Integer> getTopicsAndPartitions() {
        return null;
    }

}
