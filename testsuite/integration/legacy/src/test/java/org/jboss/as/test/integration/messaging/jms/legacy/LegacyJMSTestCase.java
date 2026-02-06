/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.legacy;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that a legacy (HornetQ) clients can look up Jakarta Messaging resources managed by the messaging-activemq subsystem
 * when they look up a legacy entry.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LegacyJMSTestCase {

    private static final String QUEUE_NAME = "LegacyJMSTestCase-Queue";
    private static final String QUEUE_ENTRY = "java:jboss/exported/jms/" + QUEUE_NAME;
    private static final String LEGACY_QUEUE_LOOKUP = "legacy/jms/" + QUEUE_NAME;
    private static final String LEGACY_QUEUE_ENTRY = "java:jboss/exported/" + LEGACY_QUEUE_LOOKUP;

    private static final String TOPIC_NAME = "LegacyJMSTestCase-Topic";
    private static final String TOPIC_ENTRY = "java:jboss/exported/jms/" + TOPIC_NAME;
    private static final String LEGACY_TOPIC_LOOKUP = "legacy/jms/" + TOPIC_NAME;
    private static final String LEGACY_TOPIC_ENTRY = "java:jboss/exported/" + LEGACY_TOPIC_LOOKUP;

    private static final String CF_NAME = "LegacyJMSTestCase-CF";
    private static final String LEGACY_CF_LOOKUP = "legacy/jms/" + CF_NAME;
    private static final String LEGACY_CF_ENTRY = "java:jboss/exported/" + LEGACY_CF_LOOKUP;

    @ContainerResource
    private Context remoteContext;

    @ContainerResource
    private ManagementClient managementClient;

    @BeforeClass
    public static void ignoreOnWindows() {
        if (System.getProperty("os.name", null).toLowerCase(Locale.ENGLISH).contains("windows")) {
            // this isn't actually an intermittent failure, but might as well allow it to be enabled via the IntermittentFailure sys prop
            IntermittentFailure.thisTestIsFailingIntermittently("WFLY-21350");
        }
    }

    @Before
    public void setUp() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

        ModelNode queueAttributes = new ModelNode();
        queueAttributes.get("legacy-entries").add(LEGACY_QUEUE_ENTRY);
        jmsOperations.createJmsQueue(QUEUE_NAME, QUEUE_ENTRY, queueAttributes);

        ModelNode topicAttributes = new ModelNode();
        topicAttributes.get("legacy-entries").add(LEGACY_TOPIC_ENTRY);
        jmsOperations.createJmsTopic(TOPIC_NAME, TOPIC_ENTRY, topicAttributes);

        ModelNode legacyConnectionFactoryAddress = jmsOperations.getServerAddress().clone()
                .add("legacy-connection-factory", CF_NAME);
        ModelNode addLegacyConnectionFactoryOp = Util.createOperation(ModelDescriptionConstants.ADD, PathAddress.pathAddress(legacyConnectionFactoryAddress));
        addLegacyConnectionFactoryOp.get("connectors").add("http-connector");
        addLegacyConnectionFactoryOp.get("entries").add(LEGACY_CF_ENTRY);
        managementClient.getControllerClient().execute(addLegacyConnectionFactoryOp);
    }

    @After
    public void tearDown() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

        jmsOperations.removeJmsQueue(QUEUE_NAME);
        jmsOperations.removeJmsTopic(TOPIC_NAME);

        ModelNode legacyConnectionFactoryAddress = jmsOperations.getServerAddress().clone()
                .add("legacy-connection-factory", CF_NAME);
        ModelNode addLegacyConnectionFactoryOp = Util.createOperation(REMOVE, PathAddress.pathAddress(legacyConnectionFactoryAddress));
        managementClient.getControllerClient().execute(addLegacyConnectionFactoryOp);
    }

    @Test
    public void testSendAndReceiveFromLegacyQueue() throws Exception {
        doSendAndReceive(LEGACY_CF_LOOKUP, LEGACY_QUEUE_LOOKUP);
    }

    @Test
    public void testSendAndReceiveFromLegacyTopic() throws Exception {
        doSendAndReceive(LEGACY_CF_LOOKUP, LEGACY_TOPIC_LOOKUP);
    }

    private void doSendAndReceive(String connectionFactoryLookup, String destinationLoookup) throws Exception {
        Destination destination = (Destination) remoteContext.lookup(destinationLoookup);
        assertNotNull(destination);
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup(connectionFactoryLookup);
        assertNotNull(cf);

        try (
                JMSContext producerContext = cf.createContext("guest", "guest");
                JMSContext consumerContext = cf.createContext("guest", "guest")
        ) {
            final CountDownLatch latch = new CountDownLatch(10);
            final List<String> result = new ArrayList<String>();

            JMSConsumer consumer = consumerContext.createConsumer(destination);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    TextMessage msg = (TextMessage) message;
                    try {
                        result.add(msg.getText());
                        latch.countDown();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            JMSProducer producer = producerContext.createProducer();
            for (int i = 0; i < 10; i++) {
                String text = "Test" + i;
                producer.send(destination, text);
            }

            assertTrue(latch.await(3, SECONDS));
            assertEquals(10, result.size());
            for (int i = 0; i < result.size(); i++) {
                assertEquals("Test" + i, result.get(i));
            }
        }
    }
}
