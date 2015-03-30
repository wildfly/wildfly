/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusteredMessagingTestCase {

    public static final String CONTAINER_0 = "messaging-container-0";
    public static final String CONTAINER_1 = "messaging-container-1";

    protected final String jmsQueueName = "ClusteredMessagingTestCase-Queue";
    protected final String jmsQueueLookup = "jms/" + jmsQueueName;

    protected final String jmsTopicName = "ClusteredMessagingTestCase-Topic";
    protected final String jmsTopicLookup = "jms/" + jmsTopicName;

    @ArquillianResource
    protected static ContainerController container;

    protected static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    protected static ModelControllerClient createClient2() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort() + 100,
                Authentication.getCallbackHandler());
    }

    @Before
    public void setUp() throws Exception {

        container.start(CONTAINER_0);
        container.start(CONTAINER_1);

        // both servers are started and configured
        assertTrue(container.isStarted(CONTAINER_0));
        assertTrue(container.isStarted(CONTAINER_1));

        try (ModelControllerClient client = createClient1()) {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
            jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
            jmsOperations.createJmsTopic(jmsTopicName, "java:jboss/exported/" + jmsTopicLookup);
        }
        try (ModelControllerClient client = createClient2()) {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
            jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
            jmsOperations.createJmsTopic(jmsTopicName, "java:jboss/exported/" + jmsTopicLookup);
        }
    }

    @After
    public void tearDown() throws Exception {
        try (ModelControllerClient client = createClient1()) {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
            jmsOperations.removeJmsQueue(jmsQueueName);
            jmsOperations.removeJmsTopic(jmsTopicName);
        }
        try (ModelControllerClient client = createClient2()) {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
            jmsOperations.removeJmsQueue(jmsQueueName);
            jmsOperations.removeJmsTopic(jmsTopicName);
        }

        container.stop(CONTAINER_0);
        container.stop(CONTAINER_1);
    }

    @Test
    public void testClusteredQueue() throws Exception {
        InitialContext contextFromServer0 = createJNDIContextFromServer0();
        InitialContext contextFromServer1 = createJNDIContextFromServer1();

        String text = UUID.randomUUID().toString();
        // send to the queue on server 0
        sendMessage(contextFromServer0, jmsQueueLookup, text);
        // receive it from the queue on server 1
        receiveMessage(contextFromServer1, jmsQueueLookup, text);

        String anotherText = UUID.randomUUID().toString();
        // send to the queue on server 1
        sendMessage(contextFromServer1, jmsQueueLookup, anotherText);
        // receive it from the queue on server 0
        receiveMessage(contextFromServer0, jmsQueueLookup, anotherText);
    }

    @Test
    public void testClusteredTopic() throws Exception {

        InitialContext contextFromServer0 = createJNDIContextFromServer0();
        InitialContext contextFromServer1 = createJNDIContextFromServer1();

        try (
                JMSContext jmsContext0 = createJMSContext(createJNDIContextFromServer0());
                JMSContext jmsContext1 = createJMSContext(createJNDIContextFromServer1())
        ) {
            JMSConsumer consumer0 = jmsContext0.createConsumer((Destination) contextFromServer0.lookup(jmsTopicLookup));
            JMSConsumer consumer1 = jmsContext1.createConsumer((Destination) contextFromServer1.lookup(jmsTopicLookup));

            String text = UUID.randomUUID().toString();
            // send a message to the topic on server 0
            sendMessage(contextFromServer0, jmsTopicLookup, text);
            // consumers receive it on both servers
            receiveMessage(consumer0, text);
            receiveMessage(consumer1, text);

            String anotherText = UUID.randomUUID().toString();
            // send another message to topic on server 1
            sendMessage(contextFromServer1, jmsTopicLookup, anotherText);
            // consumers receive it on both servers
            receiveMessage(consumer0, anotherText);
            receiveMessage(consumer1, anotherText);
        }
    }

    protected static InitialContext createJNDIContextFromServer0() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        env.put(Context.PROVIDER_URL, "http-remoting://" + TestSuiteEnvironment.getServerAddress() + ":8080");
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    protected static  InitialContext createJNDIContextFromServer1() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        env.put(Context.PROVIDER_URL, "http-remoting://" + TestSuiteEnvironment.getServerAddress() + ":8180");
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    protected static  void sendMessage(Context ctx, String destinationLookup, String text) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try (JMSContext context = cf.createContext("guest", "guest")) {
            context.createProducer().send(destination, text);
        }
    }

    protected static JMSContext createJMSContext(Context ctx) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        JMSContext context = cf.createContext("guest", "guest");
        return context;
    }

    protected static void receiveMessage(JMSConsumer consumer, String expectedText) throws NamingException {
        String text = consumer.receiveBody(String.class, 5000);
        assertNotNull(text);
        assertEquals(expectedText, text);
    }

    protected static void receiveMessage(Context ctx, String destinationLookup, String expectedText) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try (
                JMSContext context = cf.createContext("guest", "guest");
        ) {
            JMSConsumer consumer = context.createConsumer(destination);
            receiveMessage(consumer, expectedText);
        }
    }
}
