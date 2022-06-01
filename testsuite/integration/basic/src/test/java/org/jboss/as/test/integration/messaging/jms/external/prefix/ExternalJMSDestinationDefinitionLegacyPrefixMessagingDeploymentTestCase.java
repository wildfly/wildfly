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
package org.jboss.as.test.integration.messaging.jms.external.prefix;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.SocketPermission;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jboss.arquillian.container.test.api.Deployer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the creation of destination on a 'remote' Artemis broker via @JMSDestinationDefinition using legacy prefix.
 * @author Emmanuel Hugonnet (c) 2021 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ExternalJMSDestinationDefinitionLegacyPrefixMessagingDeploymentTestCase.SetupTask.class)
public class ExternalJMSDestinationDefinitionLegacyPrefixMessagingDeploymentTestCase {

    public static final String QUEUE_LOOKUP = "java:/jms/AutomaticQueueCreationOnExternalMessagingDeploymentTestCase/myQueue";
    public static final String TOPIC_LOOKUP = "java:/jms/AutomaticQueueCreationOnExternalMessagingDeploymentTestCase/myTopic";
    public static final String REMOTE_PCF = "remote-artemis";

    public static final String QUEUE_NAME = "myExternalQueue";
    public static final String TOPIC_NAME = "myExternalTopic";
    private static Set<String> initialQueues = null;

    static class SetupTask implements ServerSetupTask {

        private static final Logger logger = Logger.getLogger(ExternalJMSDestinationDefinitionLegacyPrefixMessagingDeploymentTestCase.SetupTask.class);
        private final Map<String, AutoCloseable> snapshots = new HashMap<>();

        @Override
        public final void setup(ManagementClient managementClient, String containerId) throws Exception {
            snapshots.put(containerId, ServerSnapshot.takeSnapshot(managementClient));
            Set<String> runtimeQueues = listRuntimeQueues(managementClient);
            ServerReload.executeReloadAndWaitForCompletion(managementClient, true);
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            boolean needRemoteConnector = ops.isRemoteBroker();
            if (needRemoteConnector) {
                ops.addExternalRemoteConnector("remote-broker-connector", "messaging-activemq");
            } else {
                ops.addExternalHttpConnector("http-test-connector", "http", "http-acceptor");
            }
            //To avoid security limitations as default role guest doesn't have the 'manage' rights
            // /subsystem=messaging-activemq/server=default/security-setting=#/role=guest:write-attribute(name=manage, value=true)
            ops.disableSecurity();
            ModelNode op = Operations.createRemoveOperation(getInitialPooledConnectionFactoryAddress(ops.getServerAddress()));
            managementClient.getControllerClient().execute(op);
            op = Operations.createAddOperation(getPooledConnectionFactoryAddress());
            op.get("transaction").set("xa");
            op.get("entries").add("java:/JmsXA java:jboss/DefaultJMSConnectionFactory");
            if (needRemoteConnector) {
                op.get("connectors").add("remote-broker-connector");
            } else {
                op.get("connectors").add("http-test-connector");
            }
            op.get("enable-amq1-prefix").set(false);
            execute(managementClient, op, true);
            op = Operations.createRemoveOperation(getClientTopicAddress());
            managementClient.getControllerClient().execute(op);
            op = Operations.createRemoveOperation(getClientQueueAddress());
            managementClient.getControllerClient().execute(op);
            ops.close();
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
            ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            for (String runtimeQueue : runtimeQueues) {
                if (!"jms.queue.DLQ".equals(runtimeQueue) && !"jms.queue.ExpiryQueue".equals(runtimeQueue)) {
                    ops.removeCoreQueue(runtimeQueue);
                }
            }
            ops.close();
        }

        @Override
        public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            if (!ops.isRemoteBroker()) {
                Set<String> runtimeQueues = listRuntimeQueues(managementClient);
                runtimeQueues.remove("jms.queue.DLQ");
                runtimeQueues.remove("jms.queue.ExpiryQueue");
                if (!runtimeQueues.isEmpty()) {
                    Map<String, ModelNode> runtimes = new HashMap<>(runtimeQueues.size());
                    for (String runtimeQueue : runtimeQueues) {
                        runtimes.put(runtimeQueue, readRuntimeQueue(managementClient, ops, runtimeQueue));
                    }
                    ops.close();
                    ServerReload.executeReloadAndWaitForCompletion(managementClient, true);
                    ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
                    for (String runtimeQueue : runtimeQueues) {
                        String address = runtimes.get(runtimeQueue).get("queue-address").asString();
                        String routingType = runtimes.get(runtimeQueue).get("routing-type").asString();
                        boolean durable = runtimes.get(runtimeQueue).get("durable").asBoolean();
                        ops.addCoreQueue(runtimeQueue, address, durable, routingType);
                    }
                    ops.close();
                    ServerReload.executeReloadAndWaitForCompletion(managementClient, false);
                    ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
                    for (String runtimeQueue : runtimeQueues) {
                        ops.removeCoreQueue(runtimeQueue);
                    }
                    ops.close();
                    ServerReload.executeReloadAndWaitForCompletion(managementClient, false);
                }
            }
            AutoCloseable snapshot = snapshots.remove(containerId);
            if (snapshot != null) {
                snapshot.close();
            }
        }

        private ModelNode readRuntimeQueue(ManagementClient managementClient, JMSOperations ops, String name) throws IOException {
            ModelNode op = Operations.createReadResourceOperation(ops.getServerAddress().add("runtime-queue", name), false);
            op.get("include-runtime").set(true);
            op.get("include-defaults").set(true);
            return Operations.readResult(managementClient.getControllerClient().execute(op));
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

        private ModelNode getPooledConnectionFactoryAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("pooled-connection-factory", REMOTE_PCF);
            return address;
        }

        private ModelNode getClientTopicAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("external-jms-topic", TOPIC_NAME);
            return address;
        }

        private ModelNode getClientQueueAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("external-jms-queue", QUEUE_NAME);
            return address;
        }

        private ModelNode getInitialPooledConnectionFactoryAddress(ModelNode serverAddress) {
            ModelNode address = serverAddress.clone();
            address.add("pooled-connection-factory", "activemq-ra");
            return address;
        }
    }

    @Deployment(managed = false, testable = false, name = "external-queues")
    public static WebArchive createArchive() {
        return create(WebArchive.class, "ClientMessagingDeploymentTestCase.war")
                .addClasses(AnnotatedLegacyPrefixMessagingServlet.class, TimeoutUtil.class)
                .addClasses(QueueMDB.class, TopicMDB.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new SocketPermission("localhost", "resolve"),
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")), "permissions.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset(
                        "<jboss-web>"
                        + "<context-root>/test</context-root>"
                        + "</jboss-web>"), "jboss-web.xml");
    }

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Before
    public void setUp() throws IOException {
        if (initialQueues == null) {
            initialQueues = listRuntimeQueues(managementClient);
        }
        deployer.deploy("external-queues");
    }

    @After
    public void tearDown() throws IOException {
        deployer.undeploy("external-queues");
    }

    static Set<String> listRuntimeQueues(org.jboss.as.arquillian.container.ManagementClient managementClient) throws IOException {
        JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        ModelNode op = Operations.createOperation("read-children-names", ops.getServerAddress());
        op.get("child-type").set("runtime-queue");
        ModelNode result = Operations.readResult(managementClient.getControllerClient().execute(op));
        if(! result.isDefined()) {
            return new HashSet<>();
        }
        List<ModelNode> runtimeQueues = result.asList();
        return runtimeQueues.stream().map(ModelNode::asString).collect(Collectors.toSet());
    }

    @Test
    public void testSendMessage() throws Exception {
        sendAndReceiveMessage(true);
        sendAndReceiveMessage(false);
        checkRuntimeQueue();
    }

    private void sendAndReceiveMessage(boolean sendToQueue) throws Exception {
        String destination = sendToQueue ? "queue" : "topic";
        String text = UUID.randomUUID().toString();
        String serverUrl = managementClient.getWebUri().toURL().toString() + "/test/";
        URL servletUrl = new URL(serverUrl + "ClientMessagingDeploymentTestCase?destination=" + destination + "&text=" + text);
        String reply = HttpRequest.get(servletUrl.toExternalForm(), TimeoutUtil.adjust(10), TimeUnit.SECONDS);
        assertNotNull(reply);
        assertEquals(text, reply);
        checkRuntimeQueue();
    }

    private void checkRuntimeQueue() throws IOException {
        JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        if (!ops.isRemoteBroker()) {
            ModelNode op = Operations.createOperation("read-children-names", ops.getServerAddress());
            op.get("child-type").set("runtime-queue");
            List<ModelNode> runtimeQueues = Operations.readResult(managementClient.getControllerClient().execute(op)).asList();
            Set<String> queues = runtimeQueues.stream().map(ModelNode::asString).collect(Collectors.toSet());
            Assert.assertEquals("expected:<" + (initialQueues.size() + 2) + "> but was:<" + queues.size()+ ">" + queues.toString() , initialQueues.size() + 2, queues.size());
            Assert.assertTrue("We should have myExternalQueue queue", queues.contains("jms.queue.myExternalQueue"));
            queues.removeAll(initialQueues);
            queues.remove("jms.queue.myExternalQueue");
            String topicId = queues.iterator().next();
            checkRuntimeQueue(ops, "jms.queue.myExternalQueue", "ANYCAST", "jms.queue.myExternalQueue");
            checkRuntimeQueue(ops, topicId, "MULTICAST", "jms.topic.myExternalTopic");
        }
    }

    private void checkRuntimeQueue(JMSOperations ops, String name, String expectedRoutingType, String expectedQueueAddress) throws IOException {
        ModelNode op = Operations.createReadResourceOperation(ops.getServerAddress().add("runtime-queue", name), false);
        op.get("include-runtime").set(true);
        op.get("include-defaults").set(true);
        ModelNode result = Operations.readResult(managementClient.getControllerClient().execute(op));
        Assert.assertEquals(expectedRoutingType, result.require("routing-type").asString());
        Assert.assertEquals(expectedQueueAddress, result.require("queue-address").asString());
    }
}
