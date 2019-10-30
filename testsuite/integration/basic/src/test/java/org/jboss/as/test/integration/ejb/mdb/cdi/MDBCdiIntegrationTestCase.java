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

package org.jboss.as.test.integration.ejb.mdb.cdi;

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
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the CDI request scope is active in MDB invocations.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup({MDBCdiIntegrationTestCase.JmsQueueSetup.class})
public class MDBCdiIntegrationTestCase {

    private static final String REPLY_QUEUE_JNDI_NAME = "java:/mdb-cdi-test/replyQueue";
    public static final String QUEUE_JNDI_NAME = "java:/mdb-cdi-test/mdb-cdi-test";

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

    @Resource(mappedName = QUEUE_JNDI_NAME)
    private Queue queue;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsAdminOperations.createJmsQueue("mdb-cdi-test/queue", QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("mdb-cdi-test/reply-queue", REPLY_QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("mdb-cdi-test/queue");
                jmsAdminOperations.removeJmsQueue("mdb-cdi-test/reply-queue");
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static Archive getDeployment() {

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "mdb-cdi-integration-test.jar");
        ejbJar.addClasses(CdiIntegrationMDB.class, RequestScopedCDIBean.class, JMSMessagingUtil.class, MDBCdiIntegrationTestCase.class, JmsQueueSetup.class)
           .addPackage(JMSOperations.class.getPackage());
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        ejbJar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return ejbJar;
    }

    @Test
    public void testRequestScopeActiveDuringMdbInvocation() throws Exception {
        final String goodMorning = "Good morning";
        // send as ObjectMessage
        this.jmsUtil.sendTextMessage(goodMorning, this.queue, this.replyQueue);
        // wait for an reply
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        // test the reply
        final TextMessage textMessage = (TextMessage) reply;
        Assert.assertEquals("Unexpected reply message on reply queue: " + this.replyQueue, CdiIntegrationMDB.REPLY, textMessage.getText());
    }

}
