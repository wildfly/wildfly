package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
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

    static final String SUBSYSTEM_XML_FILE = JGroupsSchema.CURRENT.format("subsystem-jgroups-%d_%d.xml");

    public OperationTestCaseBase() {
        super(JGroupsExtension.SUBSYSTEM_NAME, new JGroupsExtension());
    }

    protected static ModelNode getSubsystemAddOperation(String defaultStack) {
        ModelNode operation = Util.createAddOperation(getSubsystemAddress());
        operation.get(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName()).set(defaultStack);
        return operation;
    }

    protected static ModelNode getSubsystemReadOperation(String name) {
        return Operations.createReadAttributeOperation(getSubsystemAddress(), name);
    }

    protected static ModelNode getSubsystemWriteOperation(String name, String value) {
        return Operations.createWriteAttributeOperation(getSubsystemAddress(), name, new ModelNode(value));
    }

    protected static ModelNode getSubsystemRemoveOperation() {
        return Util.createRemoveOperation(getSubsystemAddress());
    }

    protected static ModelNode getProtocolStackAddOperation(String stackName) {
        return Util.createAddOperation(getProtocolStackAddress(stackName));
    }

    protected static ModelNode getProtocolStackAddOperationWithParameters(String stackName) {
        ModelNode[] operations = new ModelNode[] {
                getProtocolStackAddOperation(stackName),
                getTransportAddOperation(stackName, "UDP"),
                getProtocolAddOperation(stackName, "MPING"),
                getProtocolAddOperation(stackName, "pbcast.FLUSH"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getProtocolStackRemoveOperation(String stackName) {
        return Util.createRemoveOperation(getProtocolStackAddress(stackName));
    }

    protected static ModelNode getTransportAddOperation(String stackName, String protocol) {
        return Util.createAddOperation(getTransportAddress(stackName, protocol));
    }

    protected static ModelNode getTransportAddOperationWithProperties(String stackName, String type) {
        ModelNode[] operations = new ModelNode[] {
                getTransportAddOperation(stackName, type),
                getProtocolPropertyAddOperation(stackName, type, "A", "a"),
                getProtocolPropertyAddOperation(stackName, type, "B", "b"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getTransportRemoveOperation(String stackName, String type) {
        return Util.createRemoveOperation(getTransportAddress(stackName, type));
    }

    protected static ModelNode getTransportReadOperation(String stackName, String type, String name) {
        return Operations.createReadAttributeOperation(getTransportAddress(stackName, type), name);
    }

    protected static ModelNode getTransportWriteOperation(String stackName, String type, String name, String value) {
        return Operations.createWriteAttributeOperation(getTransportAddress(stackName, type), name, new ModelNode(value));
    }

    protected static ModelNode getTransportPropertyAddOperation(String stackName, String type, String propertyName, String propertyValue) {
        ModelNode operation = Util.createAddOperation(getTransportPropertyAddress(stackName, type, propertyName));
        operation.get(PropertyResourceDefinition.VALUE.getName()).set(propertyValue);
        return operation;
    }

    protected static ModelNode getTransportPropertyReadOperation(String stackName, String type, String propertyName) {
        return Operations.createReadAttributeOperation(getTransportPropertyAddress(stackName, type, propertyName), PropertyResourceDefinition.VALUE.getName());
    }

    protected static ModelNode getTransportPropertyWriteOperation(String stackName, String type, String propertyName, String propertyValue) {
        return Operations.createWriteAttributeOperation(getTransportPropertyAddress(stackName, type, propertyName), PropertyResourceDefinition.VALUE.getName(), new ModelNode(propertyValue));
    }

    protected static ModelNode getProtocolAddOperation(String stackName, String type) {
        return Util.createAddOperation(getProtocolAddress(stackName, type));
    }

    protected static ModelNode getProtocolAddOperationWithProperties(String stackName, String type) {
        ModelNode[] operations = new ModelNode[] {
                getProtocolAddOperation(stackName, type),
                getProtocolPropertyAddOperation(stackName, type, "A", "a"),
                getProtocolPropertyAddOperation(stackName, type, "B", "b"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getProtocolReadOperation(String stackName, String protocolName, String name) {
        return Operations.createReadAttributeOperation(getProtocolAddress(stackName, protocolName), name);
    }

    protected static ModelNode getProtocolWriteOperation(String stackName, String protocolName, String name, String value) {
        return Operations.createWriteAttributeOperation(getProtocolAddress(stackName, protocolName), name, new ModelNode(value));
    }

    protected static ModelNode getProtocolPropertyAddOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        ModelNode operation = Util.createAddOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName));
        operation.get(PropertyResourceDefinition.VALUE.getName()).set(propertyValue);
        return operation;
    }

    protected static ModelNode getProtocolPropertyReadOperation(String stackName, String protocolName, String propertyName) {
        return Operations.createReadAttributeOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName), PropertyResourceDefinition.VALUE.getName());
    }

    protected static ModelNode getProtocolPropertyWriteOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        return Operations.createWriteAttributeOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName), PropertyResourceDefinition.VALUE.getName(), new ModelNode(propertyValue));
    }

    protected static ModelNode getProtocolRemoveOperation(String stackName, String type) {
        return Util.createRemoveOperation(getProtocolAddress(stackName, type));
    }

    protected static PathAddress getSubsystemAddress() {
        return PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
    }

    protected static PathAddress getProtocolStackAddress(String stackName) {
        return getSubsystemAddress().append(StackResourceDefinition.pathElement(stackName));
    }

    protected static PathAddress getTransportAddress(String stackName, String type) {
        return getProtocolStackAddress(stackName).append(TransportResourceDefinition.pathElement(type));
    }

    protected static PathAddress getTransportPropertyAddress(String stackName, String type, String propertyName) {
        return getTransportAddress(stackName, type).append(PropertyResourceDefinition.pathElement(propertyName));
    }

    protected static PathAddress getProtocolAddress(String stackName, String type) {
        return getProtocolStackAddress(stackName).append(ProtocolResourceDefinition.pathElement(type));
    }

    protected static PathAddress getProtocolPropertyAddress(String stackName, String type, String propertyName) {
        return getProtocolAddress(stackName, type).append(PropertyResourceDefinition.pathElement(propertyName));
    }

    protected String getSubsystemXml() throws IOException {
        return readResource(SUBSYSTEM_XML_FILE) ;
    }

    protected KernelServices buildKernelServices() throws XMLStreamException, IOException, Exception {
        return createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(this.getSubsystemXml()).build();
    }
}