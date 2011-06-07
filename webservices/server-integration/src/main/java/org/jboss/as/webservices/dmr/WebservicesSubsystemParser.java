package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAINS;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAINS;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeType;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class WebservicesSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    private static final WebservicesSubsystemParser INSTANCE = new WebservicesSubsystemParser();

    static WebservicesSubsystemParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        SubsystemMarshallingContext newContext = new SubsystemMarshallingContext(context.getModelNode(), writer);
        newContext.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = newContext.getModelNode();
        writeElement(writer, Element.WSDL_HOST, node.require(WSDL_HOST));
        writeElement(writer, Element.MODIFY_WSDL_ADDRESS, node.require(MODIFY_WSDL_ADDRESS));
        if (has(node, WSDL_SECURE_PORT)) {
            writeElement(writer, Element.WSDL_SECURE_PORT, node.require(WSDL_SECURE_PORT));
        }
        if (has(node, WSDL_PORT)) {
            writeElement(writer, Element.WSDL_PORT, node.require(WSDL_PORT));
        }
        if (has(node, ENDPOINT_CONFIG)) {
            for (String name : node.get(ENDPOINT_CONFIG).keys()) {
                writeEndpointConfig(writer, name, node.get(ENDPOINT_CONFIG, name));
            }
        }

        writer.writeEndElement(); // End of subsystem element
    }

    private boolean has(ModelNode node, String name) {
        return node.has(name) && node.get(name).isDefined();
    }

    private void writeEndpointConfig(final XMLExtendedStreamWriter writer, String name, final ModelNode value)
            throws XMLStreamException {
        writer.writeStartElement(Constants.ENDPOINT_CONFIG);
        writer.writeStartElement(Constants.CONFIG_NAME);
        writer.writeCharacters(name);
        writer.writeEndElement();
        if (value.hasDefined(Constants.PRE_HANDLER_CHAINS)) {
            writer.writeStartElement(Constants.PRE_HANDLER_CHAINS);
            ModelNode handlerChains = value.get(Constants.PRE_HANDLER_CHAINS);
            writeHandlerChains(writer, handlerChains);
            writer.writeEndElement();
        }

        if (value.hasDefined(Constants.POST_HANDLER_CHAINS)) {
            writer.writeStartElement(Constants.POST_HANDLER_CHAINS);
            ModelNode handlerChains = value.get(Constants.POST_HANDLER_CHAINS);
            writeHandlerChains(writer, handlerChains);
            writer.writeEndElement();
        }
        writer.writeEndElement();

    }

    private void writeHandlerChains(final XMLExtendedStreamWriter writer, final ModelNode handlerChains)
            throws XMLStreamException {
        if (handlerChains.getType() == ModelType.LIST) {
            for (ModelNode handlerChain : handlerChains.asList()) {

                writer.writeStartElement(Constants.HANDLER_CHAIN);
                if (handlerChain.hasDefined(Constants.PROTOCOL_BINDING)) {
                    writer.writeStartElement(Constants.PROTOCOL_BINDING);
                    writer.writeCharacters(handlerChain.get(Constants.PROTOCOL_BINDING).asString());
                    writer.writeEndElement();
                }
                if (handlerChain.hasDefined(Constants.HANDLER)) {

                    for (String key : handlerChain.get(Constants.HANDLER).keys()) {
                        writer.writeStartElement(Constants.HANDLER);

                        writer.writeStartElement(Constants.HANDLER_NAME);
                        writer.writeCharacters(key);
                        writer.writeEndElement();

                        writer.writeStartElement(Constants.HANDLER_CLASS);
                        writer.writeCharacters(handlerChain.get(Constants.HANDLER).get(key).asString());
                        writer.writeEndElement();

                        writer.writeEndElement();
                    }

                }
                writer.writeEndElement();
            }
        }
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

        final List<ModelNode> endpointConfigs = new ArrayList<ModelNode>();

        // elements
        final EnumSet<Element> required = EnumSet.of(Element.MODIFY_WSDL_ADDRESS, Element.WSDL_HOST);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEBSERVICES_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    required.remove(element);
                    if (element != Element.ENDPOINT_CONFIG && !encountered.add(element)) {
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
                        case ENDPOINT_CONFIG: {
                            readEndpointConfig(reader, subsystem.get(OP_ADDR), endpointConfigs);
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
        list.addAll(endpointConfigs);
    }

    private String parseElementNoAttributes(XMLExtendedStreamReader reader) throws XMLStreamException {
        // no attributes
        requireNoAttributes(reader);

        return reader.getElementText().trim();
    }

    private void readEndpointConfig(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> operationList) throws XMLStreamException {
        String configName = null;
        ModelNode preHandlers = null;
        ModelNode postHandlers = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case CONFIG_NAME: {

                    configName = parseElementNoAttributes(reader);

                    break;
                }
                case PRE_HANDLER_CHAINS: {
                    preHandlers = parseHandlerChains(reader);
                    break;
                }
                case POST_HANDLER_CHAINS: {
                    postHandlers = parseHandlerChains(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode endpointConfig = new ModelNode();

        if (preHandlers != null) {
            endpointConfig.get(Constants.PRE_HANDLER_CHAINS).set(preHandlers);
        }

        if (postHandlers != null) {
            endpointConfig.get(Constants.POST_HANDLER_CHAINS).set(postHandlers);
        }

        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(ENDPOINT_CONFIG, configName);
        node.get(PRE_HANDLER_CHAINS).set(preHandlers);
        node.get(POST_HANDLER_CHAINS).set(postHandlers);
        //TODO: feature property
        operationList.add(node);
    }

    private ModelNode parseHandlerChains(XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode chainsNode = new ModelNode();
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER_CHAIN && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }

            switch (element) {
                case HANDLER_CHAIN: {
                    ModelNode chainNode = new ModelNode();
                    parseHandlerChain(reader, chainNode);
                    chainsNode.add(chainNode);
                    break;
                }

                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return chainsNode;

    }

    private void parseHandlerChain(XMLExtendedStreamReader reader, ModelNode chainNode) throws XMLStreamException {
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PROTOCOL_BINDING: {
                    String binding = parseElementNoAttributes(reader);
                    chainNode.get(Constants.PROTOCOL_BINDING).set(binding);

                    break;
                }
                case SERVICE_NAME_PATTERN: {
                    // TODO
                    break;
                }

                case PORT_NAME_PATTERN: {
                    // TODO
                    break;
                }
                case HANDLER: {
                    parseHandler(reader, chainNode);
                    break;
                }

                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    private void parseHandler(XMLExtendedStreamReader reader, ModelNode chainNode) throws XMLStreamException {
        String handlerName = null;
        String handlerClass = null;
        // ModelNode handler = new ModelNode();
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case HANDLER_NAME: {
                    handlerName = parseElementNoAttributes(reader);

                    break;
                }
                case HANDLER_CLASS: {
                    handlerClass = parseElementNoAttributes(reader);
                    // handler.get(Constants.HANDLER_CLASS).set(handlerClass);

                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        chainNode.get(Constants.HANDLER).get(handlerName).set(handlerClass);
    }
}