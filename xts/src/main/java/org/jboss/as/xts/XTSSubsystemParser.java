package org.jboss.as.xts;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.xts.XTSSubsystemDefinition.ENVIRONMENT_URL;

/**
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class XTSSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }

        final ModelNode subsystem = Util.getEmptyOperation(ADD, PathAddress.pathAddress(XTSExtension.SUBSYSTEM_PATH).toModelNode());
        list.add(subsystem);


        // elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case XTS_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    switch (element) {
                        case XTS_ENVIRONMENT: {
                            parseXTSEnvironmentElement(reader,subsystem);
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

    /**
     * Handle the xts-environment element
     *
     *
     * @param reader
     * @param subsystem
     * @return ModelNode for the core-environment
     * @throws javax.xml.stream.XMLStreamException
     *
     */
    static void parseXTSEnvironmentElement(XMLExtendedStreamReader reader, ModelNode subsystem) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case URL:
                    ENVIRONMENT_URL.parseAndSetParameter(value, subsystem, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();


        writer.writeStartElement(Element.XTS_ENVIRONMENT.getLocalName());
        ENVIRONMENT_URL.marshallAsAttribute(node, writer);
        writer.writeEndElement();

        writer.writeEndElement();
    }

    private boolean has(ModelNode node, String name) {
        return node.has(name) && node.get(name).isDefined();
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }
}
