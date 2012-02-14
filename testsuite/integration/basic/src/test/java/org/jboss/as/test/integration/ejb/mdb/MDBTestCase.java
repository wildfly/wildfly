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
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.mdb.DDBasedMDB;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
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
 * Tests MDB deployments
 *
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class MDBTestCase {

    private static final Logger logger = Logger.getLogger(MDBTestCase.class);

    @EJB (mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource (mappedName = "java:jboss/mdbtest/queue")
    private Queue queue;

    @Resource (mappedName = "java:jboss/mdbtest/replyQueue")
    private Queue replyQueue;

    private static JMSOperations jmsAdminOperations;

    @Deployment
    public static Archive getDeployment() {
        // setup the queues
        createJmsDestinations();

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "mdb.jar");
        ejbJar.addPackage(DDBasedMDB.class.getPackage());
        ejbJar.addPackage(JMSOperations.class.getPackage());
        ejbJar.addAsManifestResource(MDBTestCase.class.getPackage(), "mdb/ejb-jar.xml", "ejb-jar.xml");
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        logger.info(ejbJar.toString(true));
        return ejbJar;
    }

    private static void createJmsDestinations() {
        jmsAdminOperations = JMSOperationsProvider.getInstance();
        jmsAdminOperations.createJmsQueue("mdbtest/queue", "java:jboss/mdbtest/queue");
        jmsAdminOperations.createJmsQueue("mdbtest/replyQueue", "java:jboss/mdbtest/replyQueue");
    }

    @AfterClass
    public static void afterTestClass() {
        jmsAdminOperations.removeJmsQueue("mdbtest/queue");
        jmsAdminOperations.removeJmsQueue("mdbtest/replyQueue");
        jmsAdminOperations.close();
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
}
