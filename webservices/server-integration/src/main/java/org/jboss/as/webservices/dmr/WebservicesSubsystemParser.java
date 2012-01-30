package org.jboss.as.webservices.dmr;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.webservices.dmr.Constants.CLASS;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.VALUE;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

final class WebservicesSubsystemParser implements XMLElementReader<List<ModelNode>> {

    private static final WebservicesSubsystemParser INSTANCE = new WebservicesSubsystemParser();

    static WebservicesSubsystemParser getInstance() {
        return INSTANCE;
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
                        case MODIFY_WSDL_ADDRESS: {
                            boolean b = Boolean.parseBoolean(parseElementNoAttributes(reader));
                            subsystem.get(MODIFY_WSDL_ADDRESS).set(b);
                            break;
                        }
                        case WSDL_HOST: {
                            subsystem.get(WSDL_HOST).set(parseElementNoAttributes(reader));
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
                            readEndpointConfigOld(reader, subsystem.get(OP_ADDR), endpointConfigs);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                case WEBSERVICES_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element != Element.ENDPOINT_CONFIG && !encountered.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case MODIFY_WSDL_ADDRESS: {
                            boolean b = Boolean.parseBoolean(parseElementNoAttributes(reader));
                            subsystem.get(MODIFY_WSDL_ADDRESS).set(b);
                            break;
                        }
                        case WSDL_HOST: {
                            subsystem.get(WSDL_HOST).set(parseElementNoAttributes(reader));
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

    private void readEndpointConfigOld(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> operationList) throws XMLStreamException {
        String configName = null;

        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.PROPERTY
                    && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case CONFIG_NAME: {
                    configName = parseElementNoAttributes(reader);
                    final ModelNode node = new ModelNode();
                    node.get(OP).set(ADD);
                    node.get(OP_ADDR).set(address).add(ENDPOINT_CONFIG, configName);
                    operationList.add(node);
                    break;
                }
                case PRE_HANDLER_CHAINS: {
                    parseHandlerChainsOld(reader, configName, operationList, true);
                    break;
                }
                case POST_HANDLER_CHAINS: {
                    parseHandlerChainsOld(reader, configName, operationList, false);
                    break;
                }
                case PROPERTY : {
                    final ModelNode operation = parsePropertyOld(reader, configName);
                    operationList.add(operation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void readEndpointConfig(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> operationList) throws XMLStreamException {
        String configName = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                configName = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }

        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(ENDPOINT_CONFIG, configName);
        operationList.add(node);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.PRE_HANDLER_CHAIN && element != Element.POST_HANDLER_CHAIN && element != Element.PROPERTY
                    && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PRE_HANDLER_CHAIN: {
                    parseHandlerChain(reader, configName, operationList, true);
                    break;
                }
                case POST_HANDLER_CHAIN: {
                    parseHandlerChain(reader, configName, operationList, false);
                    break;
                }
                case PROPERTY : {
                    final ModelNode operation = parseProperty(reader, configName);
                    operationList.add(operation);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parsePropertyOld(final XMLExtendedStreamReader reader, final String configName) throws XMLStreamException {
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
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(PROPERTY, propertyName);
        if (propertyValue != null) {
            operation.get(VALUE).set(propertyValue);
        }
        return operation;
    }

    private ModelNode parseProperty(final XMLExtendedStreamReader reader, final String configName) throws XMLStreamException {
        String propertyName = null;
        String propertyValue = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                propertyName = value;
                break;
            case VALUE:
                propertyValue = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }

        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(PROPERTY, propertyName);
        if (propertyValue != null) {
            operation.get(VALUE).set(propertyValue);
        }
        return operation;
    }

    private ModelNode parseHandlerChainsOld(final XMLExtendedStreamReader reader, final String configName, final List<ModelNode> operationList, final boolean isPreHandlerChain) throws XMLStreamException {
        ModelNode chainsNode = new ModelNode();
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER_CHAIN && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }

            switch (element) {
                case HANDLER_CHAIN: {
                    parseHandlerChainOld(reader, configName, operationList, isPreHandlerChain);
                    break;
                }

                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return chainsNode;

    }

    private void parseHandlerChainOld(final XMLExtendedStreamReader reader, final String configName, final List<ModelNode> operationList, final boolean isPreHandlerChain) throws XMLStreamException {
        String handlerChainId = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case ID:
                handlerChainId = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        if (handlerChainId == null) {
            handlerChainId = "auto-generated-" + System.currentTimeMillis();
        }
        String protocolBindings = null;
        final List<ModelNode> addHandlerOperations = new LinkedList<ModelNode>();
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PROTOCOL_BINDINGS: {
                    protocolBindings = parseElementNoAttributes(reader);
                    break;
                }
                case HANDLER: {
                    parseHandlerOld(reader, configName, handlerChainId, isPreHandlerChain, addHandlerOperations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode operation = new ModelNode();
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(handlerChainType, handlerChainId);
        if (protocolBindings != null) {
            operation.get(Constants.PROTOCOL_BINDINGS).set(protocolBindings);
        }
        operationList.add(operation);
        operationList.addAll(addHandlerOperations);
    }

    private void parseHandlerChain(final XMLExtendedStreamReader reader, final String configName, final List<ModelNode> operationList, final boolean isPreHandlerChain) throws XMLStreamException {
        String handlerChainId = null;
        String protocolBindings = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                handlerChainId = value;
                break;
            case PROTOCOL_BINDINGS:
                protocolBindings = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final List<ModelNode> addHandlerOperations = new LinkedList<ModelNode>();
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.HANDLER && !encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case HANDLER: {
                    parseHandler(reader, configName, handlerChainId, isPreHandlerChain, addHandlerOperations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode operation = new ModelNode();
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(handlerChainType, handlerChainId);
        if (protocolBindings != null) {
            operation.get(Constants.PROTOCOL_BINDINGS).set(protocolBindings);
        }
        operationList.add(operation);
        operationList.addAll(addHandlerOperations);
    }

    private void parseHandlerOld(final XMLExtendedStreamReader reader, final String configName, final String handlerChainId, final boolean isPreHandlerChain, final List<ModelNode> operations) throws XMLStreamException {
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
        final ModelNode operation = new ModelNode();
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(handlerChainType, handlerChainId).add(HANDLER, handlerName);
        operation.get(CLASS).set(handlerClass);
        operations.add(operation);
    }

    private void parseHandler(final XMLExtendedStreamReader reader, final String configName, final String handlerChainId, final boolean isPreHandlerChain, final List<ModelNode> operations) throws XMLStreamException {
        String handlerName = null;
        String handlerClass = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                handlerName = value;
                break;
            case CLASS:
                handlerClass = value;
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        final ModelNode operation = new ModelNode();
        final String handlerChainType = isPreHandlerChain ? PRE_HANDLER_CHAIN : POST_HANDLER_CHAIN;
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME).add(ENDPOINT_CONFIG, configName).add(handlerChainType, handlerChainId).add(HANDLER, handlerName);
        operation.get(CLASS).set(handlerClass);
        operations.add(operation);
    }
}