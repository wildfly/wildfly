package org.wildfly.extension.cluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The cluster subsystem XML configuration:
 *
 *   <subsystem xmlns="urn:jboss:domain:cluster:1.0">
 *      <remote-management-client socket-binding="management-native"/>
 *    </subsystem
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusterSubsystemXMLReader_1_0  implements XMLElementReader<List<ModelNode>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode subsystemAddress = new ModelNode();
        subsystemAddress.add(SUBSYSTEM, ClusterExtension.SUBSYSTEM_NAME);
        subsystemAddress.protect();
        ModelNode subsystem = Util.getEmptyOperation(ADD, subsystemAddress);

        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case CLUSTER_1_0: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case MANAGEMENT_CLIENT: {
                            this.parseManagementClient(reader, subsystemAddress, operations);
                            break;
                        }
                        default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseManagementClient(XMLExtendedStreamReader reader, ModelNode subsystemAddress, List<ModelNode> operations) throws XMLStreamException {

        ModelNode managementClientAddress = subsystemAddress.clone();
        managementClientAddress.add(ModelKeys.MANAGEMENT_CLIENT, ModelKeys.MANAGEMENT_CLIENT_NAME);
        managementClientAddress.protect();
        final ModelNode managementClient = Util.getEmptyOperation(ModelDescriptionConstants.ADD, managementClientAddress);

        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SOCKET_BINDING: {
                    ManagementClientResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, managementClient, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        operations.add(managementClient);
        // add operations to create configuration resources
        for (ModelNode additionalOperation : additionalConfigurationOperations) {
            operations.add(additionalOperation);
        }
    }
}