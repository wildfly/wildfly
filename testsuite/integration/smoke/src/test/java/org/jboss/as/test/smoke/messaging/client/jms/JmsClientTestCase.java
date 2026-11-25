/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.messaging.client.jms;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static jakarta.jms.Session.AUTO_ACKNOWLEDGE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Demo using the AS management API to create and destroy a Jakarta Messaging queue.
 *
 * @author Emanuel Muckenhuber
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class JmsClientTestCase {

    private static final String QUEUE_NAME = "createdTestQueue";
    private static final String EXPORTED_QUEUE_NAME = "java:jboss/exported/createdTestQueue";

    @ContainerResource
    private Context remoteContext;

    @ContainerResource
    private ManagementClient managementClient;

    @BeforeEach
    public void setUp() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient);
        jmsOperations.createJmsQueue(QUEUE_NAME, EXPORTED_QUEUE_NAME);
    }

    @AfterEach
    public void tearDown() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient);
        jmsOperations.removeJmsQueue(QUEUE_NAME);
    }

    @Test
    public void testSendAndReceive() throws Exception {
        doSendAndReceive("jms/RemoteConnectionFactory");
    }

    private void doSendAndReceive(String connectionFactoryLookup) throws  Exception {
        Connection conn = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup(connectionFactoryLookup);
            assertNotNull(cf);
            Destination destination = (Destination) remoteContext.lookup(QUEUE_NAME);
            assertNotNull(destination);

            conn = cf.createConnection("guest", "guest");
            conn.start();
            Session consumerSession = conn.createSession(false, AUTO_ACKNOWLEDGE);

            final CountDownLatch latch = new CountDownLatch(10);
            final List<String> result = new ArrayList<String>();

            // Set the async listener
            MessageConsumer consumer = consumerSession.createConsumer(destination);
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

            final Session producerSession = conn.createSession(false, AUTO_ACKNOWLEDGE);
            MessageProducer producer = producerSession.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            for (int i = 0 ; i < 10 ; i++) {
                String s = "Test" + i;
                TextMessage msg = producerSession.createTextMessage(s);
                //System.out.println("sending " + s);
                producer.send(msg);
            }

            producerSession.close();

            assertTrue(latch.await(3, SECONDS));
            assertEquals(10, result.size());
            for (int i = 0 ; i < result.size() ; i++) {
                assertEquals("Test" + i, result.get(i));
            }
        } finally {
            try {
                conn.close();
            } catch (Exception ignore) {
            }
        }
    }
}
