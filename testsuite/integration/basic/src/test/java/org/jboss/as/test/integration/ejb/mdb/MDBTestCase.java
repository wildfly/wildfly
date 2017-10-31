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

package org.jboss.as.test.integration.ejb.mdb;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.Queue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;

/**
 * Tests MDB deployments
 *
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup({MDBTestCase.JmsQueueSetup.class})
public class MDBTestCase {

    @EJB (mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource (mappedName = "java:jboss/mdbtest/queue")
    private Queue queue;

    @Resource (mappedName = "java:jboss/mdbtest/replyQueue")
    private Queue replyQueue;

    @Resource (mappedName = "java:jboss/mdbtest/annoQueue")
    private Queue annoQueue;

    @Resource (mappedName = "java:jboss/mdbtest/annoReplyQueue")
    private Queue annoReplyQueue;

    @ArquillianResource
    private ManagementClient managementClient;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsAdminOperations.createJmsQueue("mdbtest/queue", "java:jboss/mdbtest/queue");
            jmsAdminOperations.createJmsQueue("mdbtest/replyQueue", "java:jboss/mdbtest/replyQueue");
            jmsAdminOperations.createJmsQueue("mdbtest/annoQueue", "java:jboss/mdbtest/annoQueue");
            jmsAdminOperations.createJmsQueue("mdbtest/annoReplyQueue", "java:jboss/mdbtest/annoReplyQueue");
            jmsAdminOperations.setSystemProperties("jboss/mdbtest/annoQueue", "activemq-ra.rar");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("mdbtest/queue");
                jmsAdminOperations.removeJmsQueue("mdbtest/replyQueue");
                jmsAdminOperations.removeJmsQueue("mdbtest/annoQueue");
                jmsAdminOperations.removeJmsQueue("mdbtest/annoReplyQueue");
                jmsAdminOperations.removeSystemProperties();
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static Archive getDeployment() {

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "MDBTestCase.jar");
        ejbJar.addClasses(DDBasedMDB.class, BMTSLSB.class, JMSMessagingUtil.class, AnnoBasedMDB.class);
        ejbJar.addPackage(JMSOperations.class.getPackage());
        ejbJar.addClass(JmsQueueSetup.class);
        ejbJar.addAsManifestResource(MDBTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, org.jboss.remoting3\n"), "MANIFEST.MF");
        ejbJar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");

        return ejbJar;
    }

    /**
     * Test a deployment descriptor based MDB
     * @throws Exception
     */
    @Test
    public void testDDBasedMDB() throws Exception {
        this.jmsUtil.sendTextMessage("Say hello to " + DDBasedMDB.class.getName(), this.queue, this.replyQueue);
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        Assert.assertNotNull("Reply message was null on reply queue: " + this.replyQueue, reply);
    }

    /**
     * Test an annotation based MDB with properties substitution
     * @throws Exception
     */
    @Test
    public void testAnnoBasedMDB() throws Exception {
        this.jmsUtil.sendTextMessage("Say Nihao to " + AnnoBasedMDB.class.getName(), this.annoQueue, this.annoReplyQueue);
        final Message reply = this.jmsUtil.receiveMessage(annoReplyQueue, 5000);
        Assert.assertNotNull("Reply message was null on reply queue: " + this.annoReplyQueue, reply);
    }


    /**
     * Test a deployment descriptor based MDB
     * @throws Exception
     */
    @Test
    public void testSuspendResumeWithMDB() throws Exception {
        boolean resumed = false;
        ModelNode op = new ModelNode();
        try {

            //suspend the server
            op.get(ModelDescriptionConstants.OP).set("suspend");
            managementClient.getControllerClient().execute(op);


            this.jmsUtil.sendTextMessage("Say hello to " + DDBasedMDB.class.getName(), this.queue, this.replyQueue);
            Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
            Assert.assertNull("Reply message was not null on reply queue: " + this.replyQueue, reply);

            resumed = true;
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("resume");
            managementClient.getControllerClient().execute(op);


            reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
            Assert.assertNotNull("Reply message was null on reply queue: " + this.replyQueue, reply);

        } finally {
            if(!resumed) {
                op = new ModelNode();
                op.get(ModelDescriptionConstants.OP).set("resume");
                managementClient.getControllerClient().execute(op);
            }
        }
    }
}
