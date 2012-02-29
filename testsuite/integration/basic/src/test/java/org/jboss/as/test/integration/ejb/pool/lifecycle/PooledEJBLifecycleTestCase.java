/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.ejb.pool.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests if pooled EJBs have proper lifecycle.
 * 
 * @author baranowb
 * 
 */
@RunWith(Arquillian.class)
public class PooledEJBLifecycleTestCase {

    private static final String MDB_DEPLOYMENT_NAME = "mdb-pool-ejb-callbacks"; // module
    private static final String SLSB_DEPLOYMENT_NAME = "slsb-pool-ejb-callbacks"; // module
    private static final String DEPLOYMENT_NAME_SINGLETON = "pool-ejb-callbacks-singleton"; // module
    private static final String SINGLETON_JAR = DEPLOYMENT_NAME_SINGLETON + ".jar"; // jar name
    private static final String DEPLOYED_SINGLETON_MODULE = "deployment." + SINGLETON_JAR; // singleton deployed module name
    private static final String SLSB_JNDI_NAME = "java:global/" + SLSB_DEPLOYMENT_NAME
            + "/LifecycleCounterSLSB!org.jboss.as.test.integration.ejb.pool.lifecycle.PointlesMathInterface";

    private static final Logger log = Logger.getLogger(PooledEJBLifecycleTestCase.class.getName());

    @ArquillianResource
    public Deployer deployer;

    @EJB(mappedName = "java:global/pool-ejb-callbacks-singleton/LifecycleCounterBean!org.jboss.as.test.integration.ejb.pool.lifecycle.LifecycleCounter")
    private LifecycleCounter cycleCounter;

    // ----------------- DEPLOYMENTS ------------

    // deploy Singleton bean
    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, SINGLETON_JAR);
        // this includes test case class, since package name is the same.
        archive.addClass(LifecycleCounter.class);
        archive.addClass(LifecycleCounterBean.class);
        archive.addClass(PointlesMathInterface.class);
        log.info(archive.toString(true));
        return archive;
    }

    @Deployment(name = MDB_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getMDBTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, MDB_DEPLOYMENT_NAME);
        archive.addClass(LifecycleCounterMDB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                StringBuffer dependencies = new StringBuffer();
                dependencies.append(DEPLOYED_SINGLETON_MODULE);
                dependencies.append(" , org.hornetq.ra");
                builder.addManifestHeader("Dependencies", dependencies.toString());
                return builder.openStream();
            }
        });

        log.info(archive.toString(true));
        return archive;
    }

    @Deployment(name = SLSB_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getSLSBTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, SLSB_DEPLOYMENT_NAME);
        archive.addClass(LifecycleCounterSLSB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                StringBuffer dependencies = new StringBuffer();
                dependencies.append(DEPLOYED_SINGLETON_MODULE);
                dependencies.append(" , org.hornetq.ra");
                builder.addManifestHeader("Dependencies", dependencies.toString());
                return builder.openStream();
            }
        });
        log.info(archive.toString(true));
        return archive;
    }

    // ------------------- TEST METHODS ---------------------

    @SuppressWarnings("static-access")
    @Test
    public void testMDB() throws Exception {
        try {
            log.info("-->About to deploy MDB archive");
            deployer.deploy(MDB_DEPLOYMENT_NAME);
            log.info("-->deployed");

            // // do checks
            assertEquals("Wrong postCreate calls count", 0, cycleCounter.getPostCreateCount());
            assertEquals("Wrong preDestroy calls count", 0, cycleCounter.getPreDestroyCount());
            log.info("-->Performing JMS call to spawn bean");
            triggerMDB();
            assertEquals("Wrong postCreate calls count, after EJB has been triggered", 1, cycleCounter.getPostCreateCount());
            assertEquals("Wrong preDestroy calls count, after EJB has been triggered", 0, cycleCounter.getPreDestroyCount());
            // undeploy
            log.info("-->About to undeploy MDB archive");
            deployer.undeploy(MDB_DEPLOYMENT_NAME);
            assertEquals("Wrong postCreate calls count, after EJB has been undeployed", 1, cycleCounter.getPostCreateCount());
            assertEquals("Wrong preDestroy calls count, after EJB has been undeployed", 1, cycleCounter.getPreDestroyCount());
        } finally {
            cycleCounter.reset();
        }
    }

    @SuppressWarnings("static-access")
    @Test
    public void testSLSB() throws Exception {
        try {
            log.info("-->About to deploy SLSB archive");
            deployer.deploy(SLSB_DEPLOYMENT_NAME);
            log.info("-->deployed");
            // do checks
            assertEquals("Wrong postCreate calls count", 0, cycleCounter.getPostCreateCount());
            assertEquals("Wrong preDestroy calls count", 0, cycleCounter.getPreDestroyCount());
            triggerSLSB();
            assertEquals("Wrong postCreate calls count, after EJB has been triggered", 1, cycleCounter.getPostCreateCount());
            assertEquals("Wrong preDestroy calls count, after EJB has been triggered", 0, cycleCounter.getPreDestroyCount());
            log.info("-->About to undeploy SLSB archive");
            deployer.undeploy(SLSB_DEPLOYMENT_NAME);
            assertEquals("Wrong postCreate calls count, after EJB has been undeployed", 1, cycleCounter.getPostCreateCount());
            assertEquals("Wrong preDestroy calls count, after EJB has been undeployed", 1, cycleCounter.getPreDestroyCount());
        } finally {
            cycleCounter.reset();
        }
    }

    // ------------------ HELPER METHODS -------------------

    private void triggerMDB() throws Exception {
        final InitialContext ctx = new InitialContext();
        final QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("java:/JmsXA");
        final QueueConnection connection = factory.createQueueConnection();
        connection.start();

        final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        final Queue replyDestination = session.createTemporaryQueue();
        final QueueReceiver receiver = session.createReceiver(replyDestination);
        final Message message = session.createTextMessage("Test");
        message.setJMSReplyTo(replyDestination);
        final Destination destination = (Destination) ctx.lookup("queue/test");
        final MessageProducer producer = session.createProducer(destination);
        producer.send(message);
        producer.close();

        final Message reply = receiver.receive(1000);
        assertNotNull(reply);
        final String result = ((TextMessage) reply).getText();
        assertEquals("replying Test", result);

    }

    private void triggerSLSB() throws Exception {

        PointlesMathInterface math = (PointlesMathInterface) new InitialContext().lookup(SLSB_JNDI_NAME);
        math.pointlesMathOperation(4, 5, 6);
    }
}