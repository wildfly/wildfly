/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.messagedrivencontext;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.JMSAdminOperations;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the {@link javax.ejb.MessageDrivenBean#setMessageDrivenContext(javax.ejb.MessageDrivenContext)}
 * method is invoked on MDBs which implement the {@link javax.ejb.MessageDrivenBean} interface
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class SetMessageDrivenContextInvocationTestCase {

    private static JMSAdminOperations jmsAdminOperations;

    private static final String QUEUE_JNDI_NAME = "java:jboss/queue/set-message-context";
    private static final String REPLY_QUEUE_JNDI_NAME = "java:jboss/queue/set-message-context-reply-queue";

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = QUEUE_JNDI_NAME)
    private Queue queue;

    @Resource(mappedName = REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

    @Deployment
    public static Archive createDeployment() {
        // setup the JMS destinations
        createJmsDestinations();

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "set-message-driven-context-invocation-test.jar");
        jar.addClasses(SimpleMDB.class, JMSMessagingUtil.class, JMSAdminOperations.class);
        return jar;
    }

    private static void createJmsDestinations() {
        jmsAdminOperations = new JMSAdminOperations();
        jmsAdminOperations.createJmsQueue("setMessageDrivenContext/queue", QUEUE_JNDI_NAME);
        jmsAdminOperations.createJmsQueue("setMessageDrivenContext/replyQueue", REPLY_QUEUE_JNDI_NAME);
    }

    @AfterClass
    public static void afterTestClass() {
        try {
            jmsAdminOperations.removeJmsQueue("setMessageDrivenContext/queue");
            jmsAdminOperations.removeJmsQueue("setMessageDrivenContext/replyQueue");
        } finally {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.close();
            }
        }
    }

    /**
     * Test that the {@link javax.ejb.MessageDrivenBean#setMessageDrivenContext(javax.ejb.MessageDrivenContext)}
     * was invoked
     *
     * @throws Exception
     */
    @Test
    public void testSetMessageDrivenContext() throws Exception {
        this.jmsUtil.sendTextMessage("hello world", this.queue, this.replyQueue);
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        Assert.assertNotNull("Reply message was null on reply queue: " + this.replyQueue, reply);
        final TextMessage textMessage = (TextMessage) reply;
        Assert.assertEquals("setMessageDrivenContext method was *not* invoked on MDB", SimpleMDB.SUCCESS_REPLY, textMessage.getText());
    }
}
