/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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