/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.messaging.client.messaging;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.resource.spi.IllegalStateException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;
import static org.junit.Assert.assertNotNull;

/**
 * Demo using the AS management API to create and destroy a Artemis core queue.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MessagingClientTestCase {

    private final String queueName = "queue.standalone";
    private final int messagingPort = 5445;

    private String messagingSocketBindingName = "messaging";
    private String remoteAcceptorName = "netty";

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testMessagingClientUsingMessagingPort() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getWebUri().getHost(), messagingPort,
                false);
        doMessagingClient(sf);
        sf.close();
    }

    @Test
    public void testMessagingClientUsingHTTPPort() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getWebUri().getHost(), managementClient.getWebUri().getPort(),
                true);
        doMessagingClient(sf);
        sf.close();
    }

    public void loop() throws Exception {
        for (int i = 0; i < 1000; i++) {
            //System.out.println("i = " + i);
            testMessagingClientUsingHTTPPort();
        }
    }

    private void doMessagingClient(ClientSessionFactory sf) throws Exception {

        // Check if the queue exists
        if (!queueExists(queueName, sf)) {
            throw new IllegalStateException();
        }

        ClientSession session = null;
        try {
            session = sf.createSession("guest", "guest", false, true, true, false, 1);
            ClientProducer producer = session.createProducer(queueName);
            ClientMessage message = session.createMessage(false);

            final String propName = "myprop";
            message.putStringProperty(propName, "Hello sent at " + new Date());

            producer.send(message);

            ClientConsumer messageConsumer = session.createConsumer(queueName);
            session.start();

            ClientMessage messageReceived = messageConsumer.receive(1000);
            assertNotNull("a message MUST have been received", messageReceived);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private boolean queueExists(final String queueName, final ClientSessionFactory sf) throws ActiveMQException {
        final ClientSession session = sf.createSession("guest", "guest", false, false, false, false, 1);
        try {
            final ClientSession.QueueQuery query = session.queueQuery(new SimpleString(queueName));
            return query.isExists();
        } finally {
            session.close();
        }
    }

    private ClientSessionFactory createClientSessionFactory(String host, int port, boolean httpUpgradeEnabled) throws Exception {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(TransportConstants.HOST_PROP_NAME, host);
        properties.put(TransportConstants.PORT_PROP_NAME, port);
        properties.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, httpUpgradeEnabled);
        if (httpUpgradeEnabled) {
            properties.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");
        }
        final TransportConfiguration configuration = new TransportConfiguration(NettyConnectorFactory.class.getName(), properties);
        return ActiveMQClient.createServerLocatorWithoutHA(configuration).createSessionFactory();
    }

    @Before
    public void setup() throws Exception {

        createSocketBinding(managementClient.getControllerClient(), messagingSocketBindingName, messagingPort);
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.createRemoteAcceptor(remoteAcceptorName, messagingSocketBindingName, null);
        jmsOperations.addCoreQueue(queueName, queueName, true);

        ServerReload.reloadIfRequired(managementClient.getControllerClient());
    }

    @After
    public void tearDown() throws Exception {

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.removeRemoteAcceptor(remoteAcceptorName);
        jmsOperations.removeCoreQueue(queueName);
        removeSocketBinding(managementClient.getControllerClient(), messagingSocketBindingName);

        ServerReload.reloadIfRequired(managementClient.getControllerClient());
    }

    public final void createSocketBinding(final ModelControllerClient modelControllerClient, final String name, int port) {

        ModelNode model = new ModelNode();
        model.get(ClientConstants.OP).set(ADD);
        model.get(ClientConstants.OP_ADDR).add("socket-binding-group", "standard-sockets");
        model.get(ClientConstants.OP_ADDR).add("socket-binding", name);
        model.get("interface").set("public");
        model.get("port").set(port);
        model.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);

        try {
            execute(modelControllerClient, model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public final void removeSocketBinding(final ModelControllerClient modelControllerClient, final String name) {
        ModelNode model = new ModelNode();
        model.get(ClientConstants.OP).set(REMOVE_OPERATION);
        model.get(ClientConstants.OP_ADDR).add("socket-binding-group", "standard-sockets");
        model.get(ClientConstants.OP_ADDR).add("socket-binding", name);
        model.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
        try {
            execute(modelControllerClient, model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes the operation and returns the result if successful. Else throws an exception
     *
     * @param modelControllerClient
     * @param operation
     * @return
     * @throws IOException
     */
    private ModelNode execute(final ModelControllerClient modelControllerClient, final ModelNode operation) throws IOException {
        final ModelNode result = modelControllerClient.execute(operation);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            //logger.trace("Operation " + operation.toString() + " successful");
            return result;
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new RuntimeException(failureDesc);
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }
    }
}
