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

package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.Queue;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * WFLY-2732 - test if MDB can access @DependsOn ejbs in @PostConstruct and @PreDestroy annotated methods.
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
@ServerSetup({JmsQueueServerSetupTask.class, SetupModuleServerSetupTask.class})
public class MDBWhichDependsOnTestCase {

    private static final Logger logger = Logger.getLogger(MDBWhichDependsOnTestCase.class);

    @ArquillianResource
    Deployer deployer;

    @EJB(mappedName = Constants.EJB_JMS_NAME)
    private JMSMessagingUtil jmsUtil;

    @EJB
    private CallCounterInterface counter;

    @Resource(mappedName = Constants.QUEUE_JNDI_NAME)
    private Queue queue;

    @Resource(mappedName = Constants.QUEUE_REPLY_JNDI_NAME)
    private Queue replyQueue;

    @Deployment(name = Constants.DEPLOYMENT_NAME_COUNTER, order = 0, managed = true, testable = true)
    public static Archive<?> getTestArchive() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, Constants.DEPLOYMENT_JAR_NAME_COUNTER);
        jar.addClass(CallCounterSingleton.class);
        jar.addClass(MDBWhichDependsOnTestCase.class);
        jar.addClass(Constants.class);
        jar.addClass(JMSMessagingUtil.class);
        jar.addClasses(JmsQueueServerSetupTask.class, SetupModuleServerSetupTask.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + Constants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        return jar;
    }

    @Deployment(name = Constants.DEPLOYMENT_NAME_MDB, order = 1, managed = false, testable = false)
    public static Archive getMDBArchive() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, Constants.DEPLOYMENT_JAR_NAME_MDB);
        jar.addPackage(JMSOperations.class.getPackage());
        jar.addClass(JMSMessagingUtil.class);
        jar.addClass(MDBWhichDependsOn.class);
        jar.addClass(Constants.class);
        jar.addClass(CallCounterProxy.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + Constants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        return jar;
    }

    /**
     * Test an annotation based MDB with properties substitution
     *
     * @throws Exception
     */
    @Test
    public void testAnnoBasedMDB() throws Exception {
        this.deployer.deploy(Constants.DEPLOYMENT_NAME_MDB);
        this.jmsUtil.sendTextMessage("Say Nihao to new message!", this.queue, this.replyQueue);
        final Message reply = this.jmsUtil.receiveMessage(replyQueue, 5000);
        Assert.assertNotNull("Reply message was null on reply queue: " + this.replyQueue, reply);
        this.deployer.undeploy(Constants.DEPLOYMENT_NAME_MDB);
        Assert.assertTrue("PostConstruct not called!", counter.isPostConstruct());
        Assert.assertTrue("Message not called!", counter.isMessage());
        Assert.assertTrue("PreDestroy not called!", counter.isPreDestroy());
    }
}
