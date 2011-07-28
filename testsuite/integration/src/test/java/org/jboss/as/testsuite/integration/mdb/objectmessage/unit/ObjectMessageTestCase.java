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

package org.jboss.as.testsuite.integration.mdb.objectmessage.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.common.JMSAdminOperations;
import org.jboss.as.testsuite.integration.mdb.JMSMessagingUtil;
import org.jboss.as.testsuite.integration.mdb.objectmessage.MDBAcceptingObjectMessage;
import org.jboss.as.testsuite.integration.mdb.objectmessage.SimpleMessageInEarLibJar;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.Arrays;

/**
 * Tests that a MDB can get hold of the underlying Object from a {@link ObjectMessage} without any classloading issues.
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class ObjectMessageTestCase {

    private static final Logger logger = Logger.getLogger(ObjectMessageTestCase.class);

    private static final String REPLY_QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/objectmessage-reply-queue";

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = MDBAcceptingObjectMessage.QUEUE_JNDI_NAME)
    private Queue queue;

    @Resource(mappedName = ObjectMessageTestCase.REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

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
     * |      |        |--- <classes including the Class whose object is wrapped in a ObjectMessage>
     *
     * @return
     */
    @Deployment
    public static Archive getDeployment() {
        // setup the queues
        createJmsDestinations();

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(MDBAcceptingObjectMessage.class, JMSMessagingUtil.class, ObjectMessageTestCase.class);
        logger.info(ejbJar.toString(true));

        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "util.jar");
        libJar.addClasses(SimpleMessageInEarLibJar.class, JMSAdminOperations.class);
        logger.info(libJar.toString(true));

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "mdb-objectmessage-test.ear");
        ear.addAsModule(ejbJar);
        ear.addAsLibraries(libJar);

        ear.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        logger.info(ear.toString(true));
        return ear;
    }

    private static void createJmsDestinations() {
        final JMSAdminOperations jmsAdminOperations = new JMSAdminOperations();
        jmsAdminOperations.createJmsQueue("mdbtest/objectmessage-queue", MDBAcceptingObjectMessage.QUEUE_JNDI_NAME);
        jmsAdminOperations.createJmsQueue("mdbtest/objectmessage-replyQueue", REPLY_QUEUE_JNDI_NAME);
    }

    @AfterClass
    public static void afterTestClass() {
        final JMSAdminOperations jmsAdminOperations = new JMSAdminOperations();
        jmsAdminOperations.removeJmsQueue("mdbtest/objectmessage-queue");
        jmsAdminOperations.removeJmsQueue("mdbtest/objectmessage-replyQueue");
    }

    /**
     * Test that the MDB can process a {@link ObjectMessage} without any classloading issues
     *
     * @throws Exception
     */
    @Test
    public void testObjectMessage() throws Exception {
        final String goodMorning = "Good morning";
        final String goodEvening = "Good evening";
        // create the message
        final SimpleMessageInEarLibJar[] multipleGreetings = new SimpleMessageInEarLibJar[2];
        final SimpleMessageInEarLibJar messageOne = new SimpleMessageInEarLibJar(goodMorning);
        final SimpleMessageInEarLibJar messageTwo = new SimpleMessageInEarLibJar(goodEvening);
        multipleGreetings[0] = messageOne;
        multipleGreetings[1] = messageTwo;

        // send as ObjectMessage
        this.jmsUtil.sendObjectMessage(multipleGreetings, this.queue, this.replyQueue);
        // wait for an reply
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        // test the reply
        Assert.assertNotNull("Reply message was null on reply queue: " + this.replyQueue, reply);
        final SimpleMessageInEarLibJar[] replyMessage = (SimpleMessageInEarLibJar[]) ((ObjectMessage) reply).getObject();
        Assert.assertTrue("Unexpected reply message on reply queue: " + this.replyQueue, Arrays.equals(replyMessage, multipleGreetings));

    }
}
