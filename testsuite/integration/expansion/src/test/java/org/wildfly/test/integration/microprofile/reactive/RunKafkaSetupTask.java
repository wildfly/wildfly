/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.config.ContainerConfig;
import org.testcontainers.kafka.KafkaContainer;
import org.wildfly.security.manager.WildFlySecurityManager;

@TestcontainersRequired
public class RunKafkaSetupTask implements ServerSetupTask {
    volatile KafkaContainer container;
    volatile KafkaCompanion companion;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        String kafkaVersion = ContainerConfig.KAFKA.getImageVersion() != null ? ContainerConfig.KAFKA.getImageVersion() : WildFlySecurityManager.getPropertyPrivileged("wildfly.test.kafka.version", null);
        if (kafkaVersion == null) {
            throw new IllegalArgumentException("Specify Kafka version with -Dwildfly.test.kafka.version");
        }
        container = new KafkaContainer(ContainerConfig.KAFKA.getImageName() + ":" + kafkaVersion);
        container.setPortBindings(Arrays.asList("9092:9092", "9093:9093"));

        for (Map.Entry<String, String> entry : extraBrokerProperties().entrySet()) {
            container.addEnv(entry.getKey(), entry.getValue());
        }

        container.start();

        companion = new KafkaCompanion("INTERNAL://localhost:9092");

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
    }



    protected Map<String, String> extraBrokerProperties() {
        return Collections.emptyMap();
    }

    protected Map<String, Integer> getTopicsAndPartitions() {
        return null;
    }

}
