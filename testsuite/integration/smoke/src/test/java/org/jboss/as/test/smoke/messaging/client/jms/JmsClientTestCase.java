/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.messaging.client.jms;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Demo using the AS management API to create and destroy a JMS queue.
 *
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JmsClientTestCase {

    private static final String QUEUE_NAME = "createdTestQueue";
    private static final String EXPORTED_QUEUE_NAME = "java:jboss/exported/createdTestQueue";

    @ContainerResource
    private Context remoteContext;

    @ContainerResource
    private ManagementClient managementClient;

    @Before
    public void setUp() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient);
        jmsOperations.createJmsQueue(QUEUE_NAME, EXPORTED_QUEUE_NAME);
    }

    @After
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
