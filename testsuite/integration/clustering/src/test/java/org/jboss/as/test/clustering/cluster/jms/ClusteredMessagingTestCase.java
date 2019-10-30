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

package org.jboss.as.test.clustering.cluster.jms;

import static org.jboss.as.test.shared.IntermittentFailure.thisTestIsFailingIntermittently;
import static org.junit.Assert.*;

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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusteredMessagingTestCase extends AbstractClusteringTestCase {

    protected final String jmsQueueName = "ClusteredMessagingTestCase-Queue";
    protected final String jmsQueueLookup = "jms/" + jmsQueueName;

    protected final String jmsTopicName = "ClusteredMessagingTestCase-Topic";
    protected final String jmsTopicLookup = "jms/" + jmsTopicName;

    public ClusteredMessagingTestCase() {
        super(TWO_NODES, new String[]{});
    }

    protected static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    protected static ModelControllerClient createClient2() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort() + 100,
                Authentication.getCallbackHandler());
    }

    @BeforeClass
    public static void beforeClass() {
        thisTestIsFailingIntermittently("WFLY-5390");
    }

    @Override
    public void beforeTestMethod() throws Exception {
        super.beforeTestMethod();

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

    @Override
    public void afterTestMethod() throws Exception {
        super.afterTestMethod();

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
    }

    @Test
    public void testClusteredQueue() throws Exception {
        InitialContext contextFromServer0 = createJNDIContextFromServer0();
        InitialContext contextFromServer1 = createJNDIContextFromServer1();

        String text = UUID.randomUUID().toString();

        // WIP test if the problem is that the view is not yet propagated
        Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

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

            // WIP test if the problem is that the view is not yet propagated
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

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
        env.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":8080");
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    protected static InitialContext createJNDIContextFromServer1() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        env.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":8180");
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    protected static void sendMessage(Context ctx, String destinationLookup, String text) throws NamingException {
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

    protected static void receiveMessage(JMSConsumer consumer, String expectedText) {
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
