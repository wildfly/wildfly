package org.wildfly.clustering.monitor.extension;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteringMonitorSubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(ClusteringMonitorExtension.NAMESPACE, false);
        ModelNode model = context.getModelNode();

        if (model.isDefined()) {
            // write model content here
        }
        writer.writeEndElement();
    }
}
