package org.wildfly.clustering.monitor.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The clustering-monitor subsystem XML configuration:
 *
 *   <subsystem xmlns="urn:jboss:domain:clustering-monitor:1.0">
 *   </subsystem
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
                case CLUSTERING_MONITOR_1_0: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }
}