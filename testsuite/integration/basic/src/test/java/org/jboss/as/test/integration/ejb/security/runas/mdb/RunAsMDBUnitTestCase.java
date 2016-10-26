/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security.runas.mdb;

import org.jboss.logging.Logger;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Make sure the run-as on a MDB is picked up.
 * Part of migration test from EAP5 (JBAS-6239) to AS7 [JBQA-5275].
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@ServerSetup({RunAsMDBUnitTestCase.JmsQueueSetup.class})
@Category(CommonCriteria.class)
public class RunAsMDBUnitTestCase {
    private static final Logger log = Logger.getLogger(RunAsMDBUnitTestCase.class.getName());
    private static final String queueName = "queue/mdbtest";

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue(queueName, "java:jboss/" + queueName);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue(queueName);
                jmsAdminOperations.close();
            }
        }
    }

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "runas-mdb.jar")
                .addPackage(RunAsMDBUnitTestCase.class.getPackage())
                .addPackage(JMSOperations.class.getPackage())
                .addClass(JmsQueueSetup.class)
                .addAsResource(RunAsMDBUnitTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(RunAsMDBUnitTestCase.class.getPackage(), "roles.properties", "roles.properties");
        jar.addAsManifestResource(RunAsMDBUnitTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr \n"), "MANIFEST.MF");
        jar.addPackage(CommonCriteria.class.getPackage());
        return jar;
    }

    protected <T> T lookup(String name, Class<T> cls) throws Exception {
        return cls.cast(ctx.lookup(name));
    }

    @Test
    public void testSendMessage() throws Exception {
        ConnectionFactory connFactory = lookup("ConnectionFactory", ConnectionFactory.class);
        Connection conn = connFactory.createConnection();
        conn.start();
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        TemporaryQueue replyQueue = session.createTemporaryQueue();
        TextMessage msg = session.createTextMessage("Hello world");
        msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        msg.setJMSReplyTo(replyQueue);
        Queue queue = lookup("java:jboss/" + queueName, Queue.class);
        MessageProducer producer = session.createProducer(queue);
        producer.send(msg);
        MessageConsumer consumer = session.createConsumer(replyQueue);
        Message replyMsg = consumer.receive(5000);
        Assert.assertNotNull(replyMsg);
        if (replyMsg instanceof ObjectMessage) {
            Exception e = (Exception) ((ObjectMessage) replyMsg).getObject();
            throw e;
        }
        Assert.assertTrue(replyMsg instanceof TextMessage);
        String actual = ((TextMessage) replyMsg).getText();
        Assert.assertEquals("SUCCESS", actual);

        consumer.close();
        producer.close();
        session.close();
        conn.close();
    }
}
