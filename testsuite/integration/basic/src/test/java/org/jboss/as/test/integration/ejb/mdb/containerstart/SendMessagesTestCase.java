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

import org.jboss.as.test.shared.TestUtils;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQQueue;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.util.Base64;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Part of migration EJB testsuite (JBAS-7922) to AS7 [JIRA JBQA-5483]. This test covers jira AS7-687 which aims to migrate this
 * test to new testsuite.
 * 
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SendMessagesTestCase {
    private static final Logger log = Logger.getLogger(SendMessagesTestCase.class);

    private static final String MBEAN = "mbean-containerstart";
    private static final String SINGLETON = "single-containerstart";

    private static ModelControllerClient mcClient;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private static String QUEUE_SEND = "queue/sendMessage";
    private static String QUEUE_REPLY = "queue/replyMessage";
    
    @ArquillianResource
    Deployer deployer;

    @Deployment(name = "singleton", order = 1, testable = false, managed = true)
    public static Archive<?> deploymentMbean() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SINGLETON + ".jar")
                .addClasses(HelperSingleton.class, HelperSingletonImpl.class);
        log.info(jar.toString(true));
        return jar;
    }
    
    @Deployment(name = "mdb", order = 2, testable = false, managed = false)
    public static Archive<?> deploymentMdb() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MBEAN + ".jar")
                .addClasses(ReplyingMDB.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment." + SINGLETON + ".jar\n"), "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(name = "test", order = 3, managed = true, testable = true)
    public static Archive<?> deploymentTest() {
        final JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "test-containerstart.jar")
                .addClass(SendMessagesTestCase.class)
                .addClass(Base64.class)
                .addClass(ShrinkWrapUtils.class)
                .addClass(PollingUtils.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, deployment." + SINGLETON + ".jar \n"), "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }
    
    @BeforeClass
    public static void init() throws Exception {
        mcClient = TestUtils.getModelControllerClient();
        createQueue(mcClient, QUEUE_SEND);
        createQueue(mcClient, QUEUE_REPLY);
    }
    
    @AfterClass 
    public static void clearUp() throws Exception {
        destroyQueue(mcClient, QUEUE_SEND);
        destroyQueue(mcClient, QUEUE_REPLY);
        mcClient.close();
    }
    
    private InitialContext getInitialContext() throws NamingException {
        final Hashtable<String,String> jndiProperties = new Hashtable<String,String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY,"org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(jndiProperties);
    }
    
    private static void createQueue(ModelControllerClient modelClient, String queueName) throws Exception {
        ModelNode addJmsQueue = getQueueAddr(queueName);
        addJmsQueue.get(ClientConstants.OP).set("add");
        addJmsQueue.get("entries").add("java:jboss/" + queueName);
        applyUpdate(addJmsQueue, modelClient);
    }
    
    private static void destroyQueue(ModelControllerClient modelClient, String queueName) throws Exception {
        ModelNode removeJmsQueue = getQueueAddr(queueName);
        removeJmsQueue.get(ClientConstants.OP).set("remove");
        applyUpdate(removeJmsQueue, modelClient);
    }
    
    private static ModelNode getQueueAddr(String name) {
        final ModelNode queueAddr = new ModelNode();
        queueAddr.get(ClientConstants.OP_ADDR).add("subsystem", "messaging");
        queueAddr.get(ClientConstants.OP_ADDR).add("hornetq-server", "default");
        queueAddr.get(ClientConstants.OP_ADDR).add("jms-queue", name);
        return queueAddr;
    }
    
    public static void applyUpdate(ModelNode update, final ModelControllerClient client) throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }
    // end: jms ops
    
    private int awaitSingleton(String where) throws Exception {
        HelperSingleton helper = (HelperSingleton) getInitialContext().lookup("ejb:/" + SINGLETON + "//HelperSingletonImpl!org.jboss.as.test.integration.ejb.mdb.containerstart.HelperSingleton");
        // HelperSingleton helper = (HelperSingleton) new InitialContext().lookup("java:global/" + SINGLETON + "/HelperSingletonImpl!org.jboss.as.test.integration.ejb.mdb.containerstart.HelperSingleton");
        return helper.await(where, 10, TimeUnit.SECONDS);
    }
    
    private static void sendMessage(Session session, MessageProducer sender, Queue replyQueue, String txt) throws JMSException {
        TextMessage msg = session.createTextMessage(txt);
        msg.setJMSReplyTo(replyQueue);
        sender.send(msg);
    }

    /**
     * We need to run this on client side (necessity of calling deploy through modelcontroller) - currently we need to use HornetQ api for getting queue.
     */
    @Test
    public void testShutdown() throws Exception {
        Session session = null;
        MessageProducer sender = null;
        MessageConsumer receiver = null;
        Connection connection = null;
        
        try {
            deployer.deploy("mdb");
            
            Queue queue = new HornetQQueue(QUEUE_SEND);
            Queue replyQueue = new HornetQQueue(QUEUE_REPLY);
            Map<String, Object> connectionParams = new HashMap<String, Object>();
            connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");       
            connectionParams.put(TransportConstants.PORT_PROP_NAME, 5445);
            TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams);
            ConnectionFactory cf = (ConnectionFactory)  HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);           
            connection = cf.createConnection("guest", "guest");
            
            
            connection.start();
            session = connection.createSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            sender = session.createProducer(queue);
            receiver = session.createConsumer(replyQueue);

            SortedSet<String> expected = new TreeSet<String>();
            sendMessage(session, sender, replyQueue, "await");
            expected.add("Reply: await");

            int awaitInt = awaitSingleton("first test"); // receive of first
            log.debug("testsuite: awaitSingleton() returned: " + awaitInt);
            Future<?> undeployed = executor.submit(undeployTask());
            // Hmm, we really want to wait for MDB.stop going into the semaphore
            Thread.sleep(1000);
            for (int i = 0; i < 50; i++) {
                String msg = "Do not lose! (" + i + ")";
                sendMessage(session, sender, replyQueue, msg); // should be bounced by BlockContainerShutdownInterceptor
                expected.add("Reply: " + msg);
            }
            awaitInt = awaitSingleton("second test"); // finalizing first
            log.debug("testsuite: awaitSingleton()2 returned:: " + awaitInt);

            undeployed.get(10, SECONDS);

            // deploying via arquillian does not work for some reason
            final ModelNode deployAddr = new ModelNode();
            deployAddr.get(ClientConstants.OP_ADDR).add("deployment", MBEAN + ".jar");
            deployAddr.get(ClientConstants.OP).set("deploy");
            applyUpdate(deployAddr, mcClient);

            for (int i = 0; i < 10; i++) {
                String msg = "Some more (" + i + ")";
                sendMessage(session, sender, replyQueue, msg);
                expected.add("Reply: " + msg);
            }
            log.debug("Some more messages sent");

            SortedSet<String> received = new TreeSet<String>();
            for (int i = 0; i < (1 + 50 + 10); i++) {
                Message msg = receiver.receive(SECONDS.toMillis(10));
                Assert.assertNotNull(msg);
                String text = ((TextMessage) msg).getText();
                received.add(text);
                log.info(i + ": " + text);
            }

            Assert.assertEquals(expected, received);

            connection.stop();
        } finally {
            if(connection != null) {
                connection.close();
            }
            if(session != null) {
                session.close();
            }
            if(sender != null) {
                sender.close();
            }
            if(receiver != null) {
                receiver.close();
            }
            if(executor != null) {
                executor.shutdown();
            }
            deployer.undeploy("mdb");
        }
    }

    // using DRM call for undeployment
    private Callable<Void> undeployTask() {
        return new Callable<Void>() {
            public Void call() throws Exception {
                log.debug("undpeployTask: undeploying...");                
                final ModelNode undeployAddr = new ModelNode();
                undeployAddr.get(ClientConstants.OP_ADDR).add("deployment", MBEAN + ".jar");
                undeployAddr.get(ClientConstants.OP).set("undeploy");
                SendMessagesTestCase.applyUpdate(undeployAddr, SendMessagesTestCase.mcClient);
                return null;
            }
        };
    }
}
