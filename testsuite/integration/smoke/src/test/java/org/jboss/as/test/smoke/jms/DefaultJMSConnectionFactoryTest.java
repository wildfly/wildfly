/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.as.test.smoke.jms.auxiliary.SimplifiedMessageProducer;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests sending Jakarta Messaging messages using the server's default Jakarta Messaging Connection Factory.
 *
 * Jakarta EE 8 spec, §EE.5.20 Default Jakarta Messaging Connection Factory
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(CreateQueueSetupTask.class)
public class DefaultJMSConnectionFactoryTest {

    private static final Logger logger = Logger.getLogger(DefaultJMSConnectionFactoryTest.class);

    @EJB
    private SimplifiedMessageProducer producerEJB;

    @Resource(mappedName = "/queue/myAwesomeQueue")
    private Queue queue;

    @Resource
    private ConnectionFactory factory;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "DefaultJMSConnectionFactoryTest.jar")
                .addClass(SimplifiedMessageProducer.class)
                .addClass(CreateQueueSetupTask.class)
                .addPackage(JMSOperations.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE,
                        ArchivePaths.create("beans.xml"));
    }

    @Test
    public void sendWithDefaultJMSConnectionFactory() throws Exception {
        String text = UUID.randomUUID().toString();

        producerEJB.sendWithDefaultJMSConnectionFactory(queue, text);

        assertMessageInQueue(text);
    }

    @Test
    public void sendWithRegularConnectionFactory() throws Exception {
        String text = UUID.randomUUID().toString();

        producerEJB.sendWithRegularConnectionFactory(queue, text);

        assertMessageInQueue(text);
    }

    private void assertMessageInQueue(String text) throws JMSException {
        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(queue);
        connection.start();

        Message message = consumer.receive(2000);
        assertNotNull(message);
        assertTrue(message instanceof TextMessage);
        assertEquals(text, ((TextMessage) message).getText());

        connection.close();
    }
}
