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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
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
import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusteredMessagingTestCase extends AbstractClusteringTestCase {

    public static final String JMS_USERNAME = "guest";
    public static final String JMS_PASSWORD = "guest";

    private static final String jmsQueueName = ClusteredMessagingTestCase.class.getSimpleName() + "-Queue";
    private static final String jmsQueueLookup = "jms/" + jmsQueueName;

    private static final String jmsTopicName = ClusteredMessagingTestCase.class.getSimpleName() + "-Topic";
    private static final String jmsTopicLookup = "jms/" + jmsTopicName;

    public ClusteredMessagingTestCase() {
        super(TWO_NODES, new String[] {});
    }

    protected static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    protected static ModelControllerClient createClient2() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddress()), TestSuiteEnvironment.getServerPort() + getPortOffsetForNode(NODE_2), Authentication.getCallbackHandler());
    }

    @Override
    public void beforeTestMethod() throws Exception {
        super.beforeTestMethod();

        // Create JMS Queue and Topic (the ServerSetupTask won't do the trick since there is no deployment in this test case...)
        Arrays.stream(new ModelControllerClient[] { createClient1(), createClient2() }).forEach(client -> {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
            jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
            jmsOperations.createJmsTopic(jmsTopicName, "java:jboss/exported/" + jmsTopicLookup);
        });
    }

    @Override
    public void afterTestMethod() throws Exception {
        super.afterTestMethod();

        // Remove JMS Queue and Topic
        Arrays.stream(new ModelControllerClient[] { createClient1(), createClient2() }).forEach(client -> {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
            jmsOperations.removeJmsQueue(jmsQueueName);
            jmsOperations.removeJmsTopic(jmsTopicName);
        });
    }

    @Test
    public void testClusteredQueue() throws Exception {
        InitialContext contextFromServer1 = createJNDIContext(NODE_1);
        InitialContext contextFromServer2 = createJNDIContext(NODE_2);

        try {
            String text = UUID.randomUUID().toString();

            Destination destination1 = (Destination) contextFromServer1.lookup(jmsQueueLookup);
            Destination destination2 = (Destination) contextFromServer2.lookup(jmsQueueLookup);

            // send to the queue on server 1
            sendMessage(contextFromServer1, destination1, text);
            // receive it from the queue on server 2
            receiveMessage(contextFromServer2, destination2, text);

            String anotherText = UUID.randomUUID().toString();
            // send to the queue on server 2
            sendMessage(contextFromServer2, destination2, anotherText);
            // receive it from the queue on server 1
            receiveMessage(contextFromServer1, destination1, anotherText);
        } finally {
            contextFromServer1.close();
            contextFromServer2.close();
        }
    }

    @Test
    public void testClusteredTopic() throws Exception {
        InitialContext contextFromServer1 = createJNDIContext(NODE_1);
        InitialContext contextFromServer2 = createJNDIContext(NODE_2);

        try (
                JMSContext jmsContext1 = createJMSContext(createJNDIContext(NODE_1));
                JMSConsumer consumer1 = jmsContext1.createConsumer((Destination) contextFromServer1.lookup(jmsTopicLookup));
                JMSContext jmsContext2 = createJMSContext(createJNDIContext(NODE_2));
                JMSConsumer consumer2 = jmsContext2.createConsumer((Destination) contextFromServer2.lookup(jmsTopicLookup))
        ) {
            String text = UUID.randomUUID().toString();

            Destination destination1 = (Destination) contextFromServer1.lookup(jmsTopicLookup);
            Destination destination2 = (Destination) contextFromServer2.lookup(jmsTopicLookup);

            // send a message to the topic on server 1
            sendMessage(contextFromServer1, destination1, text);
            // consumers receive it on both servers
            receiveMessage(consumer1, text);
            receiveMessage(consumer2, text);

            String anotherText = UUID.randomUUID().toString();
            // send another message to topic on server 2
            sendMessage(contextFromServer2, destination2, anotherText);
            // consumers receive it on both servers
            receiveMessage(consumer1, anotherText);
            receiveMessage(consumer2, anotherText);
        } finally {
            contextFromServer1.close();
            contextFromServer2.close();
        }
    }

    protected static InitialContext createJNDIContext(String node) throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        int port = 8080 + getPortOffsetForNode(node);
        env.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":" + port);
        env.put(Context.SECURITY_PRINCIPAL, JMS_USERNAME);
        env.put(Context.SECURITY_CREDENTIALS, JMS_PASSWORD);
        return new InitialContext(env);
    }

    protected static void sendMessage(Context ctx, Destination destination, String text) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        assertNotNull(destination);

        try (JMSContext context = cf.createContext(JMS_USERNAME, JMS_PASSWORD)) {
            context.createProducer().send(destination, text);
        }
    }

    protected static JMSContext createJMSContext(Context ctx) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        return cf.createContext(JMS_USERNAME, JMS_PASSWORD);
    }

    protected static void receiveMessage(JMSConsumer consumer, String expectedText) {
        String text = consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
        assertNotNull(text);
        assertEquals(expectedText, text);
    }

    protected static void receiveMessage(Context ctx, Destination destination, String expectedText) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        assertNotNull(destination);

        try (JMSContext context = cf.createContext(JMS_USERNAME, JMS_PASSWORD)) {
            JMSConsumer consumer = context.createConsumer(destination);
            receiveMessage(consumer, expectedText);
        }
    }
}
