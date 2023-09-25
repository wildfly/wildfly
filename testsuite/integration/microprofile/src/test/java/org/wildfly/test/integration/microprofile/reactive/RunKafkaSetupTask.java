/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.test.EmbeddedKafkaBroker;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunKafkaSetupTask implements ServerSetupTask {
    volatile EmbeddedKafkaBroker broker;
    volatile KafkaCompanion companion;

    final boolean ipv6 = WildFlySecurityManager.doChecked(
            (PrivilegedAction<Boolean>) () -> System.getProperties().containsKey("ipv6"));
    protected final String LOOPBACK = ipv6 ? "[::1]" : "127.0.0.1";

    private final Logger logger = Logger.getLogger("RunKafkaSetupTask");

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

//    protected String[] getTopics() {
//        return new String[]{"testing"};
//    }
//
//    protected int getPartitions() {
//        return 1;
//    }

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