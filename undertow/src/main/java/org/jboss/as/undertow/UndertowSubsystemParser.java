package org.jboss.as.undertow;

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

import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

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
        UndertowRootDefinition.DEFAULT_SERVER.marshallAsAttribute(model, writer);
        UndertowRootDefinition.DEFAULT_VIRTUAL_HOST.marshallAsAttribute(model, writer);
        UndertowRootDefinition.DEFAULT_SERVLET_CONTAINER.marshallAsAttribute(model, writer);
        UndertowRootDefinition.INSTANCE_ID.marshallAsAttribute(model, writer);
        if (model.hasDefined(Constants.BUFFER_CACHE)) {
            writer.writeStartElement(Constants.BUFFER_CACHES);
            BufferCacheDefinition.INSTANCE.persist(writer, model);
            writer.writeEndElement();
        }
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

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            switch (reader.getAttributeLocalName(i)) {
                case Constants.DEFAULT_SERVER:
                    UndertowRootDefinition.DEFAULT_SERVER.parseAndSetParameter(value, subsystem, reader);
                    break;
                case Constants.DEFAULT_SERVLET_CONTAINER:
                    UndertowRootDefinition.DEFAULT_SERVLET_CONTAINER.parseAndSetParameter(value, subsystem, reader);
                    break;
                case Constants.DEFAULT_VIRTUAL_HOST:
                    UndertowRootDefinition.DEFAULT_VIRTUAL_HOST.parseAndSetParameter(value, subsystem, reader);
                    break;
                case Constants.INSTANCE_ID:
                    UndertowRootDefinition.INSTANCE_ID.parseAndSetParameter(value, subsystem, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (namespace) {
                case UNDERTOW_1_0: {
                    switch (reader.getLocalName()) {
                        case Constants.SERVER: {
                            ServerDefinition.INSTANCE.parse(reader, address, list);
                            break;
                        }
                        case Constants.SERVLET_CONTAINER: {
                            ServletContainerDefinition.INSTANCE.parse(reader, address, list);
                            break;
                        }
                        case Constants.BUFFER_CACHES:
                            parseBufferCaches(reader, address, list);
                            break;
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

    private void parseBufferCaches(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (reader.getLocalName()) {
                case Constants.BUFFER_CACHE: {
                    BufferCacheDefinition.INSTANCE.parse(reader, address, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
            break;
        }
        ParseUtils.requireNoContent(reader);
    }
}

