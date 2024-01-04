/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.test.EmbeddedKafkaBroker;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunKafkaSetupTask implements ServerSetupTask {
    volatile EmbeddedKafkaBroker broker;
    volatile KafkaCompanion companion;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        try {
            broker = new EmbeddedKafkaBroker()
                    .withNodeId(0)
                    .withKafkaPort(9092)
                    .withDeleteLogDirsOnClose(true);

            broker.withAdditionalProperties(props -> addBrokerProperties(props));
            broker = augmentKafkaBroker(broker);
            broker.start();

            companion = new KafkaCompanion(broker.getAdvertisedListeners());


            for (Map.Entry<String, Integer> topicAndPartition : getTopicsAndPartitions().entrySet()) {
                companion.topics().createAndWait(topicAndPartition.getKey(), topicAndPartition.getValue(), Duration.of(10, ChronoUnit.SECONDS));
            }
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

    protected EmbeddedKafkaBroker augmentKafkaBroker(EmbeddedKafkaBroker broker) {
        return broker;
    }

    protected void addBrokerProperties(Properties brokerProperties) {

    }

    protected Map<String, Integer> getTopicsAndPartitions() {
        return Collections.singletonMap("testing", 1);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        if (companion != null) {
            companion.close();
        }
        if (broker != null) {
            broker.close();
        }
    }
}