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

package org.jboss.as.test.integration.ejb.mdb.objectmessage.unit;

import java.util.Arrays;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.as.test.integration.ejb.mdb.objectmessage.MDBAcceptingObjectMessage;
import org.jboss.as.test.integration.ejb.mdb.objectmessage.MDBAcceptingObjectMessageOfArrayType;
import org.jboss.as.test.integration.ejb.mdb.objectmessage.SimpleMessageInEarLibJar;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a MDB can get hold of the underlying Object from a {@link ObjectMessage} without any classloading issues.
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup({ObjectMessageTestCase.JmsQueueSetup.class})
public class ObjectMessageTestCase {

    private static final String OBJECT_MESSAGE_ARRAY_TYPE_REPLY_QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/objectmessage-array-reply-queue";

    private static final String OBJECT_MESSAGE_REPLY_QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/objectmessage-reply-queue";

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = MDBAcceptingObjectMessageOfArrayType.QUEUE_JNDI_NAME)
    private Queue objectMessageOfArrayTypeQueue;

    @Resource(mappedName = ObjectMessageTestCase.OBJECT_MESSAGE_ARRAY_TYPE_REPLY_QUEUE_JNDI_NAME)
    private Queue objectMessageOfArrayTypeReplyQueue;

    @Resource(mappedName = MDBAcceptingObjectMessage.QUEUE_JNDI_NAME)
    private Queue objectMessageQueue;

    @Resource(mappedName = ObjectMessageTestCase.OBJECT_MESSAGE_REPLY_QUEUE_JNDI_NAME)
    private Queue objectMessageReplyQueue;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsAdminOperations.createJmsQueue("mdbtest/objectmessage-queue", MDBAcceptingObjectMessage.QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("mdbtest/objectmessage-replyQueue", OBJECT_MESSAGE_REPLY_QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("mdbtest/objectmessage-array-queue", MDBAcceptingObjectMessageOfArrayType.QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("mdbtest/objectmessage-array-replyQueue", OBJECT_MESSAGE_ARRAY_TYPE_REPLY_QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("mdbtest/objectmessage-queue");
                jmsAdminOperations.removeJmsQueue("mdbtest/objectmessage-replyQueue");
                jmsAdminOperations.removeJmsQueue("mdbtest/objectmessage-array-queue");
                jmsAdminOperations.removeJmsQueue("mdbtest/objectmessage-array-replyQueue");
                jmsAdminOperations.close();
            }
        }
    }

    /**
     * .ear
     * |
     * |--- ejb.jar
     * |       |--- <classes including the MDB>
     * |
     * |--- lib
     * |      |
     * |      |--- util.jar
     * |      |        |
     * |      |        |--- <classes including the Class whose object is wrapped in an ObjectMessage>
     *
     * @return
     */
    @Deployment
    public static Archive getDeployment() {

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(MDBAcceptingObjectMessageOfArrayType.class, JMSMessagingUtil.class, ObjectMessageTestCase.class, MDBAcceptingObjectMessage.class);

        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "util.jar");
        libJar.addClasses(SimpleMessageInEarLibJar.class);
        libJar.addPackage(JMSOperations.class.getPackage());
        libJar.addClass(JmsQueueSetup.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "mdb-objectmessage-test.ear");
        ear.addAsModule(ejbJar);
        ear.addAsLibraries(libJar);

        ear.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        return ear;
    }

    /**
     * Test that the MDB can process a {@link ObjectMessage} which consists of an array of objects,
     * without any classloading issues
     *
     * @throws Exception
     */
    @Test
    public void testObjectMessageWithObjectArray() throws Exception {
        final String goodMorning = "Good morning";
        final String goodEvening = "Good evening";
        // create the message
        final SimpleMessageInEarLibJar[] multipleGreetings = new SimpleMessageInEarLibJar[2];
        final SimpleMessageInEarLibJar messageOne = new SimpleMessageInEarLibJar(goodMorning);
        final SimpleMessageInEarLibJar messageTwo = new SimpleMessageInEarLibJar(goodEvening);
        multipleGreetings[0] = messageOne;
        multipleGreetings[1] = messageTwo;

        // send as ObjectMessage
        this.jmsUtil.sendObjectMessage(multipleGreetings, this.objectMessageOfArrayTypeQueue, this.objectMessageOfArrayTypeReplyQueue);
        // wait for an reply
        final Message reply = this.jmsUtil.receiveMessage(objectMessageOfArrayTypeReplyQueue, 5000);
        // test the reply
        Assert.assertNotNull("Reply message was null on reply queue: " + this.objectMessageOfArrayTypeReplyQueue, reply);
        final SimpleMessageInEarLibJar[] replyMessage = (SimpleMessageInEarLibJar[]) ((ObjectMessage) reply).getObject();
        Assert.assertTrue("Unexpected reply message on reply queue: " + this.objectMessageOfArrayTypeReplyQueue, Arrays.equals(replyMessage, multipleGreetings));

    }

    /**
     * Test that the MDB can process a {@link ObjectMessage} without any classloading issues
     *
     * @throws Exception
     */
    @Test
    public void testObjectMessage() throws Exception {
        final String goodAfternoon = "Good afternoon!";
        // create the message
        final SimpleMessageInEarLibJar message = new SimpleMessageInEarLibJar(goodAfternoon);

        // send as ObjectMessage
        this.jmsUtil.sendObjectMessage(message, this.objectMessageQueue, this.objectMessageReplyQueue);
        // wait for an reply
        final Message reply = this.jmsUtil.receiveMessage(objectMessageReplyQueue, 5000);
        // test the reply
        Assert.assertNotNull("Reply message was null on reply queue: " + this.objectMessageReplyQueue, reply);
        final SimpleMessageInEarLibJar replyMessage = (SimpleMessageInEarLibJar) ((ObjectMessage) reply).getObject();
        Assert.assertEquals("Unexpected reply message on reply queue: " + this.objectMessageReplyQueue, message, replyMessage);

    }

}
