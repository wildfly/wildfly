/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.messagelistener;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;

/**
 * Tests that if a message listener interface is implemented by the base class of a message driven bean, then
 * it's taken into consideration while checking for message listener interface for the MDB.
 *
 * <p>See https://issues.jboss.org/browse/AS7-2638</p>
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup({MessageListenerInClassHierarchyTestCase.JmsQueueSetup.class})
public class MessageListenerInClassHierarchyTestCase {

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("messagelistener/queue", QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("messagelistener/replyQueue", REPLY_QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("messagelistener/queue");
                jmsAdminOperations.removeJmsQueue("messagelistener/replyQueue");
                jmsAdminOperations.close();
            }
        }
    }

    private static final String QUEUE_JNDI_NAME = "java:jboss/queue/message-listener";
    private static final String REPLY_QUEUE_JNDI_NAME = "java:jboss/queue/message-listener-reply-queue";

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = QUEUE_JNDI_NAME)
    private Queue queue;

    @Resource(mappedName = REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "message-listener-in-class-hierarchy-test.jar");
        jar.addClasses(ConcreteMDB.class, CommonBase.class, JMSMessagingUtil.class, JmsQueueSetup.class);
        jar.addPackage(JMSOperations.class.getPackage());
        return jar;
    }


    /**
     * Test that if a {@link javax.jms.MessageListener} interface is implemented by the base class of a
     * message driven bean, then it's taken into consideration while looking for potential message listener
     * interface
     * <p>See https://issues.jboss.org/browse/AS7-2638</p>
     *
     * @throws Exception
     */
    @Test
    public void testSetMessageDrivenContext() throws Exception {
        this.jmsUtil.sendTextMessage("hello world", this.queue, this.replyQueue);
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        Assert.assertNotNull("Reply message was null on reply queue: " + this.replyQueue, reply);
        final TextMessage textMessage = (TextMessage) reply;
        Assert.assertEquals("setMessageDrivenContext method was *not* invoked on MDB", ConcreteMDB.SUCCESS_REPLY, textMessage.getText());
    }
}
