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
package org.jboss.as.test.smoke.messaging.jms;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.setup.TestQueueSetupTask;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [TODO]
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(TestQueueSetupTask.class)
public class JmsTestCase {
    static final Logger log = Logger.getLogger(JmsTestCase.class);

    private QueueConnection conn;
    private Queue queue;
    private QueueSession session;

    CountDownLatch latch = new CountDownLatch(1);
    private volatile String received;


    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jms-example.jar")
        .addPackage(JmsTestCase.class.getPackage())
        .addPackage(TestQueueSetupTask.class.getPackage());
        return archive;
    }

    @Before
    public void start() throws Exception {
        //FIXME Arquillian Alpha bug - it also wants to execute this on the client despite this test being IN_CONTAINER
        if (!isInContainer()) {
            return;
        }

        InitialContext ctx = new InitialContext();

        QueueConnectionFactory qcf = (QueueConnectionFactory)ctx.lookup("java:/ConnectionFactory");
        conn = qcf.createQueueConnection();
        conn.start();
        queue = (Queue)ctx.lookup("queue/test");
        session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

        // Set the async listener
        QueueReceiver recv = session.createReceiver(queue);
        recv.setMessageListener(new ExampeMessageListener());
    }

    @After
    public void stop() throws Exception {
        //FIXME Arquillian Alpha bug - it also wants to execute this on the client despite this test being IN_CONTAINER
        if (!isInContainer()) {
            return;
        }

        if (conn != null) {
            conn.stop();
        }
        if (session != null) {
            session.close();
        }
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    public void testJms() throws Exception {
        sendMessage("Test");
        Assert.assertTrue(latch.await(3, TimeUnit.SECONDS));
        Assert.assertEquals("Test", received);
    }

    private void sendMessage(String txt) throws Exception {
        QueueSender send = null;
        try {
            // Send a text msg
            send = session.createSender(queue);
            TextMessage tm = session.createTextMessage(txt);
            send.send(tm);
            log.info("-----> sent text=" + tm.getText());
        } finally {
            send.close();
        }
    }

    private class ExampeMessageListener implements MessageListener {

        @Override
        public void onMessage(Message message) {
            TextMessage msg = (TextMessage)message;
            try {
                log.info("-----> on message: " + msg.getText());
                received = msg.getText();
                latch.countDown();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private boolean isInContainer() {
        ClassLoader cl = this.getClass().getClassLoader();
        if (cl instanceof ModuleClassLoader == false) {
            return false;
        }
        ModuleClassLoader mcl = (ModuleClassLoader)cl;
        ModuleIdentifier surefireModule = ModuleIdentifier.fromString("jboss.surefire.module");
        if (surefireModule.equals(mcl.getModule().getIdentifier())) {
            return false;
        }
        return true;
    }
}
