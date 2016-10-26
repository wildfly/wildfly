/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
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

package org.jboss.as.test.integration.ejb.mdb.containerstart;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.test.integration.common.jms.JMSOperationsProvider.getInstance;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.PropertyPermission;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Part of migration EJB testsuite (JBAS-7922) to AS7 [JIRA JBQA-5483]. This test covers jira AS7-687 which aims to migrate this
 * test to new testsuite.
 * Testing undeploying an app in middle of MDB onMessage function and checking whether all messages will processed correctly.
 *
 * Code sequence is:
 * 1. deploy the application
 * 2. send 1 "await" message
 *    => test waits to MDB would start with receiving the message
 * 3. undeploy the app (undeploy waits until onMessage finishes work)
 * 4. MDB transaction is timeouted which means that is marked as rollback only (no exception thrown)
 *    transaction timeout influences only incoming message (is putting back to queue)
 *    the outgoing message is not "send" by XA aware connection factory
 *    when TM runs on JTS then sleep is interrupted with interrupted exception
 * 5. meanwhile sending 50 "50loop" messages
 * 6. MDB onMessage finishes ist work ("await" message is delivered to out-queue)
 *    and the undeployment of app could finish its work
 * 7. ensure that the undeployment is completed
 * 8. redeploy the application
 * 9. send 10 "10loop" messages
 * 10. check the test receives 62 messages:
 *    * 1 await
 *    * 50 do not lose
 *    * 10 some more
 *    * 1 await (redelivered message)
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SendMessagesTestCase.SendMessagesTestCaseSetup.class)
public class SendMessagesTestCase {
    private static final Logger log = Logger.getLogger(SendMessagesTestCase.class);

    private static final String MESSAGE_DRIVEN_BEAN = "message-driven-bean-containerstart";
    private static final String SINGLETON = "single-containerstart";

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    private static String QUEUE_SEND = "queue/sendMessage";

    private static final int UNDEPLOYED_WAIT_S = TimeoutUtil.adjust(30);
    private static final int RECEIVE_WAIT_S = TimeoutUtil.adjust(30);

    @ContainerResource
    private ManagementClient managementClient;

    static class SendMessagesTestCaseSetup implements ServerSetupTask {
        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations operations = getInstance(managementClient);
            operations.createJmsQueue(QUEUE_SEND, "java:jboss/exported/" + QUEUE_SEND);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations operations = getInstance(managementClient);
            operations.removeJmsQueue(QUEUE_SEND);
        }
    }

    @ArquillianResource
    private Deployer deployer;

    @ContainerResource
    private InitialContext ctx;

    @AfterClass
    public static void afterClass() {
        executor.shutdown();
    }

    @Deployment(name = "singleton", order = 1, testable = false, managed = true)
    public static Archive<?> deploymentMbean() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SINGLETON + ".jar")
                .addClasses(HelperSingleton.class, HelperSingletonImpl.class);
        // grant necessary permissions
        jar.addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
        return jar;
    }

    @Deployment(name = "mdb", order = 2, testable = false, managed = false)
    public static Archive<?> deploymentMdb() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MESSAGE_DRIVEN_BEAN + ".jar")
                .addClasses(ReplyingMDB.class, TimeoutUtil.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment." + SINGLETON + ".jar\n"), "MANIFEST.MF");
        // grant necessary permissions
        jar.addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
        return jar;
    }

    public static void applyUpdate(ModelNode update, final ModelControllerClient client) throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                log.trace(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }


    private int awaitSingleton(String where) throws Exception {
        HelperSingleton helper = (HelperSingleton) ctx.lookup(SINGLETON + "/HelperSingletonImpl!org.jboss.as.test.integration.ejb.mdb.containerstart.HelperSingleton");
        return helper.await(where, UNDEPLOYED_WAIT_S, SECONDS);
    }

    private static void sendMessage(Session session, MessageProducer sender, Queue replyQueue, String txt) throws JMSException {
        TextMessage msg = session.createTextMessage(txt);
        msg.setJMSReplyTo(replyQueue);
        msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        sender.send(msg);
    }

    @Test
    public void testShutdown(@ArquillianResource @OperateOnDeployment("singleton") ManagementClient client) throws Exception {
        Connection connection = null;

        try {
            deployer.deploy("mdb");

            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
            Queue queue = (Queue) ctx.lookup(QUEUE_SEND);

            connection = cf.createConnection("guest", "guest");
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue replyQueue = session.createTemporaryQueue();

            MessageProducer sender = session.createProducer(queue);
            MessageConsumer receiver = session.createConsumer(replyQueue);

            connection.start();

            // we do not assume message order since the 1st message will be redelivered
            // after redeployment (as the MDB is interrupted on 1st delivery)
            Set<String> expected = new TreeSet<String>();
            sendMessage(session, sender, replyQueue, "await");
            expected.add("Reply: await");

            // synchronize receiving message
            int awaitInt = awaitSingleton("await before undeploy");
            log.debug("testsuite: first awaitSingleton() returned: " + awaitInt);

            Future<?> undeployed = executor.submit(undeployTask());

            for (int i = 1; i <= 50; i++) {
                String msg = this.getClass().getSimpleName() + " 50loop: " + i;
                sendMessage(session, sender, replyQueue, msg); // should be bounced by BlockContainerShutdownInterceptor
                expected.add("Reply: " + msg);
            }
            log.debug("Sent 50 messages during MDB is undeploying");

            // synchronize with transaction timeout
            awaitInt = awaitSingleton("await after undeploy");
            log.debug("testsuite: second awaitSingleton() returned: " + awaitInt);

            undeployed.get(UNDEPLOYED_WAIT_S, SECONDS);

            // deploying via management client, arquillian deployer does not work for some reason
            final ModelNode deployAddr = new ModelNode();
            deployAddr.get(ClientConstants.OP_ADDR).add("deployment", MESSAGE_DRIVEN_BEAN + ".jar");
            deployAddr.get(ClientConstants.OP).set("deploy");
            applyUpdate(deployAddr, managementClient.getControllerClient());

            for (int i = 1; i <= 10; i++) {
                String msg = this.getClass().getSimpleName() + "10loop: " + i;
                sendMessage(session, sender, replyQueue, msg);
                expected.add("Reply: " + msg);
            }
            log.debug("Sent 10 more messages");

            Set<String> received = new TreeSet<String>();
            for (int i = 1; i <= (1 + 50 + 10 + 1); i++) {
                Message msg = receiver.receive(SECONDS.toMillis(RECEIVE_WAIT_S));
                assertNotNull("did not receive message with ordered number " + i +
                        " in " + SECONDS.toMillis(RECEIVE_WAIT_S) + " seconds", msg);
                String text = ((TextMessage) msg).getText();
                received.add(text);
                log.trace(i + ": " + text);
            }
            assertNull(receiver.receiveNoWait());

            assertEquals(expected, received);

        } finally {
            if(connection != null) {
                connection.close();
            }
            deployer.undeploy("mdb");
        }
    }

    // using DRM call for undeployment
    private Callable<Void> undeployTask() {
        return new Callable<Void>() {
            public Void call() throws Exception {
                ServerDeploymentManager deploymentManager = ServerDeploymentManager.Factory.create(managementClient.getControllerClient());
                final DeploymentPlan plan = deploymentManager.newDeploymentPlan().undeploy(MESSAGE_DRIVEN_BEAN + ".jar").build();
                deploymentManager.execute(plan).get(UNDEPLOYED_WAIT_S, SECONDS);
                return null;
            }
        };
    }
}
