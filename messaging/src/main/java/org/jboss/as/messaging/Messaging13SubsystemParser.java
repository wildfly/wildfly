/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.DEFAULT;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.JMS_BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION;
import static org.jboss.as.messaging.CommonAttributes.USE_AUTO_RECOVERY;
import static org.jboss.as.messaging.Element.SOURCE;
import static org.jboss.as.messaging.Element.TARGET;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.messaging.jms.JndiEntriesAttribute;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;


/**
 * Messaging subsystem 1.3 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging13SubsystemParser extends Messaging12SubsystemParser {

    private static final Messaging13SubsystemParser INSTANCE = new Messaging13SubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    protected Messaging13SubsystemParser() {
    }

    @Override
    public Namespace getExpectedNamespace() {
        return Namespace.MESSAGING_1_3;
    }

    @Override
    protected void writePooledConnectionFactoryAttributes(XMLExtendedStreamWriter writer, String name, ModelNode factory)
            throws XMLStreamException {
        super.writePooledConnectionFactoryAttributes(writer, name, factory);

        USE_AUTO_RECOVERY.marshallAsElement(factory, writer);
    }

    protected ModelNode createConnectionFactory(XMLExtendedStreamReader reader, ModelNode connectionFactory, boolean pooled) throws XMLStreamException
    {
        Set<Element> seen = EnumSet.noneOf(Element.class);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!seen.add(element)) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }
            switch(element) {
                // =========================================================
                // elements common to regular & pooled connection factories
                case DISCOVERY_GROUP_REF: {
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.DISCOVERY_GROUP_REF, Element.CONNECTORS);
                    final String groupRef = readStringAttributeElement(reader, DISCOVERY_GROUP_NAME.getXmlName());
                    DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, connectionFactory, reader);
                    break;
                } case CONNECTORS: {
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.CONNECTORS, Element.DISCOVERY_GROUP_REF);
                    connectionFactory.get(CONNECTOR).set(processJmsConnectors(reader));
                    break;
                } case ENTRIES: {
                    while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final Element local = Element.forName(reader.getLocalName());
                        if(local != Element.ENTRY ) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                        JndiEntriesAttribute.CONNECTION_FACTORY.parseAndAddParameterElement(entry, connectionFactory, reader);
                    }
                    break;
                }
                case HA:
                case CLIENT_FAILURE_CHECK_PERIOD:
                case CONNECTION_TTL:
                case CALL_TIMEOUT:
                case CONSUMER_WINDOW_SIZE:
                case CONSUMER_MAX_RATE:
                case CONFIRMATION_WINDOW_SIZE:
                case PRODUCER_WINDOW_SIZE:
                case PRODUCER_MAX_RATE:
                case CACHE_LARGE_MESSAGE_CLIENT:
                case MIN_LARGE_MESSAGE_SIZE:
                case CLIENT_ID:
                case DUPS_OK_BATCH_SIZE:
                case TRANSACTION_BATH_SIZE:
                case BLOCK_ON_ACK:
                case BLOCK_ON_NON_DURABLE_SEND:
                case BLOCK_ON_DURABLE_SEND:
                case AUTO_GROUP:
                case PRE_ACK:
                case RETRY_INTERVAL_MULTIPLIER:
                case MAX_RETRY_INTERVAL:
                case FAILOVER_ON_INITIAL_CONNECTION:
                case FAILOVER_ON_SERVER_SHUTDOWN:
                case LOAD_BALANCING_CLASS_NAME:
                case USE_GLOBAL_POOLS:
                case GROUP_ID:
                    handleElementText(reader, element, connectionFactory);
                    break;
                case RETRY_INTERVAL:
                    // Use the "default" variant
                    handleElementText(reader, element, "default", connectionFactory);
                    break;
                case RECONNECT_ATTEMPTS:
                case SCHEDULED_THREAD_POOL_MAX_SIZE:
                case THREAD_POOL_MAX_SIZE:
                    // Use the "connection" variant
                    handleElementText(reader, element, "connection", connectionFactory);
                    break;
                // end of common elements
                // =========================================================

                // =========================================================
                // elements specific to regular (non-pooled) connection factories
                case CONNECTION_FACTORY_TYPE:
                    if(pooled) {
                        throw unexpectedElement(reader);
                    }
                    handleElementText(reader, element, connectionFactory);
                    break;
                // end of regular CF elements
                // =========================================================

                // =========================================================
                // elements specific to pooled connection factories
                case INBOUND_CONFIG: {
                    if(!pooled) {
                        throw unexpectedElement(reader);
                    }
                    while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final Element local = Element.forName(reader.getLocalName());
                        switch (local) {
                            case USE_JNDI:
                            case JNDI_PARAMS:
                            case USE_LOCAL_TX:
                            case SETUP_ATTEMPTS:
                            case SETUP_INTERVAL:
                                handleElementText(reader, local, connectionFactory);
                                break;
                            default:
                                throw unexpectedElement(reader);
                        }
                    }
                    break;
                } case TRANSACTION: {
                    if(!pooled) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    final String txType = reader.getAttributeValue(0);
                    if( txType != null) {
                        connectionFactory.get(TRANSACTION).set(txType);
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case USER:
                    if(!pooled) {
                        throw unexpectedElement(reader);
                    }
                    // Element name is overloaded, handleElementText can not be used, we must use the correct attribute
                    CommonAttributes.PCF_USER.parseAndSetParameter(reader.getElementText(), connectionFactory, reader);
                    break;
                case PASSWORD:
                    if(!pooled) {
                        throw unexpectedElement(reader);
                    }
                    // Element name is overloaded, handleElementText can not be used, we must use the correct attribute
                    CommonAttributes.PCF_PASSWORD.parseAndSetParameter(reader.getElementText(), connectionFactory, reader);
                    break;
                case MAX_POOL_SIZE:
                case MIN_POOL_SIZE:
                case USE_AUTO_RECOVERY:
                    if(!pooled) {
                        throw unexpectedElement(reader);
                    }
                    handleElementText(reader, element, connectionFactory);
                    break;
                // end of pooled CF elements
                // =========================================================
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        checkOnlyOneOfElements(reader, seen, Element.CONNECTORS, Element.DISCOVERY_GROUP_REF);

        return connectionFactory;
    }

    protected void processHornetQServers(final XMLExtendedStreamReader reader, final ModelNode subsystemAddress, final List<ModelNode> list) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Namespace schemaVer = Namespace.forUri(reader.getNamespaceURI());
            switch (schemaVer) {
                case MESSAGING_1_0:
                case UNKNOWN:
                    throw ParseUtils.unexpectedElement(reader);
                default: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case HORNETQ_SERVER:
                            processHornetQServer(reader, subsystemAddress, list, schemaVer);
                            break;
                        case JMS_BRIDGE:
                            processJmsBridge(reader, subsystemAddress, list);
                            break;
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }
    }

    private void processJmsBridge(XMLExtendedStreamReader reader, ModelNode subsystemAddress, List<ModelNode> list) throws XMLStreamException {
        String bridgeName = null;
        String moduleName = null;

        final int count = reader.getAttributeCount();
        for (int n = 0; n < count; n++) {
            String attrName = reader.getAttributeLocalName(n);
            Attribute attribute = Attribute.forName(attrName);
            switch (attribute) {
                case NAME:
                    bridgeName = reader.getAttributeValue(n);
                    break;
                case MODULE:
                    moduleName = reader.getAttributeValue(n);
                    break;
                default:
                    throw unexpectedAttribute(reader, n);
            }
        }

        if (bridgeName == null || bridgeName.length() == 0) {
            bridgeName = DEFAULT;
        }

        final ModelNode address = subsystemAddress.clone();
        address.add(JMS_BRIDGE, bridgeName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        list.add(operation);

        if (moduleName != null && moduleName.length() > 0) {
            JMSBridgeDefinition.MODULE.parseAndSetParameter(moduleName, operation, reader);
        }

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SOURCE:
                case TARGET:
                    processJmsBridgeResource(reader, operation, element.getLocalName());
                    break;
                case QUALITY_OF_SERVICE:
                case FAILURE_RETRY_INTERVAL:
                case MAX_RETRIES:
                case MAX_BATCH_SIZE:
                case MAX_BATCH_TIME:
                case SUBSCRIPTION_NAME:
                case CLIENT_ID:
                case ADD_MESSAGE_ID_IN_HEADER:
                    handleElementText(reader, element, operation);
                    break;
                case SELECTOR:
                    requireSingleAttribute(reader, CommonAttributes.STRING);
                    final String selector = readStringAttributeElement(reader, CommonAttributes.STRING);
                    SELECTOR.parseAndSetParameter(selector, operation, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void processJmsBridgeResource(XMLExtendedStreamReader reader, ModelNode operation, String modelName) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case USER:
                case PASSWORD:
                    handleElementText(reader, element, modelName, operation);
                    break;
                case CONNECTION_FACTORY:
                case DESTINATION:
                    handleSingleAttribute(reader, element, modelName, CommonAttributes.NAME, operation);
                    break;
                case CONTEXT:
                    ModelNode context = operation.get(element.getDefinition(modelName).getName());
                    processContext(reader, context);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void processContext(XMLExtendedStreamReader reader, ModelNode context) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY:
                    int count = reader.getAttributeCount();
                    String key = null;
                    String value = null;
                    for (int n = 0; n < count; n++) {
                        String attrName = reader.getAttributeLocalName(n);
                        Attribute attribute = Attribute.forName(attrName);
                        switch (attribute) {
                            case KEY:
                                key = reader.getAttributeValue(n);
                                break;
                            case VALUE:
                                value = reader.getAttributeValue(n);
                                break;
                            default:
                                throw unexpectedAttribute(reader, n);
                        }
                    }
                    context.get(key).set(value);
                    ParseUtils.requireNoContent(reader);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(getExpectedNamespace().getUriString(), false);
        final ModelNode node = context.getModelNode();

        if (node.hasDefined(HORNETQ_SERVER)) {
            final ModelNode servers = node.get(HORNETQ_SERVER);
            boolean first = true;
            for (Property prop : servers.asPropertyList()) {
                writeHornetQServer(writer, prop.getName(), prop.getValue());
                if (!first) {
                    writeNewLine(writer);
                } else {
                    first = false;
                }
            }
        }

        if (node.hasDefined(JMS_BRIDGE)) {
            final ModelNode jmsBridges = node.get(JMS_BRIDGE);
            boolean first = true;for (Property prop : jmsBridges.asPropertyList()) {
                writeJmsBridge(writer, prop.getName(), prop.getValue());
                if (!first) {
                    writeNewLine(writer);
                } else {
                    first = false;
                }
            }
        }
        writer.writeEndElement();
    }

    private void writeJmsBridge(XMLExtendedStreamWriter writer, String bridgeName, ModelNode value) throws XMLStreamException {
        writer.writeStartElement(Element.JMS_BRIDGE.getLocalName());

        if (!DEFAULT.equals(bridgeName)) {
            writer.writeAttribute(Attribute.NAME.getLocalName(), bridgeName);
        }

        JMSBridgeDefinition.MODULE.marshallAsAttribute(value, writer);

        writer.writeStartElement(SOURCE.getLocalName());
        for (SimpleAttributeDefinition attr : JMSBridgeDefinition.JMS_SOURCE_ATTRIBUTES) {
            attr.marshallAsElement(value, writer);
        }
        writer.writeEndElement();

        writer.writeStartElement(TARGET.getLocalName());
        for (SimpleAttributeDefinition attr : JMSBridgeDefinition.JMS_TARGET_ATTRIBUTES) {
            attr.marshallAsElement(value, writer);
        }
        writer.writeEndElement();

        for (SimpleAttributeDefinition attr : JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES) {
            if (attr == JMSBridgeDefinition.MODULE) {
                // handled as a XML attribute
                continue;
            }
            attr.marshallAsElement(value, writer);
        }

        writer.writeEndElement();
    }

    static void handleSingleAttribute(final XMLExtendedStreamReader reader, final Element element, final String modelName, String attributeName, final ModelNode node) throws XMLStreamException {
        AttributeDefinition attributeDefinition = element.getDefinition(modelName);
        final String value = readStringAttributeElement(reader, attributeName);
        if (attributeDefinition instanceof SimpleAttributeDefinition) {
            ((SimpleAttributeDefinition) attributeDefinition).parseAndSetParameter(value, node, reader);
        } else if (attributeDefinition instanceof ListAttributeDefinition) {
            ((ListAttributeDefinition) attributeDefinition).parseAndAddParameterElement(value, node, reader);
        }
    }
}
