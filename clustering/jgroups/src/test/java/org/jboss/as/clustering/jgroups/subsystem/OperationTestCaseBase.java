package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.as.clustering.jgroups.JGroupsMessages;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.dmr.ModelNode;

/**
* Base test case for testing management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
public class OperationTestCaseBase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = "subsystem-jgroups-test.xml" ;

    public OperationTestCaseBase() {
        super(JGroupsExtension.SUBSYSTEM_NAME, new JGroupsExtension());
    }

    protected static ModelNode getCompositeOperation(ModelNode[] operations) {
        // create the address of the cache
        ModelNode compositeOp = new ModelNode() ;
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();
        // the operations to be performed
        for (ModelNode operation : operations) {
            compositeOp.get(STEPS).add(operation);
        }
        return compositeOp ;
    }

    protected static ModelNode getSubsystemAddOperation(String defaultStack) {
        // create the address of the subsystem
        PathAddress subsystemAddress =  PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME));
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(subsystemAddress.toModelNode());
        // required attributes
        addOp.get(ModelKeys.DEFAULT_STACK).set(defaultStack);
        return addOp ;
    }

    protected static ModelNode getSubsystemReadOperation(String name) {
        // create the address of the subsystem
        PathAddress subsystemAddress =  PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME));
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(subsystemAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(name);
        return readOp ;
    }

    protected static ModelNode getSubsystemWriteOperation(String name, String value) {
        // create the address of the subsystem
        PathAddress subsystemAddress =  PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME));
        ModelNode writeOp = new ModelNode() ;
        writeOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        writeOp.get(OP_ADDR).set(subsystemAddress.toModelNode());
        // required attributes
        writeOp.get(NAME).set(name);
        writeOp.get(VALUE).set(value);
        return writeOp ;
    }

    protected static ModelNode getSubsystemRemoveOperation() {
        // create the address of the subsystem
        PathAddress subsystemAddress =  PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME));
        ModelNode removeOp = new ModelNode() ;
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(subsystemAddress.toModelNode());
        return removeOp ;
    }

    protected static ModelNode getProtocolStackAddOperation(String stackName) {
        // create the address of the cache
        PathAddress stackAddr = getProtocolStackAddress(stackName);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(stackAddr.toModelNode());
        // required attributes
        // addOp.get(DEFAULT_CACHE).set("default");
        return addOp ;
    }

    protected static ModelNode getProtocolStackAddOperationWithParameters(String stackName) {
        ModelNode addOp = getProtocolStackAddOperation(stackName);
        // add optional TRANSPORT attribute
        ModelNode transport = addOp.get(ModelKeys.TRANSPORT);
        transport.get(ModelKeys.TYPE).set("UDP");

        // add optional PROTOCOLS attribute
        ModelNode protocolsList = addOp.get(ModelKeys.PROTOCOLS);
        protocolsList.add("MPING");
        protocolsList.add("pbcast.FLUSH");
        return addOp ;
    }

    protected static ModelNode getProtocolStackRemoveOperation(String stackName) {
        // create the address of the cache
        PathAddress stackAddr = getProtocolStackAddress(stackName);
        ModelNode removeOp = new ModelNode() ;
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(stackAddr.toModelNode());
        return removeOp ;
    }

    protected static ModelNode getTransportAddOperation(String stackName, String protocolType) {
        // create the address of the cache
        PathAddress transportAddr = getTransportAddress(stackName);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(transportAddr.toModelNode());
        // required attributes
        addOp.get(ModelKeys.TYPE).set(protocolType);
        return addOp ;
    }

    protected static ModelNode getTransportAddOperationWithProperties(String stackName, String protocolType) {
        ModelNode addOp = getTransportAddOperation(stackName, protocolType);
        // add optional PROPERTIES attribute
        ModelNode propertyList = new ModelNode();
        ModelNode propA = new ModelNode();
        propA.add("A","a");
        propertyList.add(propA);
        ModelNode propB = new ModelNode();
        propB.add("B","b");
        propertyList.add(propB);
        addOp.get(ModelKeys.PROPERTIES).set(propertyList);
        return addOp ;
    }

    protected static ModelNode getTransportRemoveOperation(String stackName, String protocolType) {
        // create the address of the cache
        PathAddress transportAddr = getTransportAddress(stackName);
        ModelNode removeOp = new ModelNode() ;
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(transportAddr.toModelNode());
        return removeOp ;
    }

    protected static ModelNode getTransportReadOperation(String stackName, String name) {
        // create the address of the subsystem
        PathAddress transportAddress = getTransportAddress(stackName);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(transportAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(name);
        return readOp ;
    }

    protected static ModelNode getTransportWriteOperation(String stackName, String name, String value) {
        // create the address of the subsystem
        PathAddress transportAddress = getTransportAddress(stackName);
        ModelNode writeOp = new ModelNode() ;
        writeOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        writeOp.get(OP_ADDR).set(transportAddress.toModelNode());
        // required attributes
        writeOp.get(NAME).set(name);
        writeOp.get(VALUE).set(value);
        return writeOp ;
    }

    protected static ModelNode getTransportPropertyAddOperation(String stackName, String propertyName, String propertyValue) {
        // create the address of the subsystem
        PathAddress transportPropertyAddress = getTransportPropertyAddress(stackName, propertyName);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(transportPropertyAddress.toModelNode());
        // required attributes
        addOp.get(NAME).set(ModelKeys.VALUE);
        addOp.get(VALUE).set(propertyValue);
        return addOp ;
    }

    protected static ModelNode getTransportPropertyReadOperation(String stackName, String propertyName) {
        // create the address of the subsystem
        PathAddress transportPropertyAddress = getTransportPropertyAddress(stackName, propertyName);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(transportPropertyAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(ModelKeys.VALUE);
        return readOp ;
    }

    protected static ModelNode getTransportPropertyWriteOperation(String stackName, String propertyName, String propertyValue) {
        // create the address of the subsystem
        PathAddress transportPropertyAddress = getTransportPropertyAddress(stackName, propertyName);
        ModelNode writeOp = new ModelNode() ;
        writeOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        writeOp.get(OP_ADDR).set(transportPropertyAddress.toModelNode());
        // required attributes
        writeOp.get(NAME).set(ModelKeys.VALUE);
        writeOp.get(VALUE).set(propertyValue);
        return writeOp ;
    }

    protected static ModelNode getProtocolAddOperation(String stackName, String protocolType) {
        // create the address of the cache
        PathAddress stackAddr = getProtocolStackAddress(stackName);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set("add-protocol");
        addOp.get(OP_ADDR).set(stackAddr.toModelNode());
        // required attributes
        addOp.get(ModelKeys.TYPE).set(protocolType);
        return addOp ;
    }

    protected static ModelNode getProtocolAddOperationWithProperties(String stackName, String protocolType) {
        ModelNode addOp = getProtocolAddOperation(stackName, protocolType);
        // add optional PROPERTIES attribute
        ModelNode propertyList = new ModelNode();
        ModelNode propA = new ModelNode();
        propA.add("A","a");
        propertyList.add(propA);
        ModelNode propB = new ModelNode();
        propB.add("B","b");
        propertyList.add(propB);
        addOp.get(ModelKeys.PROPERTIES).set(propertyList);
        return addOp ;
    }

    protected static ModelNode getProtocolReadOperation(String stackName, String protocolName, String name) {
        // create the address of the subsystem
        PathAddress protocolAddress = getProtocolAddress(stackName, protocolName);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(protocolAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(name);
        return readOp ;
    }

    protected static ModelNode getProtocolWriteOperation(String stackName, String protocolName, String name, String value) {
        // create the address of the subsystem
        PathAddress protocolAddress = getProtocolAddress(stackName, protocolName);
        ModelNode writeOp = new ModelNode() ;
        writeOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        writeOp.get(OP_ADDR).set(protocolAddress.toModelNode());
        // required attributes
        writeOp.get(NAME).set(name);
        writeOp.get(VALUE).set(value);
        return writeOp ;
    }

    protected static ModelNode getProtocolPropertyAddOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        // create the address of the subsystem
        PathAddress protocolPropertyAddress = getProtocolPropertyAddress(stackName, protocolName, propertyName);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(protocolPropertyAddress.toModelNode());
        // required attributes
        addOp.get(NAME).set(ModelKeys.VALUE);
        addOp.get(VALUE).set(propertyValue);
        return addOp ;
    }

    protected static ModelNode getProtocolPropertyReadOperation(String stackName, String protocolName, String propertyName) {
        // create the address of the subsystem
        PathAddress protocolPropertyAddress = getProtocolPropertyAddress(stackName, protocolName, propertyName);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(protocolPropertyAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(ModelKeys.VALUE);
        return readOp ;
    }

    protected static ModelNode getProtocolPropertyWriteOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        // create the address of the subsystem
        PathAddress protocolPropertyAddress = getProtocolPropertyAddress(stackName, protocolName, propertyName);
        ModelNode writeOp = new ModelNode() ;
        writeOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        writeOp.get(OP_ADDR).set(protocolPropertyAddress.toModelNode());
        // required attributes
        writeOp.get(NAME).set(ModelKeys.VALUE);
        writeOp.get(VALUE).set(propertyValue);
        return writeOp ;
    }

    protected static ModelNode getProtocolRemoveOperation(String stackName, String protocolType) {
        // create the address of the cache
        PathAddress stackAddr = getProtocolStackAddress(stackName);
        ModelNode removeOp = new ModelNode() ;
        removeOp.get(OP).set("remove-protocol");
        removeOp.get(OP_ADDR).set(stackAddr.toModelNode());
        // required attributes
        removeOp.get(ModelKeys.TYPE).set(protocolType);
        return removeOp ;
    }

    protected static PathAddress getProtocolStackAddress(String stackName) {
        // create the address of the stack
        PathAddress stackAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("stack",stackName));
        return stackAddr ;
    }

    protected static PathAddress getTransportAddress(String stackName) {
        // create the address of the cache
        PathAddress protocolAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("stack",stackName),
                PathElement.pathElement("transport", "TRANSPORT"));
        return protocolAddr ;
    }

    protected static PathAddress getTransportPropertyAddress(String stackName, String propertyName) {
        // create the address of the cache
        PathAddress protocolAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("stack",stackName),
                PathElement.pathElement("transport", "TRANSPORT"),
                PathElement.pathElement("property", propertyName));
        return protocolAddr ;
    }

    protected static PathAddress getProtocolAddress(String stackName, String protocolType) {
        // create the address of the cache
        PathAddress protocolAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("stack",stackName),
                PathElement.pathElement("protocol", protocolType));
        return protocolAddr ;
    }

    protected static PathAddress getProtocolPropertyAddress(String stackName, String protocolType, String propertyName) {
        // create the address of the cache
        PathAddress protocolAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("stack",stackName),
                PathElement.pathElement("protocol", protocolType),
                PathElement.pathElement("property", propertyName));
        return protocolAddr ;
    }

    protected String getSubsystemXml() throws IOException {
        return getSubsystemXml(SUBSYSTEM_XML_FILE) ;
    }

    protected String getSubsystemXml(String xml_file) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(xml_file);
        if (url == null) {
            throw new IllegalStateException(JGroupsMessages.MESSAGES.notFound(xml_file));
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())));
            StringWriter writer = new StringWriter();
            try {
                String line = reader.readLine();
                while (line != null) {
                    writer.write(line);
                    line = reader.readLine();
                }
            } finally {
                reader.close();
            }
            return writer.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}