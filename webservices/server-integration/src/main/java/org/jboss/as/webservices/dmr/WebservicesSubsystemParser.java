package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAINS;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAINS;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;
import static org.jboss.wsf.spi.util.StAXUtils.match;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
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
        //context = new SubsystemMarshallingContext(context.getModelNode(), writer);// for test
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        writer.writeNamespace("javaee", Namespace.JAVAEE.getUriString());
        writer.writeNamespace("jaxwsconfig", Namespace.JAXWSCONFIG.getUriString());
        ModelNode node = context.getModelNode();
        writeElement(writer, Element.MODIFY_WSDL_ADDRESS, node.require(MODIFY_WSDL_ADDRESS));
        writeElement(writer, Element.WSDL_HOST, node.require(WSDL_HOST));
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
        writer.writeStartElement(Namespace.JAXWSCONFIG.getUriString(), Constants.CONFIG_NAME);
        writer.writeCharacters(name);
        writer.writeEndElement();
        if (value.hasDefined(Constants.PRE_HANDLER_CHAINS)) {
            writer.writeStartElement(Namespace.JAXWSCONFIG.getUriString(), Constants.PRE_HANDLER_CHAINS);
            ModelNode handlerChains = value.get(Constants.PRE_HANDLER_CHAINS);
            writeHandlerChains(writer, handlerChains);
            writer.writeEndElement();
        }

        if (value.hasDefined(Constants.POST_HANDLER_CHAINS)) {
            writer.writeStartElement(Namespace.JAXWSCONFIG.getUriString(), Constants.POST_HANDLER_CHAINS);
            ModelNode handlerChains = value.get(Constants.POST_HANDLER_CHAINS);
            writeHandlerChains(writer, handlerChains);
            writer.writeEndElement();
        }

        if (value.hasDefined(Constants.PROPERTY)) {
            for (String key : value.get(PROPERTY).keys()) {
                writer.writeStartElement(Namespace.JAXWSCONFIG.getUriString(), Constants.PROPERTY);

                writer.writeStartElement(Namespace.JAXWSCONFIG.getUriString(), Constants.PROPERTY_NAME);
                writer.writeCharacters(key);
                writer.writeEndElement();

                writer.writeStartElement(Namespace.JAXWSCONFIG.getUriString(), Constants.PROPERTY_VALUE);
                writer.writeCharacters(value.get(PROPERTY).get(key).asString());
                writer.writeEndElement();

                writer.writeEndElement();
            }
        }

        if (value.hasDefined(Constants.FEATURE)) {
            for (String key : value.get(Constants.FEATURE).keys()) {
                writer.writeStartElement(Namespace.JAXWSCONFIG.getUriString(), Constants.FEATURE);

                writer.writeStartElement(Namespace.JAXWSCONFIG.getUriString(), Constants.FEATURE_NAME);
                writer.writeCharacters(key);
                writer.writeEndElement();
                //TODO: Feature data support
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeHandlerChains(final XMLExtendedStreamWriter writer, final ModelNode handlerChains)
            throws XMLStreamException {
        if (handlerChains.getType() == ModelType.LIST) {
            for (ModelNode handlerChain : handlerChains.asList()) {
                writer.writeStartElement(Namespace.JAVAEE.getUriString(), Constants.HANDLER_CHAIN);
                if (handlerChain.hasDefined(Constants.PROTOCOL_BINDING)) {
                    writer.writeStartElement(Namespace.JAVAEE.getUriString(), Constants.PROTOCOL_BINDING);
                    writer.writeCharacters(handlerChain.get(Constants.PROTOCOL_BINDING).asString());
                    writer.writeEndElement();
                }

                if (handlerChain.hasDefined(Constants.SERVICE_NAME_PATTERN)) {
                    writer.writeStartElement(Namespace.JAVAEE.getUriString(), Constants.SERVICE_NAME_PATTERN);
                    writer.writeCharacters(handlerChain.get(Constants.SERVICE_NAME_PATTERN).asString());
                    writer.writeEndElement();
                }

                if (handlerChain.hasDefined(Constants.PORT_NAME_PATTERN)) {
                    writer.writeStartElement(Namespace.JAVAEE.getUriString(), Constants.PORT_NAME_PATTERN);
                    writer.writeCharacters(handlerChain.get(Constants.PORT_NAME_PATTERN).asString());
                    writer.writeEndElement();
                }

                if (handlerChain.hasDefined(Constants.HANDLER)) {

                    for (String key : handlerChain.get(Constants.HANDLER).keys()) {
                        writer.writeStartElement(Namespace.JAVAEE.getUriString(), Constants.HANDLER);

                        writer.writeStartElement(Namespace.JAVAEE.getUriString(), Constants.HANDLER_NAME);
                        writer.writeCharacters(key);
                        writer.writeEndElement();

                        writer.writeStartElement(Namespace.JAVAEE.getUriString(), Constants.HANDLER_CLASS);
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
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEBSERVICES_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
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
                        case WSDL_PORT: {
                            int port = Integer.valueOf(parseElementNoAttributes(reader));
                            subsystem.get(WSDL_PORT).set(port);
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
        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);

        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.FEATURE && element != Element.PROPERTY
                    && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case CONFIG_NAME: {
                    configName = parseElementNoAttributes(reader);
                    node.get(OP_ADDR).set(address).add(ENDPOINT_CONFIG, configName);
                    break;
                }
                case PRE_HANDLER_CHAINS: {
                    preHandlers = parseHandlerChains(reader);
                    node.get(PRE_HANDLER_CHAINS).set(preHandlers);
                    break;
                }
                case POST_HANDLER_CHAINS: {
                    postHandlers = parseHandlerChains(reader);
                    node.get(POST_HANDLER_CHAINS).set(postHandlers);
                    break;
                }
                case PROPERTY : {
                    parseProperty(reader, node);
                    break;
                }
                case FEATURE : {
                    parseFeature(reader, node);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        operationList.add(node);
    }

    private void parseProperty(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        String propertyName = null;
        String propertyValue = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PROPERTY_NAME: {
                    propertyName = parseElementNoAttributes(reader);
                    break;
                }
                case PROPERTY_VALUE : {
                    propertyValue = parseElementNoAttributes(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        node.get(Constants.PROPERTY).get(propertyName).set(propertyValue);
    }

    private void parseFeature(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        String featureName = null;
        String featureData = "";
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case FEATURE_NAME: {
                    featureName = parseElementNoAttributes(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        node.get(Constants.FEATURE).get(featureName).set(featureData);
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
                    String serviceNamePattern = parseElementNoAttributes(reader);
                    chainNode.get(Constants.SERVICE_NAME_PATTERN).set(serviceNamePattern);
                    break;
                }

                case PORT_NAME_PATTERN: {
                    String portNamePattern = parseElementNoAttributes(reader);
                    chainNode.get(Constants.PORT_NAME_PATTERN).set(portNamePattern);
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