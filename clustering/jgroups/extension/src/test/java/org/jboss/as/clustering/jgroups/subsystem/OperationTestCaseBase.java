/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;

/**
* Base test case for testing management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
public class OperationTestCaseBase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = String.format("jgroups-%d.%d.xml", JGroupsSubsystemSchema.CURRENT.getVersion().major(), JGroupsSubsystemSchema.CURRENT.getVersion().minor());

    public OperationTestCaseBase() {
        super(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new JGroupsExtension());
    }

    protected static ModelNode getSubsystemAddOperation(String defaultStack) {
        return Util.createAddOperation(getSubsystemAddress());
    }

    protected static ModelNode getSubsystemReadOperation(AttributeDefinition attribute) {
        return Util.getReadAttributeOperation(getSubsystemAddress(), attribute.getName());
    }

    protected static ModelNode getSubsystemWriteOperation(AttributeDefinition attribute, String value) {
        return Util.getWriteAttributeOperation(getSubsystemAddress(), attribute.getName(), new ModelNode(value));
    }

    protected static ModelNode getSubsystemRemoveOperation() {
        return Util.createRemoveOperation(getSubsystemAddress());
    }

    protected static ModelNode getProtocolStackAddOperation(String stackName) {
        return Util.createAddOperation(getProtocolStackAddress(stackName));
    }

    protected static ModelNode getProtocolStackAddOperationWithParameters(String stackName) {
        return Util.createCompositeOperation(List.of(
                getProtocolStackAddOperation(stackName),
                getTransportAddOperation(stackName, "UDP"),
                getProtocolAddOperation(stackName, "PING"),
                getProtocolAddOperation(stackName, "pbcast.FLUSH")
        ));
    }

    protected static ModelNode getProtocolStackRemoveOperation(String stackName) {
        return Util.createRemoveOperation(getProtocolStackAddress(stackName));
    }

    protected static ModelNode getTransportAddOperation(String stackName, String protocol) {
        ModelNode operation = Util.createAddOperation(getTransportAddress(stackName, protocol));
        operation.get(AbstractTransportResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.getName()).set("some-binding");
        return operation;
    }

    protected static ModelNode getTransportRemoveOperation(String stackName, String type) {
        return Util.createRemoveOperation(getTransportAddress(stackName, type));
    }

    protected static ModelNode getTransportReadOperation(String stackName, String type, AttributeDefinition attribute) {
        return Util.getReadAttributeOperation(getTransportAddress(stackName, type), attribute.getName());
    }

    protected static ModelNode getTransportWriteOperation(String stackName, String type, AttributeDefinition attribute, String value) {
        return Util.getWriteAttributeOperation(getTransportAddress(stackName, type), attribute.getName(), new ModelNode(value));
    }

    // Transport property map operations
    protected static ModelNode getTransportGetPropertyOperation(String stackName, String type, String propertyName) {
        return Util.createMapGetOperation(getTransportAddress(stackName, type), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName);
    }

    protected static ModelNode getTransportPutPropertyOperation(String stackName, String type, String propertyName, String propertyValue) {
        return Util.createMapPutOperation(getTransportAddress(stackName, type), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName, propertyValue);
    }

    protected static ModelNode getTransportRemovePropertyOperation(String stackName, String type, String propertyName) {
        return Util.createMapRemoveOperation(getTransportAddress(stackName, type), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName);
    }

    protected static ModelNode getTransportClearPropertiesOperation(String stackName, String type) {
        return Util.createMapClearOperation(getTransportAddress(stackName, type), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName());
    }

    protected static ModelNode getTransportUndefinePropertiesOperation(String stackName, String type) {
        return Util.getUndefineAttributeOperation(getTransportAddress(stackName, type), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName());
    }

    /**
     * Creates operations such as /subsystem=jgroups/stack=tcp/transport=TCP/:write-attribute(name=properties,value={a=b,c=d})".
     *
     * @return resulting :write-attribute operation
     */
    protected static ModelNode getTransportSetPropertiesOperation(String stackName, String type, ModelNode values) {
        return Util.getWriteAttributeOperation(getTransportAddress(stackName, type), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), values);
    }

    protected static ModelNode getThreadPoolAddOperation(String stackName, String type, String threadPoolName) {
        return Util.createAddOperation(getTransportAddress(stackName, type).append("thread-pool", threadPoolName));
    }

    // Protocol operations
    protected static ModelNode getProtocolAddOperation(String stackName, String type) {
        return Util.createAddOperation(getProtocolAddress(stackName, type));
    }

    protected static ModelNode getProtocolReadOperation(String stackName, String protocolName, AttributeDefinition attribute) {
        return Util.getReadAttributeOperation(getProtocolAddress(stackName, protocolName), attribute.getName());
    }

    protected static ModelNode getProtocolWriteOperation(String stackName, String protocolName, AttributeDefinition attribute, String value) {
        return Util.getWriteAttributeOperation(getProtocolAddress(stackName, protocolName), attribute.getName(), new ModelNode(value));
    }

    protected static ModelNode getProtocolGetPropertyOperation(String stackName, String protocolName, String propertyName) {
        return Util.createMapGetOperation(getProtocolAddress(stackName, protocolName), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName);
    }

    protected static ModelNode getProtocolPutPropertyOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        return Util.createMapPutOperation(getProtocolAddress(stackName, protocolName), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName, propertyValue);
    }

    protected static ModelNode getProtocolRemovePropertyOperation(String stackName, String protocolName, String propertyName) {
        return Util.createMapRemoveOperation(getProtocolAddress(stackName, protocolName), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName);
    }

    protected static ModelNode getProtocolClearPropertiesOperation(String stackName, String protocolName) {
        return Util.createMapClearOperation(getProtocolAddress(stackName, protocolName), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName());
    }

    protected static ModelNode getProtocolUndefinePropertiesOperation(String stackName, String protocolName) {
        return Util.getUndefineAttributeOperation(getProtocolAddress(stackName, protocolName), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName());
    }

    /**
     * Creates operations such as /subsystem=jgroups/stack=tcp/protocol=MPING/:write-attribute(name=properties,value={a=b,c=d})".
     */
    protected static ModelNode getProtocolSetPropertiesOperation(String stackName, String protocolName, ModelNode values) {
        return Util.getWriteAttributeOperation(getProtocolAddress(stackName, protocolName), ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), values);
    }

    protected static ModelNode getProtocolRemoveOperation(String stackName, String type) {
        return Util.createRemoveOperation(getProtocolAddress(stackName, type));
    }

    protected static PathAddress getSubsystemAddress() {
        return PathAddress.pathAddress(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement());
    }

    protected static PathAddress getProtocolStackAddress(String stackName) {
        return getSubsystemAddress().append(JGroupsResourceRegistration.STACK.pathElement(stackName));
    }

    protected static PathAddress getTransportAddress(String stackName, String type) {
        return getProtocolStackAddress(stackName).append(StackResourceDefinitionRegistrar.Component.TRANSPORT.pathElement(type));
    }

    protected static PathAddress getProtocolAddress(String stackName, String type) {
        return getProtocolStackAddress(stackName).append(StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(type));
    }

    protected String getSubsystemXml() throws IOException {
        return readResource(SUBSYSTEM_XML_FILE) ;
    }

    protected KernelServices buildKernelServices() throws Exception {
        return this.createKernelServicesBuilder(AdditionalInitialization.withCapabilities(JGroupsSubsystemSchema.CURRENT,
                RuntimeCapability.resolveCapabilityName(SocketBinding.SERVICE_DESCRIPTOR, "some-binding"),
                RuntimeCapability.resolveCapabilityName(SocketBinding.SERVICE_DESCRIPTOR, "jgroups-diagnostics"),
                RuntimeCapability.resolveCapabilityName(SocketBinding.SERVICE_DESCRIPTOR, "jgroups-mping"),
                RuntimeCapability.resolveCapabilityName(SocketBinding.SERVICE_DESCRIPTOR, "jgroups-tcp-fd"),
                RuntimeCapability.resolveCapabilityName(SocketBinding.SERVICE_DESCRIPTOR, "new-socket-binding")))
            .setSubsystemXml(this.getSubsystemXml()).build();
    }
}