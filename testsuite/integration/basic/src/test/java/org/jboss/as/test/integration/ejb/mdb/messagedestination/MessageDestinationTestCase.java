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

package org.jboss.as.test.integration.ejb.mdb.messagedestination;

import javax.ejb.EJB;
import javax.jms.Message;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests message-destination resolution
 *
 */
@RunWith(Arquillian.class)
public class MessageDestinationTestCase {

    private static final Logger logger = Logger.getLogger(MessageDestinationTestCase.class);

    @EJB (mappedName = "java:module/MessagingEjb")
    private MessagingEjb messagingMdb;

    private static JMSOperations jmsAdminOperations;

    @Deployment
    public static Archive getDeployment() {
        // setup the queues
        createJmsDestinations();

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "mdb.jar");
        ejbJar.addPackage(MessageDestinationTestCase.class.getPackage());
        ejbJar.addPackage(JMSOperations.class.getPackage());
        ejbJar.addAsManifestResource(MessageDestinationTestCase.class.getPackage(),  "ejb-jar.xml", "ejb-jar.xml");
        ejbJar.addAsManifestResource(MessageDestinationTestCase.class.getPackage(),  "jboss-ejb3.xml", "jboss-ejb3.xml");
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        logger.info(ejbJar.toString(true));
        return ejbJar;
    }

    private static void createJmsDestinations() {
        jmsAdminOperations = JMSOperationsProvider.getInstance();
        jmsAdminOperations.createJmsQueue("messagedestinationtest/queue", "java:jboss/mdbtest/messageDestinationQueue");
        jmsAdminOperations.createJmsQueue("messagedestinationtest/replyQueue", "java:jboss/mdbtest/messageDestinationReplyQueue");
    }

    @AfterClass
    public static void afterTestClass() {
        jmsAdminOperations.removeJmsQueue("messagedestinationtest/queue");
        jmsAdminOperations.removeJmsQueue("messagedestinationtest/replyQueue");
        jmsAdminOperations.close();
    }

    /**
     * Test a deployment descriptor based MDB
     * @throws Exception
     */
    @Test
    public void testMEssageDestinationResolution() throws Exception {
        this.messagingMdb.sendTextMessage("Say hello to " + MessagingEjb.class.getName());
        final Message reply = this.messagingMdb.receiveMessage(5000);
        Assert.assertNotNull("Reply message was null on reply queue", reply);
    }
}
