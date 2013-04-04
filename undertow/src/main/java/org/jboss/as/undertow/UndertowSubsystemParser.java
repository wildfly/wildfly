package org.jboss.as.undertow;

import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    protected static final UndertowSubsystemParser INSTANCE = new UndertowSubsystemParser();

    private UndertowSubsystemParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode model = context.getModelNode();
        WorkerResourceDefinition.INSTANCE.persist(writer, model);
        BufferPoolResourceDefinition.INSTANCE.persist(writer, model);
        ServerDefinition.INSTANCE.persist(writer, model);
        ServletContainerDefinition.INSTANCE.persist(writer, model);
        writer.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = PathAddress.pathAddress(UndertowExtension.SUBSYSTEM_PATH);
        final ModelNode subsystem = Util.createAddOperation(address);
        list.add(subsystem);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (namespace) {
                case UNDERTOW_1_0: {
                    switch (reader.getLocalName()) {
                        case Constants.WORKER: {
                            WorkerResourceDefinition.INSTANCE.parse(reader, address, list);
                            break;
                        }
                        case Constants.BUFFER_POOL: {
                            BufferPoolResourceDefinition.INSTANCE.parse(reader, address, list);
                            break;
                        }
                        case Constants.SERVER: {
                            ServerDefinition.INSTANCE.parse(reader, address, list);
                            break;
                        }
                        case Constants.SERVLET_CONTAINER: {
                            ServletContainerDefinition.INSTANCE.parse(reader, address, list);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        ParseUtils.requireNoContent(reader);

    }
}

