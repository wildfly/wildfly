/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.apache.activemq.artemis.api.jms.ActiveMQJMSConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Jakarta Messaging bridge test.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(DefaultCreateJMSBridgeSetupTask.class)
public class DefaultJMSBridgeTest {

    private static final Logger logger = Logger.getLogger(DefaultJMSBridgeTest.class);

    private static final String MESSAGE_TEXT = "Hello world!";

    @Resource(mappedName = "/queue/myAwesomeQueue")
    private Queue sourceQueue;

    @Resource(mappedName = "/queue/myAwesomeQueue2")
    private Queue targetQueue;

    @Resource(mappedName = "/myAwesomeCF")
    private ConnectionFactory factory;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addPackage(JMSOperations.class.getPackage())
                .addClass(DefaultCreateJMSBridgeSetupTask.class)
                .addClass(AbstractCreateJMSBridgeSetupTask.class)
                .addClass(CreateQueueSetupTask.class)
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        ArchivePaths.create("beans.xml"));
    }

    /**
     * Send a message on the source queue
     * Consumes it on the target queue
     *
     * The test will pass since a Jakarta Messaging Bridge has been created to bridge the source destination to the target destination.
     */
    @Test
    public void sendAndReceiveMessage() throws Exception {
        Connection connection = null;
        Session session = null;
        Message receivedMessage = null;

        try {
            // SEND A MESSAGE on the source queue
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(sourceQueue);
            String text = MESSAGE_TEXT + " " + UUID.randomUUID().toString();
            Message message = session.createTextMessage(text);
            message.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(message);
            connection.start();
            connection.close();

            // RECEIVE THE MESSAGE from the target queue
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(targetQueue);
            connection.start();
            receivedMessage = consumer.receive(5000);

            // ASSERTIONS
            assertNotNull("did not receive expected message", receivedMessage);
            assertTrue(receivedMessage instanceof TextMessage);
            assertEquals(text, ((TextMessage) receivedMessage).getText());
            assertTrue("got header set by the Jakarta Messaging bridge", receivedMessage.getStringProperty(ActiveMQJMSConstants.AMQ_MESSAGING_BRIDGE_MESSAGE_ID_LIST) == null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // CLEANUP
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

    }
}
