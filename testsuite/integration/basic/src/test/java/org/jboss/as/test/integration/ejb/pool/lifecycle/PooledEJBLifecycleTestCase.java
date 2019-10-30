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

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.PropertyPermission;
import javax.ejb.EJB;
import javax.jms.Connection;
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
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests instance lifecycle of EJB components which can potentially be pooled. Note that this testcase does *not* mandate that the components being tested be pooled. It's completely upto the container
 * to decide if they want to pool these components by default or not.
 *
 * @author baranowb
 * @author Jaikiran Pai - Updates related to https://issues.jboss.org/browse/WFLY-1506
 */
@RunWith(Arquillian.class)
@ServerSetup(PooledEJBLifecycleTestCase.CreateQueueForPooledEJBLifecycleTestCase.class)
public class PooledEJBLifecycleTestCase {

    private static final String MDB_DEPLOYMENT_NAME = "mdb-pool-ejb-callbacks"; // module
    private static final String SLSB_DEPLOYMENT_NAME = "slsb-pool-ejb-callbacks"; // module
    private static final String DEPLOYMENT_NAME_SINGLETON = "pool-ejb-callbacks-singleton"; // module
    private static final String SINGLETON_JAR = DEPLOYMENT_NAME_SINGLETON + ".jar"; // jar name
    private static final String DEPLOYED_SINGLETON_MODULE = "deployment." + SINGLETON_JAR; // singleton deployed module name
    private static final String SLSB_JNDI_NAME = "java:global/" + SLSB_DEPLOYMENT_NAME
            + "/PointLessMathBean!org.jboss.as.test.integration.ejb.pool.lifecycle.PointlesMathInterface";

    private static final Logger log = Logger.getLogger(PooledEJBLifecycleTestCase.class.getName());

    @ArquillianResource
    public Deployer deployer;

    @EJB(mappedName = "java:global/pool-ejb-callbacks-singleton/LifecycleTrackerBean!org.jboss.as.test.integration.ejb.pool.lifecycle.LifecycleTracker")
    private LifecycleTracker lifecycleTracker;

    // ----------------- DEPLOYMENTS ------------

    // deploy Singleton bean. this will be deployed/managed by Arquillian outside of the test methods
    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, SINGLETON_JAR);
        // this includes test case class, since package name is the same.
        archive.addClass(LifecycleTracker.class);
        archive.addClass(LifecycleTrackerBean.class);
        archive.addClass(TimeoutUtil.class);
        archive.addClass(PointlesMathInterface.class);
        archive.addClass(Constants.class);
        archive.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "jboss-permissions.xml");
        return archive;
    }

    // this will be deployed manually in the individual test methods
    @Deployment(name = MDB_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getMDBTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, MDB_DEPLOYMENT_NAME);
        archive.addClass(LifecycleCounterMDB.class);
        archive.addClass(LifecycleTracker.class);
        archive.addClass(Constants.class);
        archive.setManifest(new StringAsset(
                Descriptors.create(ManifestDescriptor.class)
                        .attribute("Dependencies", DEPLOYED_SINGLETON_MODULE + ", org.apache.activemq.artemis.ra")
                        .exportAsString()));

        return archive;
    }

    // this will be deployed manually in the individual test methods
    @Deployment(name = SLSB_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getSLSBTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, SLSB_DEPLOYMENT_NAME);
        archive.addClass(PointLessMathBean.class);
        archive.addClass(LifecycleTracker.class);
        archive.setManifest(new StringAsset(
                Descriptors.create(ManifestDescriptor.class)
                        .attribute("Dependencies", DEPLOYED_SINGLETON_MODULE + ", org.apache.activemq.artemis.ra")
                        .exportAsString()));
        return archive;
    }

    @Before
    public void beforeTest() {
        log.trace("Clearing state of the singleton lifecycle tracker bean");
        lifecycleTracker.clearState();
    }

    // ------------------- TEST METHODS ---------------------

    @SuppressWarnings("static-access")
    @Test
    public void testMDB() throws Exception {
        boolean requiresUndeploy = false;
        try {
            // do the deployment of the MDB
            log.trace("About to deploy MDB archive " + MDB_DEPLOYMENT_NAME);
            deployer.deploy(MDB_DEPLOYMENT_NAME);
            // we keep track of this to make sure we undeploy before leaving this method
            requiresUndeploy = true;
            log.trace("deployed " + MDB_DEPLOYMENT_NAME);

            // now send a messag to the queue on which the MDB is listening
            log.trace("Sending a message to the queue on which the MDB " + " is listening");
            triggerRequestResponseCycleOnQueue();

            assertTrue("@PostConstruct wasn't invoked on MDB", lifecycleTracker.wasPostConstructInvokedOn(this.getClass().getPackage().getName() + ".LifecycleCounterMDB"));

            // undeploy
            log.trace("About to undeploy MDB archive " + MDB_DEPLOYMENT_NAME);
            deployer.undeploy(MDB_DEPLOYMENT_NAME);
            // we have undeployed successfully, there's no need anymore to trigger an undeployment before returning from this method
            requiresUndeploy = false;

            assertTrue("@PreDestroy wasn't invoked on MDB", lifecycleTracker.wasPreDestroyInvokedOn(this.getClass().getPackage().getName() + ".LifecycleCounterMDB"));
        } finally {
            if (requiresUndeploy) {
                try {
                    deployer.undeploy(MDB_DEPLOYMENT_NAME);
                } catch (Throwable t) {
                    // log and return since we don't want to corrupt any original exceptions that might have caused the test to fail
                    log.trace("Ignoring the undeployment failure of " + MDB_DEPLOYMENT_NAME, t);
                }
            }
        }
    }

    @SuppressWarnings("static-access")
    @Test
    public void testSLSB() throws Exception {
        boolean requiresUndeploy = false;
        try {
            // deploy the SLSB
            log.trace("About to deploy SLSB archive " + SLSB_DEPLOYMENT_NAME);
            deployer.deploy(SLSB_DEPLOYMENT_NAME);
            requiresUndeploy = true;
            log.trace("deployed " + SLSB_DEPLOYMENT_NAME);

            // invoke on bean
            final PointlesMathInterface mathBean = (PointlesMathInterface) new InitialContext().lookup(SLSB_JNDI_NAME);
            mathBean.pointlesMathOperation(4, 5, 6);

            assertTrue("@PostConstruct wasn't invoked on SLSB", lifecycleTracker.wasPostConstructInvokedOn(this.getClass().getPackage().getName() + ".PointLessMathBean"));

            log.trace("About to undeploy SLSB archive " + SLSB_DEPLOYMENT_NAME);
            deployer.undeploy(SLSB_DEPLOYMENT_NAME);
            requiresUndeploy = false;

            assertTrue("@PreDestroy wasn't invoked on SLSB", lifecycleTracker.wasPreDestroyInvokedOn(this.getClass().getPackage().getName() + ".PointLessMathBean"));

        } finally {
            if (requiresUndeploy) {
                try {
                    deployer.undeploy(SLSB_DEPLOYMENT_NAME);
                } catch (Throwable t) {
                    // log and return since we don't want to corrupt any original exceptions that might have caused the test to fail
                    log.trace("Ignoring the undeployment failure of " + SLSB_DEPLOYMENT_NAME, t);
                }
            }
        }
    }

    // ------------------ HELPER METHODS -------------------

    private void triggerRequestResponseCycleOnQueue() throws Exception {
        final InitialContext ctx = new InitialContext();
        final QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("java:/JmsXA");
        final QueueConnection connection = factory.createQueueConnection();
        try {
            connection.start();
            final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            final Queue replyDestination = session.createTemporaryQueue();
            final String requestMessage = "test";
            final Message message = session.createTextMessage(requestMessage);
            message.setJMSReplyTo(replyDestination);
            final Destination destination = (Destination) ctx.lookup(Constants.QUEUE_JNDI_NAME);
            final MessageProducer producer = session.createProducer(destination);
            // create receiver
            final QueueReceiver receiver = session.createReceiver(replyDestination);
            producer.send(message);
            producer.close();

            //wait for reply
            final Message reply = receiver.receive(TimeoutUtil.adjust(5000));
            assertNotNull("Did not receive a reply on the reply queue. Perhaps the original (request) message didn't make it to the MDB?", reply);
            final String result = ((TextMessage) reply).getText();
            assertEquals("Unexpected reply messsage", Constants.REPLY_MESSAGE_PREFIX + requestMessage, result);
        } finally {
            if (connection != null) {
                // just closing the connection will close the session and other related resources (@see javax.jms.Connection)
                safeClose(connection);
            }
        }

    }

    /**
     * Responsible for creating and removing the queue required by this testcase
     */
    static class CreateQueueForPooledEJBLifecycleTestCase implements ServerSetupTask {

        private static final String QUEUE_NAME = "Queue-for-" + PooledEJBLifecycleTestCase.class.getName();

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            // create the JMS queue
            final JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsOperations.createJmsQueue(QUEUE_NAME, Constants.QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // destroy the JMS queue
            final JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsOperations.removeJmsQueue(QUEUE_NAME);
        }
    }

    private static void safeClose(final Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Throwable t) {
            // just log
            log.trace("Ignoring a problem which occurred while closing: " + connection, t);
        }
    }
}