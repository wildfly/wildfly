/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagedrivencontext;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.PropertyPermission;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests that the {@link jakarta.ejb.MessageDrivenBean#setMessageDrivenContext(jakarta.ejb.MessageDrivenContext)}
 * method is invoked on MDBs which implement the {@link jakarta.ejb.MessageDrivenBean} interface
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup({SetMessageDrivenContextInvocationTestCase.JmsQueueSetup.class})
public class SetMessageDrivenContextInvocationTestCase {

    private static final String QUEUE_JNDI_NAME = "java:jboss/queue/set-message-context";
    private static final String REPLY_QUEUE_JNDI_NAME = "java:jboss/queue/set-message-context-reply-queue";

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("setMessageDrivenContext/queue", QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("setMessageDrivenContext/replyQueue", REPLY_QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("setMessageDrivenContext/queue");
                jmsAdminOperations.removeJmsQueue("setMessageDrivenContext/replyQueue");
                jmsAdminOperations.close();
            }
        }
    }

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = QUEUE_JNDI_NAME)
    private Queue queue;

    @Resource(mappedName = REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

    @Deployment
    public static Archive createDeployment() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "set-message-driven-context-invocation-test.jar");
        jar.addClasses(SimpleMDB.class, JMSMessagingUtil.class, JmsQueueSetup.class, TimeoutUtil.class);
        jar.addPackage(JMSOperations.class.getPackage());
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")), "permissions.xml");
        return jar;
    }

    /**
     * Test that the {@link jakarta.ejb.MessageDrivenBean#setMessageDrivenContext(jakarta.ejb.MessageDrivenContext)}
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
