/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.cdi;

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
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.PropertyPermission;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

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
        ejbJar.addClasses(CdiIntegrationMDB.class, RequestScopedCDIBean.class, JMSMessagingUtil.class, MDBCdiIntegrationTestCase.class,
                JmsQueueSetup.class, TimeoutUtil.class)
           .addPackage(JMSOperations.class.getPackage());
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        ejbJar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        ejbJar.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")), "permissions.xml");
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
