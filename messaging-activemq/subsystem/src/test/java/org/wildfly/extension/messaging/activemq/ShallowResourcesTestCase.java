/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;

/**
 * Verifies that an attempt to undefine socket-binding attr on the socket-discovery-group resource, when performed via
 * the original discovery-group shallow resource, fails with informative error message.
 *
 * The same is tested for the jgroups-cluster attr on the jgroups-discovery-group, and similarly for the *-broadcast-group resources.
 */
public class ShallowResourcesTestCase extends AbstractSubsystemTest {

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
    private static final PathAddress SERVER_ADDRESS = SUBSYSTEM_ADDRESS.append(CommonAttributes.SERVER, CommonAttributes.DEFAULT);

    private static PathAddress discoveryGroupAddress(String name) {
        return SUBSYSTEM_ADDRESS.append(CommonAttributes.DISCOVERY_GROUP, name);
    }

    private static PathAddress broadcastGroupAddress(String name) {
        return SERVER_ADDRESS.append(CommonAttributes.BROADCAST_GROUP, name);
    }

    public ShallowResourcesTestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Test
    public void testDiscoveryGroupRequiresSocketBindingOrJGroupsCluster() throws Exception {
        ModelNode operationTemplate = Util.createAddOperation(discoveryGroupAddress("dg"));
        testRequiresSocketBindingOrJGroupsCluster(operationTemplate);
    }

    @Test
    public void testBroadcastGroupRequiresSocketBindingOrJGroupsCluster() throws Exception {
        ModelNode operationTemplate = Util.createAddOperation(broadcastGroupAddress("bg"));
        operationTemplate.get(CommonAttributes.CONNECTORS).setEmptyList().add("in-vm");
        testRequiresSocketBindingOrJGroupsCluster(operationTemplate);
    }

    private void testRequiresSocketBindingOrJGroupsCluster(ModelNode addOperationTemplate) throws Exception {
        KernelServices kernelServices = createKernelServices();

        // try to add discovery or broadcast group with no socket-binding and no jgroups-cluster attrs -> should fail
        // with a message saying that one of the two attributes must be set
        ModelNode op = addOperationTemplate.clone();
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        Assert.assertTrue(result.get(FAILURE_DESCRIPTION).asString().contains("WFLYMSGAMQ0108:"));

        // try to add discovery or broadcast group with jgroups-cluster set -> should succeed
        op = addOperationTemplate.clone();
        op.get(CommonAttributes.JGROUPS_CLUSTER.getName()).set("default-cluster");
        result = kernelServices.executeOperation(op);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(),
                SUCCESS, result.get(OUTCOME).asString());
        removeResource(kernelServices, addOperationTemplate.get(ADDRESS));

        // try to add discovery or broadcast group with socket-binding set -> should succeed
        op = addOperationTemplate.clone();
        op.get(CommonAttributes.SOCKET_BINDING.getName()).set("http");
        result = kernelServices.executeOperation(op);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(),
                SUCCESS, result.get(OUTCOME).asString());
        removeResource(kernelServices, addOperationTemplate.get(ADDRESS));
    }

    @Test
    public void testUndefineDiscoveryGroupAttrs() throws Exception {
        ModelNode addSocketDiscoveryGroup = Util.createAddOperation(discoveryGroupAddress("socket-group"));
        addSocketDiscoveryGroup.get(CommonAttributes.SOCKET_BINDING.getName()).set("http");

        ModelNode addJGroupsDiscoveryGroup = Util.createAddOperation(discoveryGroupAddress("jgroups-group"));
        addJGroupsDiscoveryGroup.get(CommonAttributes.JGROUPS_CLUSTER.getName()).set("default");

        testUndefineAttrs(addSocketDiscoveryGroup, addJGroupsDiscoveryGroup);
    }

    @Test
    public void testUndefineBroadcastGroupAttrs() throws Exception {
        ModelNode addSocketBroadcastGroup = Util.createAddOperation(broadcastGroupAddress("socket-group"));
        addSocketBroadcastGroup.get(CommonAttributes.CONNECTORS).setEmptyList().add("in-vm");
        addSocketBroadcastGroup.get(CommonAttributes.SOCKET_BINDING.getName()).set("http");

        ModelNode addJGroupsBroadcastGroup = Util.createAddOperation(broadcastGroupAddress("jgroups-group"));
        addJGroupsBroadcastGroup.get(CommonAttributes.CONNECTORS).setEmptyList().add("in-vm");
        addJGroupsBroadcastGroup.get(CommonAttributes.JGROUPS_CLUSTER.getName()).set("default");

        testUndefineAttrs(addSocketBroadcastGroup, addJGroupsBroadcastGroup);
    }

    private void testUndefineAttrs(ModelNode addSocketGroupTemplate, ModelNode addJGroupsGroupTemplate) throws Exception {
        KernelServices kernelServices = createKernelServices();

        // create socket-*-group to work with
        ModelNode result = kernelServices.executeOperation(addSocketGroupTemplate);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // create jgroups-*-group to work with
        result = kernelServices.executeOperation(addJGroupsGroupTemplate);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // try to undefine socket-binding from socket-*-group
        PathAddress socketGroupAddress = PathAddress.pathAddress(addSocketGroupTemplate.get(ADDRESS));
        ModelNode op = Util.getUndefineAttributeOperation(socketGroupAddress, CommonAttributes.SOCKET_BINDING.getName());
        result = kernelServices.executeOperation(op);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        Assert.assertTrue("Failure description doesn't match: " + result.get(FAILURE_DESCRIPTION).asString(),
                result.get(FAILURE_DESCRIPTION).asString().contains("WFLYCTL0231"));
        Assert.assertTrue("Failure description doesn't match: " + result.get(FAILURE_DESCRIPTION).asString(),
                result.get(FAILURE_DESCRIPTION).asString().contains(CommonAttributes.SOCKET_BINDING.getName()));
        Assert.assertFalse("Failure description doesn't match: " + result.get(FAILURE_DESCRIPTION).asString(),
                result.get(FAILURE_DESCRIPTION).asString().contains(CommonAttributes.JGROUPS_CLUSTER.getName()));

        // try to undefine jgroups-cluster from jgroups-*-group
        PathAddress jgroupsGroupAddress = PathAddress.pathAddress(addJGroupsGroupTemplate.get(ADDRESS));
        op = Util.getUndefineAttributeOperation(jgroupsGroupAddress, CommonAttributes.JGROUPS_CLUSTER.getName());
        result = kernelServices.executeOperation(op);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        Assert.assertTrue("Failure description doesn't match: " + result.get(FAILURE_DESCRIPTION).asString(),
                result.get(FAILURE_DESCRIPTION).asString().contains("WFLYCTL0231:"));
        Assert.assertFalse("Failure description doesn't match: " + result.get(FAILURE_DESCRIPTION).asString(),
                result.get(FAILURE_DESCRIPTION).asString().contains(CommonAttributes.SOCKET_BINDING.getName()));
        Assert.assertTrue("Failure description doesn't match: " + result.get(FAILURE_DESCRIPTION).asString(),
                result.get(FAILURE_DESCRIPTION).asString().contains(CommonAttributes.JGROUPS_CLUSTER.getName()));

        // try to undefine jgroups-cluster attribute in socket-*-group, operation should succeed (it should be ignored)
        op = Util.getUndefineAttributeOperation(socketGroupAddress, CommonAttributes.JGROUPS_CLUSTER.getName());
        result = kernelServices.executeOperation(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // try to undefine socket-binding attribute in jgroups-*-group, operation should succeed (it should be ignored)
        op = Util.getUndefineAttributeOperation(jgroupsGroupAddress, CommonAttributes.SOCKET_BINDING.getName());
        result = kernelServices.executeOperation(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    private KernelServices createKernelServices() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(readResource("subsystem_15_0.xml"))
                .build();
        Assert.assertTrue("Subsystem boot failed!", kernelServices.isSuccessfulBoot());
        return kernelServices;
    }

    private static void removeResource(KernelServices kernelServices, ModelNode address) {
        ModelNode op = Util.createRemoveOperation(PathAddress.pathAddress(address));
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    private AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(RuntimeCapability.resolveCapabilityName(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, "ee"),
                ClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY.getName(),
                Capabilities.ELYTRON_DOMAIN_CAPABILITY,
                Capabilities.ELYTRON_DOMAIN_CAPABILITY + ".elytronDomain",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".cs1",
                Capabilities.DATA_SOURCE_CAPABILITY + ".fooDS",
                "org.wildfly.messaging.activemq.connector.external.in-vm",
                "org.wildfly.messaging.activemq.connector.external.client",
                "org.wildfly.remoting.http-listener-registry",
                Capabilities.HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME + ".default");
    }
}
