/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.mgmt;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.client.HornetQDestination;
import org.hornetq.jms.client.HornetQQueue;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the management API for hornetq-server resource.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c)2012 Red Hat, inc
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class HornetQServerManagementTestCase {

    private static HornetQConnectionFactory connectionFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {HashMap<String, Object> map = new HashMap<>();
        map.put("host", TestSuiteEnvironment.getServerAddress());
        map.put("port", 8080);
        map.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
        map.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");
        TransportConfiguration transportConfiguration =
                new TransportConfiguration(NettyConnectorFactory.class.getName(), map);
        connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        if (connectionFactory != null) {
            connectionFactory.close();
        }
    }

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testCloseConnectionForUser() throws Exception {
        Connection connection = connectionFactory.createConnection("guest", "guest");
        connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        final CountDownLatch connectionClosed = new CountDownLatch(1);

        connection.setExceptionListener(new ExceptionListener() {
            @Override
            public void onException(JMSException e) {
                connectionClosed.countDown();
            }
        });

        ModelNode operation = getHornetQServerOperation("close-connections-for-user");
        operation.get("user").set("guest");
        ModelNode result = execute(operation, true);
        assertTrue(result.isDefined());
        assertEquals(true, result.asBoolean());

        assertTrue(connectionClosed.await(500, MILLISECONDS));

        // connection is no longer valid
        try {
            connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Assert.fail("connection can no longer be used after it has been closed");
        } catch (JMSException e) {
        }
    }

    @Test
    public void testCloseConsumerConnectionsForAddress() throws Exception {

        JMSOperations adminSupport = JMSOperationsProvider.getInstance(managementClient);
        adminSupport.createJmsQueue(getQueueName(), getQueueJndiName());

        Connection connection = connectionFactory.createConnection("guest", "guest");
        HornetQQueue queue = HornetQDestination.createQueue(getQueueName());

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(queue);
        connection.start();

        final CountDownLatch connectionClosed = new CountDownLatch(1);

        connection.setExceptionListener(new ExceptionListener() {
            @Override
            public void onException(JMSException e) {
                connectionClosed.countDown();
            }
        });

        ModelNode operation = getHornetQServerOperation("close-consumer-connections-for-address");
        operation.get("address-match").set("jms.#");
        ModelNode result = execute(operation, true);
        assertTrue(result.isDefined());
        assertEquals(true, result.asBoolean());

        assertTrue(connectionClosed.await(500, MILLISECONDS));

        // consumer is no longer valid
        try {
            consumer.receiveNoWait();
            connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Assert.fail("consumer can no longer be used after it has been closed");
        } catch (JMSException e) {
        }

        // connection is no longer valid
        try {
            connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Assert.fail("connection can no longer be used after it has been closed");
        } catch (JMSException e) {
        }
    }

    private ModelNode getHornetQServerOperation(String operationName) {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        return getEmptyOperation(operationName, address);
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            if (!"success".equals(outcome)) {
                System.out.println(response);
            }
            assertEquals("success", outcome);
            return response.get("result");
        } else {
            if ("success".equals(outcome)) {
                System.out.println(response);
            }
            assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }

    private String getQueueName() {
        return getClass().getSimpleName();
    }

    private String getQueueJndiName() {
        return "queue/" + getQueueName();
    }

}