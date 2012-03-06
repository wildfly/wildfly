/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import java.net.URL;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case based on reproducer for JBPAPP-7897 - RunAs role not propogated past first ejb3 method.
 *
 * @author Derek Horton, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RunAsTestCaseEJBMDB.RunAsTestCaseEJBMDBSetup.class)
public class RunAsTestCaseEJBMDB {
    private static final Logger log = Logger.getLogger(RunAsTestCaseEJBMDB.class.getName());

    private static final Integer REMOTE_PORT = 4447;

    private static final String QUEUE_NAME = "queue/TestQueue";

    @ArquillianResource
    @OperateOnDeployment("ejb2")
    URL baseUrl;

    @ContainerResource
    private InitialContext initialContext;

    static class RunAsTestCaseEJBMDBSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations ops = JMSOperationsProvider.getInstance(managementClient);
            ops.createJmsQueue(QUEUE_NAME, "java:jboss/exported/" + QUEUE_NAME);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations ops = JMSOperationsProvider.getInstance(managementClient);
            ops.removeJmsQueue(QUEUE_NAME);
        }
    }

    @Deployment(testable = false, managed = true, name = "ejb2", order = 1)
    public static Archive<?> runAsEJB2() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "runasmdbejb-ejb2.jar")
                .addClasses(
                        GoodBye.class,
                        GoodByeBean.class,
                        GoodByeHome.class,
                        GoodByeLocal.class,
                        GoodByeLocalHome.class);
        jar.addAsManifestResource(RunAsTestCaseEJBMDB.class.getPackage(), "ejb-jar-ejb2.xml", "ejb-jar.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(testable = false, managed = true, name = "ejb3", order = 1)
    public static Archive<?> runAsEJB3() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "runasmdbejb-ejb3.jar")
                .addClasses(
                        HelloBean.class,
                        Hello.class,
                        HolaBean.class,
                        Hola.class,
                        Howdy.class,
                        HowdyBean.class,
                        HelloMDB.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment.runasmdbejb-ejb2.jar  \n"), "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testEjb() throws Exception {
        Hello helloBean = (Hello) initialContext.lookup("runasmdbejb-ejb3/Hello!org.jboss.as.test.integration.ejb.security.runas.ejb2mdb.Hello");
        String hellomsg = helloBean.sayHello();
        log.info(hellomsg);
        Assert.assertEquals("Hello Fred! Howdy Fred! GoodBye user1", hellomsg);
    }

    @Test
    public void testSendMessage() throws Exception {
        ConnectionFactory cf = null;
        Connection connection = null;
        Session session = null;

        try {
            cf = (ConnectionFactory) initialContext.lookup("jms/RemoteConnectionFactory");
            Queue queue = (Queue) initialContext.lookup(QUEUE_NAME);
            connection = cf.createConnection("guest", "guest");
            connection.start(); //for consumer we need to start connection
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer sender = session.createProducer(queue);
            TemporaryQueue replyQueue = session.createTemporaryQueue();
            TextMessage message = session.createTextMessage("hello goodbye");
            message.setJMSReplyTo(replyQueue);
            sender.send(message);
            log.info("testSendMessage(): Message sent!");

            MessageConsumer consumer = session.createConsumer(replyQueue);
            Message replyMsg = consumer.receive(5000);
            Assert.assertNotNull(replyMsg);
            Assert.assertTrue(replyMsg instanceof TextMessage);
            String actual = ((TextMessage) replyMsg).getText();
            Assert.assertEquals("Howdy Fred! GoodBye user1", actual);
        } finally {
            if(session != null) {
                session.close();
            }
            closeConnection(connection);
        }
    }

    private void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (JMSException jmse) {
            System.out.println("connection close failed: " + jmse);
        }
    }

}
