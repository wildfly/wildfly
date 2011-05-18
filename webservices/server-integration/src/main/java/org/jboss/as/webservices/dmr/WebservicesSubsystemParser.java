package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

final class WebservicesSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    private static final WebservicesSubsystemParser INSTANCE = new WebservicesSubsystemParser();

    static WebservicesSubsystemParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();
        writeElement(writer, Element.WSDL_HOST, node.require(WSDL_HOST));
        writeElement(writer, Element.MODIFY_WSDL_ADDRESS, node.require(MODIFY_WSDL_ADDRESS));
        if (has(node, WSDL_SECURE_PORT)) {
            writeElement(writer, Element.WSDL_SECURE_PORT, node.require(WSDL_SECURE_PORT));
        }
        if (has(node, WSDL_PORT)) {
            writeElement(writer, Element.WSDL_PORT, node.require(WSDL_PORT));
        }

        writer.writeEndElement(); // End of subsystem element
    }

    private boolean has(ModelNode node, String name) {
        return node.has(name) && node.get(name).isDefined();
    }

    private void writeElement(final XMLExtendedStreamWriter writer, final Element element, final ModelNode value)
            throws XMLStreamException {
        writer.writeStartElement(element.getLocalName());
        writer.writeCharacters(value.asString());
        writer.writeEndElement();
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME);

        // elements
        final EnumSet<Element> required = EnumSet.of(Element.MODIFY_WSDL_ADDRESS, Element.WSDL_HOST);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEBSERVICES_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    required.remove(element);
                    if (!encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                    case WSDL_HOST: {
                        subsystem.get(WSDL_HOST).set(parseElementNoAttributes(reader));
                        break;
                    }
                    case MODIFY_WSDL_ADDRESS: {
                        boolean b = Boolean.parseBoolean(parseElementNoAttributes(reader));
                        subsystem.get(MODIFY_WSDL_ADDRESS).set(b);
                        break;
                    }
                    case WSDL_SECURE_PORT: {
                        int port = Integer.valueOf(parseElementNoAttributes(reader));
                        subsystem.get(WSDL_SECURE_PORT).set(port);
                        break;
                    }
                    case WSDL_PORT: {
                        int port = Integer.valueOf(parseElementNoAttributes(reader));
                        subsystem.get(WSDL_PORT).set(port);
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

        if (!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }

        list.add(subsystem);
    }

    private String parseElementNoAttributes(XMLExtendedStreamReader reader) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        return reader.getElementText().trim();
    }
}