/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms;

import static org.junit.jupiter.api.Assertions.*;

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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Jakarta Messaging bridge test.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
@ExtendWith(ArquillianExtension.class)
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
            assertNotNull(receivedMessage, "did not receive expected message");
            assertTrue(receivedMessage instanceof TextMessage);
            assertEquals(text, ((TextMessage) receivedMessage).getText());
            assertNull(receivedMessage.getStringProperty(ActiveMQJMSConstants.AMQ_MESSAGING_BRIDGE_MESSAGE_ID_LIST), "got header set by the Jakarta Messaging bridge");
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
