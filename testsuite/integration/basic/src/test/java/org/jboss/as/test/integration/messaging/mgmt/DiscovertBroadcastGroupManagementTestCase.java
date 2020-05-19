/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.messaging.mgmt;



import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DiscovertBroadcastGroupManagementTestCase extends ContainerResourceMgmtTestBase {
    private static final String MULTICAST_SOCKET_BINDING = "messaging-group";
    private static final String JGROUPS_CLUSTER = "activemq-cluster";

    @Test
    public void testServerBroadcastGroup() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createBroadcastGroupWithSocketBinding(jmsOperations.getServerAddress(), "bg-group1", MULTICAST_SOCKET_BINDING, "http-connector"), true);
        final ModelNode legacyBgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/broadcast-group=bg-group1").toModelNode();
        final ModelNode socketBgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/socket-broadcast-group=bg-group1").toModelNode();
        ModelNode broadcastLegacy = executeOperation(Operations.createReadResourceOperation(legacyBgAddress));
        broadcastLegacy.remove("jgroups-channel");
        broadcastLegacy.remove("jgroups-cluster");
        broadcastLegacy.remove("jgroups-stack");
        ModelNode broadcast = executeOperation(Operations.createReadResourceOperation(socketBgAddress));
        Assert.assertEquals(broadcast.toString(), broadcastLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/server=default/jgroups-broadcast-group=bg-group1");

        executeOperation(Operations.createWriteAttributeOperation(socketBgAddress, "broadcast-period", 5000));
        Assert.assertEquals(5000L, executeOperation(Operations.createReadAttributeOperation(legacyBgAddress, "broadcast-period")).asLong());
        Assert.assertEquals(5000L, executeOperation(Operations.createReadAttributeOperation(socketBgAddress, "broadcast-period")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(socketBgAddress, "broadcast-period"));
        Assert.assertEquals(2000L, executeOperation(Operations.createReadAttributeOperation(legacyBgAddress, "broadcast-period")).asLong());
        Assert.assertEquals(2000L, executeOperation(Operations.createReadAttributeOperation(socketBgAddress, "broadcast-period")).asLong());

        executeOperation(Operations.createRemoveOperation(socketBgAddress));
        checkNoResource(socketBgAddress);
        checkNoResource(legacyBgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testServerBroadcastGroupCluster() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createBroadcastGroupWithJGroupsCluster(jmsOperations.getServerAddress(), "bg-group1", JGROUPS_CLUSTER, "http-connector"), true);
        final ModelNode legacyBgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/broadcast-group=bg-group1").toModelNode();
        final ModelNode jgroupBgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/jgroups-broadcast-group=bg-group1").toModelNode();
        ModelNode broadcastLegacy = executeOperation(Operations.createReadResourceOperation(legacyBgAddress));
        broadcastLegacy.remove("socket-binding");
        ModelNode broadcast = executeOperation(Operations.createReadResourceOperation(jgroupBgAddress));
        Assert.assertEquals(broadcast.toString(), broadcastLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/server=default/socket-broadcast-group=bg-group1");

        executeOperation(Operations.createWriteAttributeOperation(jgroupBgAddress, "broadcast-period", 5000));
        Assert.assertEquals(5000L, executeOperation(Operations.createReadAttributeOperation(legacyBgAddress, "broadcast-period")).asLong());
        Assert.assertEquals(5000L, executeOperation(Operations.createReadAttributeOperation(jgroupBgAddress, "broadcast-period")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(jgroupBgAddress, "broadcast-period"));
        Assert.assertEquals(2000L, executeOperation(Operations.createReadAttributeOperation(legacyBgAddress, "broadcast-period")).asLong());
        Assert.assertEquals(2000L, executeOperation(Operations.createReadAttributeOperation(jgroupBgAddress, "broadcast-period")).asLong());

        executeOperation(Operations.createRemoveOperation(jgroupBgAddress));
        checkNoResource(jgroupBgAddress);
        checkNoResource(legacyBgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testServerDiscoveryGroup() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createDiscoveryGroupWithSocketBinding(jmsOperations.getServerAddress(), "dg-group1", MULTICAST_SOCKET_BINDING), true);
        final ModelNode legacyDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/discovery-group=dg-group1").toModelNode();
        final ModelNode socketDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/socket-discovery-group=dg-group1").toModelNode();
        ModelNode discoveryLegacy = executeOperation(Operations.createReadResourceOperation(legacyDgAddress));
        discoveryLegacy.remove("jgroups-channel");
        discoveryLegacy.remove("jgroups-cluster");
        discoveryLegacy.remove("jgroups-stack");
        ModelNode discovery = executeOperation(Operations.createReadResourceOperation(socketDgAddress));
        Assert.assertEquals(discovery.toString(), discoveryLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/server=default/jgroups-discovery-group=dg-group1");

        executeOperation(Operations.createWriteAttributeOperation(socketDgAddress, "initial-wait-timeout", 50000));
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "initial-wait-timeout")).asLong());
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "initial-wait-timeout")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(socketDgAddress, "initial-wait-timeout"));
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "initial-wait-timeout")).asLong());
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "initial-wait-timeout")).asLong());

        executeOperation(Operations.createRemoveOperation(socketDgAddress));
        checkNoResource(socketDgAddress);
        checkNoResource(legacyDgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testServerDiscoveryGroupCluster() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createDiscoveryGroupWithJGroupsCluster(jmsOperations.getServerAddress(), "dg-group1", JGROUPS_CLUSTER), true);
        final ModelNode legacyDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/discovery-group=dg-group1").toModelNode();
        final ModelNode socketDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/jgroups-discovery-group=dg-group1").toModelNode();
        ModelNode discoveryLegacy = executeOperation(Operations.createReadResourceOperation(legacyDgAddress));
        discoveryLegacy.remove("socket-binding");
        ModelNode discovery = executeOperation(Operations.createReadResourceOperation(socketDgAddress));
        Assert.assertEquals(discovery.toString(), discoveryLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/server=default/socket-discovery-group=dg-group1");

        executeOperation(Operations.createWriteAttributeOperation(socketDgAddress, "initial-wait-timeout", 50000));
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "initial-wait-timeout")).asLong());
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "initial-wait-timeout")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(socketDgAddress, "initial-wait-timeout"));
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "initial-wait-timeout")).asLong());
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "initial-wait-timeout")).asLong());

        executeOperation(Operations.createRemoveOperation(socketDgAddress));
        checkNoResource(socketDgAddress);
        checkNoResource(legacyDgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testExternalDiscoveryGroup() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        jmsOperations.createSocketBinding(MULTICAST_SOCKET_BINDING, "public", 5444);
        executeOperation(createDiscoveryGroupWithSocketBinding(jmsOperations.getSubsystemAddress(), "dg-group1", MULTICAST_SOCKET_BINDING), true);
        final ModelNode legacyDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/discovery-group=dg-group1").toModelNode();
        final ModelNode socketDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/socket-discovery-group=dg-group1").toModelNode();
        ModelNode discoveryLegacy = executeOperation(Operations.createReadResourceOperation(legacyDgAddress));
        discoveryLegacy.remove("jgroups-channel");
        discoveryLegacy.remove("jgroups-cluster");
        discoveryLegacy.remove("jgroups-stack");
        ModelNode discovery = executeOperation(Operations.createReadResourceOperation(socketDgAddress));
        Assert.assertEquals(discovery.toString(), discoveryLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/jgroups-discovery-group=dg-group1");

        executeOperation(Operations.createWriteAttributeOperation(socketDgAddress, "refresh-timeout", 50000));
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "refresh-timeout")).asLong());
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "refresh-timeout")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(socketDgAddress, "refresh-timeout"));
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "refresh-timeout")).asLong());
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "refresh-timeout")).asLong());

        executeOperation(Operations.createRemoveOperation(socketDgAddress));
        checkNoResource(socketDgAddress);
        checkNoResource(legacyDgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());

        executeOperation(Operations.createRemoveOperation(PathAddress.parseCLIStyleAddress("/socket-binding-group=standard-sockets/socket-binding=" + MULTICAST_SOCKET_BINDING).toModelNode()));
        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }
@Test
    public void testExternalDiscoveryGroupCluster() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createDiscoveryGroupWithJGroupsCluster(jmsOperations.getSubsystemAddress(), "dg-group1", JGROUPS_CLUSTER), true);
        final ModelNode legacyDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/discovery-group=dg-group1").toModelNode();
        final ModelNode jgroupDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/jgroups-discovery-group=dg-group1").toModelNode();
        ModelNode discoveryLegacy = executeOperation(Operations.createReadResourceOperation(legacyDgAddress));
        discoveryLegacy.remove("socket-binding");
        ModelNode discovery = executeOperation(Operations.createReadResourceOperation(jgroupDgAddress));
        Assert.assertEquals(discovery.toString(), discoveryLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/socket-discovery-group=dg-group1");

        executeOperation(Operations.createWriteAttributeOperation(jgroupDgAddress, "refresh-timeout", 50000));
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "refresh-timeout")).asLong());
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(jgroupDgAddress, "refresh-timeout")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(jgroupDgAddress, "refresh-timeout"));
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "refresh-timeout")).asLong());
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(jgroupDgAddress, "refresh-timeout")).asLong());

        executeOperation(Operations.createRemoveOperation(jgroupDgAddress));
        checkNoResource(jgroupDgAddress);
        checkNoResource(legacyDgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testServerShallowBroadcastGroup() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createShallowBroadcastGroupWithSocketBinding(jmsOperations.getServerAddress(), "bg-group1", MULTICAST_SOCKET_BINDING, "http-connector"), true);
        final ModelNode legacyBgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/broadcast-group=bg-group1").toModelNode();
        final ModelNode socketBgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/socket-broadcast-group=bg-group1").toModelNode();
        ModelNode broadcastLegacy = executeOperation(Operations.createReadResourceOperation(legacyBgAddress));
        broadcastLegacy.remove("jgroups-channel");
        broadcastLegacy.remove("jgroups-cluster");
        broadcastLegacy.remove("jgroups-stack");
        ModelNode broadcast = executeOperation(Operations.createReadResourceOperation(socketBgAddress));
        Assert.assertEquals(broadcast.toString(), broadcastLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/server=default/jgroups-broadcast-group=bg-group1");

        executeOperation(Operations.createWriteAttributeOperation(legacyBgAddress, "broadcast-period", 5000));
        Assert.assertEquals(5000L, executeOperation(Operations.createReadAttributeOperation(legacyBgAddress, "broadcast-period")).asLong());
        Assert.assertEquals(5000L, executeOperation(Operations.createReadAttributeOperation(socketBgAddress, "broadcast-period")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(socketBgAddress, "broadcast-period"));
        Assert.assertEquals(2000L, executeOperation(Operations.createReadAttributeOperation(legacyBgAddress, "broadcast-period")).asLong());
        Assert.assertEquals(2000L, executeOperation(Operations.createReadAttributeOperation(socketBgAddress, "broadcast-period")).asLong());

        executeOperation(Operations.createRemoveOperation(legacyBgAddress));
        checkNoResource(legacyBgAddress);
        checkNoResource(socketBgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testServerShallowBroadcastGroupCluster() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createShallowBroadcastGroupWithhJGroupCluster(jmsOperations.getServerAddress(), "bg-group1", JGROUPS_CLUSTER, "http-connector"), true);
        final ModelNode legacyBgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/broadcast-group=bg-group1").toModelNode();
        final ModelNode jgroupBgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/jgroups-broadcast-group=bg-group1").toModelNode();
        ModelNode broadcastLegacy = executeOperation(Operations.createReadResourceOperation(legacyBgAddress));
        broadcastLegacy.remove("socket-binding");
        ModelNode broadcast = executeOperation(Operations.createReadResourceOperation(jgroupBgAddress));
        Assert.assertEquals(broadcast.toString(), broadcastLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/server=default/socket-broadcast-group=bg-group1");

        executeOperation(Operations.createWriteAttributeOperation(legacyBgAddress, "broadcast-period", 5000));
        Assert.assertEquals(5000L, executeOperation(Operations.createReadAttributeOperation(legacyBgAddress, "broadcast-period")).asLong());
        Assert.assertEquals(5000L, executeOperation(Operations.createReadAttributeOperation(jgroupBgAddress, "broadcast-period")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(jgroupBgAddress, "broadcast-period"));
        Assert.assertEquals(2000L, executeOperation(Operations.createReadAttributeOperation(legacyBgAddress, "broadcast-period")).asLong());
        Assert.assertEquals(2000L, executeOperation(Operations.createReadAttributeOperation(jgroupBgAddress, "broadcast-period")).asLong());

        executeOperation(Operations.createRemoveOperation(legacyBgAddress));
        checkNoResource(legacyBgAddress);
        checkNoResource(jgroupBgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testServerShallowDiscoveryGroup() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createShallowDiscoveryGroupWithSocketBinding(jmsOperations.getServerAddress(), "dg-group1", MULTICAST_SOCKET_BINDING), true);
        final ModelNode legacyDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/discovery-group=dg-group1").toModelNode();
        final ModelNode socketDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/socket-discovery-group=dg-group1").toModelNode();
        ModelNode discoveryLegacy = executeOperation(Operations.createReadResourceOperation(legacyDgAddress));
        discoveryLegacy.remove("jgroups-channel");
        discoveryLegacy.remove("jgroups-cluster");
        discoveryLegacy.remove("jgroups-stack");
        ModelNode discovery = executeOperation(Operations.createReadResourceOperation(socketDgAddress));
        Assert.assertEquals(discovery.toString(), discoveryLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/server=default/jgroups-discovery-group=dg-group1");

        executeOperation(Operations.createWriteAttributeOperation(legacyDgAddress, "refresh-timeout", 50000));
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "refresh-timeout")).asLong());
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "refresh-timeout")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(socketDgAddress, "refresh-timeout"));
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "refresh-timeout")).asLong());
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "refresh-timeout")).asLong());

        executeOperation(Operations.createRemoveOperation(legacyDgAddress));
        checkNoResource(legacyDgAddress);
        checkNoResource(socketDgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testServerShallowDiscoveryGroupCluster() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createShallowDiscoveryGroupWithJGroupCluster(jmsOperations.getServerAddress(), "dg-group1", JGROUPS_CLUSTER), true);
        final ModelNode legacyDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/discovery-group=dg-group1").toModelNode();
        final ModelNode jgroupDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/jgroups-discovery-group=dg-group1").toModelNode();
        ModelNode discoveryLegacy = executeOperation(Operations.createReadResourceOperation(legacyDgAddress));
        discoveryLegacy.remove("socket-binding");
        ModelNode discovery = executeOperation(Operations.createReadResourceOperation(jgroupDgAddress));
        Assert.assertEquals(discovery.toString(), discoveryLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/server=default/socket-discovery-group=dg-group1");

        executeOperation(Operations.createWriteAttributeOperation(legacyDgAddress, "refresh-timeout", 50000));
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "refresh-timeout")).asLong());
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(jgroupDgAddress, "refresh-timeout")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(jgroupDgAddress, "refresh-timeout"));
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "refresh-timeout")).asLong());
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(jgroupDgAddress, "refresh-timeout")).asLong());

        executeOperation(Operations.createRemoveOperation(legacyDgAddress));
        checkNoResource(legacyDgAddress);
        checkNoResource(jgroupDgAddress);
        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testExternalShallowDiscoveryGroup() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        jmsOperations.createSocketBinding(MULTICAST_SOCKET_BINDING, "public", 5444);
        executeOperation(createShallowDiscoveryGroupWithSocketBinding(jmsOperations.getSubsystemAddress(), "dg-group1", MULTICAST_SOCKET_BINDING), true);
        final ModelNode legacyDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/discovery-group=dg-group1").toModelNode();
        final ModelNode socketDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/socket-discovery-group=dg-group1").toModelNode();
        ModelNode discoveryLegacy = executeOperation(Operations.createReadResourceOperation(legacyDgAddress));
        discoveryLegacy.remove("jgroups-channel");
        discoveryLegacy.remove("jgroups-cluster");
        discoveryLegacy.remove("jgroups-stack");
        ModelNode discovery = executeOperation(Operations.createReadResourceOperation(socketDgAddress));
        Assert.assertEquals(discovery.toString(), discoveryLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/jgroups-discovery-group=dg-group1");

        executeOperation(Operations.createWriteAttributeOperation(legacyDgAddress, "initial-wait-timeout", 50000));
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "initial-wait-timeout")).asLong());
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "initial-wait-timeout")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(socketDgAddress, "initial-wait-timeout"));
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "initial-wait-timeout")).asLong());
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(socketDgAddress, "initial-wait-timeout")).asLong());

        executeOperation(Operations.createRemoveOperation(legacyDgAddress));
        checkNoResource(legacyDgAddress);
        checkNoResource(socketDgAddress);

        executeOperation(Operations.createRemoveOperation(PathAddress.parseCLIStyleAddress("/socket-binding-group=standard-sockets/socket-binding=" + MULTICAST_SOCKET_BINDING).toModelNode()));
        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    @Test
    public void testExternalShallowDiscoveryGroupCluster() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(getModelControllerClient());
        executeOperation(createShallowDiscoveryGroupWithJGroupCluster(jmsOperations.getSubsystemAddress(), "dg-group1", JGROUPS_CLUSTER), true);
        final ModelNode legacyDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/discovery-group=dg-group1").toModelNode();
        final ModelNode jgroupDgAddress = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/jgroups-discovery-group=dg-group1").toModelNode();
        ModelNode discoveryLegacy = executeOperation(Operations.createReadResourceOperation(legacyDgAddress));
        discoveryLegacy.remove("socket-binding");
        ModelNode discovery = executeOperation(Operations.createReadResourceOperation(jgroupDgAddress));
        Assert.assertEquals(discovery.toString(), discoveryLegacy.toString());
        checkNoResource("/subsystem=messaging-activemq/socket-discovery-group=dg-group1");

        executeOperation(Operations.createWriteAttributeOperation(legacyDgAddress, "initial-wait-timeout", 50000));
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "initial-wait-timeout")).asLong());
        Assert.assertEquals(50000L, executeOperation(Operations.createReadAttributeOperation(jgroupDgAddress, "initial-wait-timeout")).asLong());
        executeOperation(Operations.createUndefineAttributeOperation(jgroupDgAddress, "initial-wait-timeout"));
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(legacyDgAddress, "initial-wait-timeout")).asLong());
        Assert.assertEquals(10000L, executeOperation(Operations.createReadAttributeOperation(jgroupDgAddress, "initial-wait-timeout")).asLong());

        executeOperation(Operations.createRemoveOperation(legacyDgAddress));
        checkNoResource(legacyDgAddress);
        checkNoResource(jgroupDgAddress);

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());
    }

    ModelNode createShallowDiscoveryGroupWithSocketBinding(ModelNode serverAddress, String discoveryGroupName, String socketBinding) throws Exception {
        ModelNode address = serverAddress.clone();
        address.add("discovery-group", discoveryGroupName);
        ModelNode op = Operations.createAddOperation(address);
        op.get("socket-binding").set(socketBinding);
        return op;
    }

    ModelNode createShallowBroadcastGroupWithSocketBinding(ModelNode serverAddress, String broadcastGroupName, String socketBinding, String connector) throws Exception {
        ModelNode address = serverAddress.clone();
        address.add("broadcast-group", broadcastGroupName);
        ModelNode op = Operations.createAddOperation(address);
        op.get("socket-binding").set(socketBinding);
        op.get("connectors").add(connector);
        return op;
    }

    ModelNode createShallowDiscoveryGroupWithJGroupCluster(ModelNode serverAddress, String discoveryGroupName, String jgroupCluster) throws Exception {
        ModelNode address = serverAddress.clone();
        address.add("discovery-group", discoveryGroupName);
        ModelNode op = Operations.createAddOperation(address);
        op.get("jgroups-cluster").set(jgroupCluster);
        return op;
    }

    ModelNode createShallowBroadcastGroupWithhJGroupCluster(ModelNode serverAddress, String broadcastGroupName, String jgroupCluster, String connector) throws Exception {
        ModelNode address = serverAddress.clone();
        address.add("broadcast-group", broadcastGroupName);
        ModelNode op = Operations.createAddOperation(address);
        op.get("jgroups-cluster").set(jgroupCluster);
        op.get("connectors").add(connector);
        return op;
    }

    ModelNode createDiscoveryGroupWithSocketBinding(ModelNode serverAddress, String discoveryGroupName, String socketBinding) throws Exception {
        ModelNode address = serverAddress.clone();
        address.add("socket-discovery-group", discoveryGroupName);
        ModelNode op = Operations.createAddOperation(address);
        op.get("socket-binding").set(socketBinding);
        return op;
    }

    ModelNode createBroadcastGroupWithSocketBinding(ModelNode serverAddress, String broadcastGroupName, String socketBinding, String connector) throws Exception {
        ModelNode address = serverAddress.clone();
        address.add("socket-broadcast-group", broadcastGroupName);
        ModelNode op = Operations.createAddOperation(address);
        op.get("socket-binding").set(socketBinding);
        op.get("connectors").add(connector);
        return op;
    }

    ModelNode createDiscoveryGroupWithJGroupsCluster(ModelNode serverAddress, String discoveryGroupName, String jgroupCluster) throws Exception {
        ModelNode address = serverAddress.clone();
        address.add("jgroups-discovery-group", discoveryGroupName);
        ModelNode op = Operations.createAddOperation(address);
        op.get("jgroups-cluster").set(jgroupCluster);
        return op;
    }

    ModelNode createBroadcastGroupWithJGroupsCluster(ModelNode serverAddress, String broadcastGroupName, String jgroupCluster, String connector) throws Exception {
        ModelNode address = serverAddress.clone();
        address.add("jgroups-broadcast-group", broadcastGroupName);
        ModelNode op = Operations.createAddOperation(address);
        op.get("jgroups-cluster").set(jgroupCluster);
        op.get("connectors").add(connector);
        return op;
    }

    void checkNoResource(String address) throws IOException, MgmtOperationException {
        checkNoResource(PathAddress.parseCLIStyleAddress(address).toModelNode());
    }

    void checkNoResource(ModelNode address) throws IOException, MgmtOperationException {
        ModelNode result = executeOperation(Operations.createReadResourceOperation(address), false);
        Assert.assertEquals(ModelDescriptionConstants.FAILED, result.require(OUTCOME).asString());
        Assert.assertTrue(result.require(FAILURE_DESCRIPTION).asString().contains("WFLYCTL0216"));
    }
}
