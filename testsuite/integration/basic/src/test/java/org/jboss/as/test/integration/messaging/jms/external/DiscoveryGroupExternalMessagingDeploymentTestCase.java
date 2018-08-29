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
package org.jboss.as.test.integration.messaging.jms.external;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jgroups.util.StackType;
import org.jgroups.util.Util;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that invoking a management operation that removes a JMS resource that is used by a deployed archive must fail:
 * the resource must not be removed and any depending services must be recovered.
 * The deployment must still be operating after the failing management operation.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DiscoveryGroupExternalMessagingDeploymentTestCase.SetupTask.class)
public class DiscoveryGroupExternalMessagingDeploymentTestCase {

    public static final boolean SKIP = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                return Util.checkForWindows() && (Util.getIpStackType() == StackType.IPv6);
            });
    public static final String QUEUE_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myQueue";
    public static final String TOPIC_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myTopic";
    public static final String REMOTE_PCF = "remote-artemis";

    private static final String QUEUE_NAME = "myQueue";
    private static final String TOPIC_NAME = "myTopic";
    private static final String DISCOVERY_GROUP_NAME = "dg1";
    private static final String MULTICAST_SOCKET_BINDING = "messaging-group";
   private static final String TESTSUITE_MCAST = System.getProperty("mcast", "230.0.0.4");

    @ArquillianResource
    private URL url;

    static class SetupTask extends SnapshotRestoreSetupTask {

        private static final Logger logger = Logger.getLogger(DiscoveryGroupExternalMessagingDeploymentTestCase.SetupTask.class);

        @Override
        public void doSetup(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
            if(SKIP) {
                logger.info("We are running on Windows with IPV6 stack");
                logger.info("[WFCI-32] Disable on Windows+IPv6 until CI environment is fixed");
                return;
            }
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ops.createJmsQueue(QUEUE_NAME, "/queue/" + QUEUE_NAME);
            ops.createJmsTopic(TOPIC_NAME, "/topic/" + TOPIC_NAME);
            execute(managementClient, addMulticastSocketBinding(MULTICAST_SOCKET_BINDING, TESTSUITE_MCAST, "${jboss.messaging.group.port:45700}"), true);
            execute(managementClient, addClientDiscoveryGroup(DISCOVERY_GROUP_NAME, MULTICAST_SOCKET_BINDING), true);
            ModelNode op = Operations.createRemoveOperation(getInitialPooledConnectionFactoryAddress());
            execute(managementClient, op, true);
            execute(managementClient, createBroadcastGroupWithSocketBinding(ops.getServerAddress(), "bg-group1", MULTICAST_SOCKET_BINDING, "http-connector"), true);
            execute(managementClient, createDiscoveryGroupWithSocketBinding(ops.getServerAddress(), "dg-group1", MULTICAST_SOCKET_BINDING), true);
            execute(managementClient, createClusterConnection(ops.getServerAddress(), "my-cluster", "jms", "http-connector", "dg-group1"), true);
            op = Operations.createAddOperation(getPooledConnectionFactoryAddress());
            op.get("transaction").set("xa");
            op.get("entries").add("java:/JmsXA java:jboss/DefaultJMSConnectionFactory");
            op.get("discovery-group").set(DISCOVERY_GROUP_NAME);
            execute(managementClient, op, true);
            op = Operations.createAddOperation(getClientTopicAddress());
            op.get("entries").add(TOPIC_LOOKUP);
            op.get("entries").add("/topic/myAwesomeClientTopic");
            execute(managementClient, op, true);
            op = Operations.createAddOperation(getClientQueueAddress());
            op.get("entries").add(QUEUE_LOOKUP);
            op.get("entries").add("/topic/myAwesomeClientQueue");
            execute(managementClient, op, true);
            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }

        private ModelNode execute(final org.jboss.as.arquillian.container.ManagementClient managementClient, final ModelNode op, final boolean expectSuccess) throws IOException {
            ModelNode response = managementClient.getControllerClient().execute(op);
            final String outcome = response.get("outcome").asString();
            if (expectSuccess) {
                assertEquals(response.toString(), "success", outcome);
                return response.get("result");
            } else {
                assertEquals("failed", outcome);
                return response.get("failure-description");
            }
        }

        ModelNode getPooledConnectionFactoryAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("pooled-connection-factory", REMOTE_PCF);
            return address;
        }

        ModelNode getClientTopicAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("external-jms-topic", TOPIC_NAME);
            return address;
        }

        ModelNode getClientQueueAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("external-jms-queue", QUEUE_NAME);
            return address;
        }

        ModelNode getInitialPooledConnectionFactoryAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "default");
            address.add("pooled-connection-factory", "activemq-ra");
            return address;
        }

        ModelNode addClientDiscoveryGroup(String name, String socketBinding) {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("discovery-group", name);

            ModelNode add = new ModelNode();
            add.get(OP).set(ADD);
            add.get(OP_ADDR).set(address);
            add.get("socket-binding").set(socketBinding);
            add.get("initial-wait-timeout").set(TimeoutUtil.adjust(30000));
            return add;
        }

        ModelNode addMulticastSocketBinding(String bindingName, String multicastAddress, String multicastPort) {
            ModelNode address = new ModelNode();
            address.add("socket-binding-group", "standard-sockets");
            address.add("socket-binding", bindingName);

            ModelNode socketBindingOp = new ModelNode();
            socketBindingOp.get(OP).set(ADD);
            socketBindingOp.get(OP_ADDR).set(address);
            socketBindingOp.get("multicast-address").set(multicastAddress);
            socketBindingOp.get("multicast-port").set(multicastPort);
            return socketBindingOp;
        }

        ModelNode createDiscoveryGroupWithSocketBinding(ModelNode serverAddress, String discoveryGroupName, String socketBinding) throws Exception {
            ModelNode address = serverAddress.clone();
            address.add("discovery-group", discoveryGroupName);
            ModelNode op = Operations.createAddOperation(address);
            op.get("socket-binding").set(socketBinding);
            return op;
        }

        ModelNode createBroadcastGroupWithSocketBinding(ModelNode serverAddress, String broadcastGroupName, String socketBinding, String connector) throws Exception {
            ModelNode address = serverAddress.clone();
            address.add("broadcast-group", broadcastGroupName);
            ModelNode op = Operations.createAddOperation(address);
            op.get("socket-binding").set(socketBinding);
            op.get("connectors").add(connector);
            return op;
        }

        ModelNode createClusterConnection(ModelNode serverAddress, String name, String address, String connector, String discoveryGroup) throws Exception {
            ModelNode opAddress = serverAddress.clone();
            opAddress.add("cluster-connection", name);
            ModelNode op = Operations.createAddOperation(opAddress);
            op.get("cluster-connection-address").set(address);
            op.get("connector-name").set(connector);
            op.get("discovery-group").set(discoveryGroup);
            return op;
        }
    }

    @Deployment
    public static WebArchive createArchive() {
        if(SKIP) {
            return create(WebArchive.class, "ClientMessagingDeploymentTestCase.war")
                    .addAsWebResource(new StringAsset("Root file"), "root-file.txt");
        }
        return create(WebArchive.class, "ClientMessagingDeploymentTestCase.war")
                .addClass(MessagingServlet.class)
                .addClasses(QueueMDB.class, TopicMDB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void before() {
        Assume.assumeFalse("[WFCI-32] Disable on Windows+IPv6 until CI environment is fixed", SKIP);
    }

    @Test
    public void testSendMessageInClientQueue() throws Exception {
        sendAndReceiveMessage(true);
    }

    @Test
    public void testSendMessageInClientTopic() throws Exception {
        sendAndReceiveMessage(false);
    }

    private void sendAndReceiveMessage(boolean sendToQueue) throws Exception {
        String destination = sendToQueue ? "queue" : "topic";
        String text = UUID.randomUUID().toString();
        URL url = new URL(this.url.toExternalForm() + "ClientMessagingDeploymentTestCase?destination=" + destination + "&text=" + text);
        String reply = HttpRequest.get(url.toExternalForm(), TimeoutUtil.adjust(10), TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals(text, reply);
    }
}
