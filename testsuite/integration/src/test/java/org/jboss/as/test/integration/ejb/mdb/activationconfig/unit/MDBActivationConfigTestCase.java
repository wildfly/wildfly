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

package org.jboss.as.test.integration.ejb.mdb.activationconfig.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.JMSAdminOperations;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.as.test.integration.ejb.mdb.activationconfig.MDBWithUnknownActivationConfigProperties;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.Queue;

/**
 * Tests activation config properties on a MDB
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class MDBActivationConfigTestCase {

    private static final Logger logger = Logger.getLogger(MDBActivationConfigTestCase.class);

    private static final String REPLY_QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/activation-config-reply-queue";

    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil jmsUtil;

    @Resource(mappedName = MDBWithUnknownActivationConfigProperties.QUEUE_JNDI_NAME)
    private Queue queue;

    @Resource(mappedName = MDBActivationConfigTestCase.REPLY_QUEUE_JNDI_NAME)
    private Queue replyQueue;

    @Deployment
    public static Archive getDeployment() {
        // setup the queues
        createJmsDestinations();

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "mdb-activation-config-test.jar");
        ejbJar.addPackage(MDBWithUnknownActivationConfigProperties.class.getPackage());
        ejbJar.addClasses(JMSAdminOperations.class, JMSMessagingUtil.class);
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        logger.info(ejbJar.toString(true));
        return ejbJar;
    }

    private static void createJmsDestinations() {
        final JMSAdminOperations jmsAdminOperations = new JMSAdminOperations();
        jmsAdminOperations.createJmsQueue("mdbtest/activation-config-queue", MDBWithUnknownActivationConfigProperties.QUEUE_JNDI_NAME);
        jmsAdminOperations.createJmsQueue("mdbtest/activation-config-replyQueue", REPLY_QUEUE_JNDI_NAME);
    }

    @AfterClass
    public static void afterTestClass() {
        final JMSAdminOperations jmsAdminOperations = new JMSAdminOperations();
        jmsAdminOperations.removeJmsQueue("mdbtest/activation-config-queue");
        jmsAdminOperations.removeJmsQueue("mdbtest/activation-config-replyQueue");
    }

    /**
     * Tests that deployment of a MDB containing a unknown/unsupported activation config property doesn't throw
     * a deployment error.
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedActivationConfigProps() throws Exception {
        this.jmsUtil.sendTextMessage("Say hello to " + MDBWithUnknownActivationConfigProperties.class.getName(), this.queue, this.replyQueue);
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        Assert.assertNotNull("Reply message was null on reply queue: " + this.replyQueue, reply);
    }

}
