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

package org.jboss.as.test.smoke.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.as.test.smoke.jms.auxiliary.SimplifiedMessageProducer;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests sending JMS messages using the server's default JMS Connection Factory.
 *
 * Java EE 7 spec, §EE.5.20 Default JMS Connection Factory
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
@RunWith(Arquillian.class)
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
                        ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),
                        "MANIFEST.MF");
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
