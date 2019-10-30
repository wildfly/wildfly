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

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;

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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * User: jpai
 */
@RunWith(Arquillian.class)
@ServerSetup({ResourceAdapterNameTestCase.JmsQueueSetup.class})
public class ResourceAdapterNameTestCase {

    private static final String REPLY_QUEUE_JNDI_NAME = "java:jboss/resource-adapter-name-test/replyQueue";
    public static final String QUEUE_JNDI_NAME = "java:jboss/jms/queue/resource-adapater-name-queue";

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = ResourceAdapterNameTestCase.REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

    @Resource(mappedName = QUEUE_JNDI_NAME)
    private Queue queue;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsAdminOperations.createJmsQueue("resource-adapter-name-test/queue", QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("resource-adapter-name-test/reply-queue", REPLY_QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("resource-adapter-name-test/queue");
                jmsAdminOperations.removeJmsQueue("resource-adapter-name-test/reply-queue");
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static Archive getDeployment() {

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "resource-adapter-name-mdb-test.jar");
        ejbJar.addClasses(OverriddenResourceAdapterNameMDB.class, JMSMessagingUtil.class, ResourceAdapterNameTestCase.class,
                JmsQueueSetup.class);
        ejbJar.addPackage(JMSOperations.class.getPackage());
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        return ejbJar;
    }

    @Test
    public void testMDBWithOverriddenResourceAdapterName() throws Exception {
        final String goodMorning = "Good morning";
        // send as ObjectMessage
        this.jmsUtil.sendTextMessage(goodMorning, this.queue, this.replyQueue);
        // wait for an reply
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        // test the reply
        final TextMessage textMessage = (TextMessage) reply;
        Assert.assertNotNull(textMessage);
        Assert.assertEquals("Unexpected reply message on reply queue: " + this.replyQueue, OverriddenResourceAdapterNameMDB.REPLY, textMessage.getText());
    }

}
