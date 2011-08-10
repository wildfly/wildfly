/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.parsePossibleExpression;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS_SETTING;
import static org.jboss.as.messaging.CommonAttributes.BINDINGS_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_GROUP;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONSUME_NAME;
import static org.jboss.as.messaging.CommonAttributes.CREATEDURABLEQUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.CREATE_NON_DURABLE_QUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.DELETEDURABLEQUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.DELETE_NON_DURABLE_QUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_REF;
import static org.jboss.as.messaging.CommonAttributes.DIVERT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.FACTORY_CLASS;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.GROUPING_HANDLER;
import static org.jboss.as.messaging.CommonAttributes.INBOUND_CONFIG;
import static org.jboss.as.messaging.CommonAttributes.JMS_CONNECTION_FACTORIES;
import static org.jboss.as.messaging.CommonAttributes.JMS_DESTINATIONS;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.LIVE_CONNECTOR_REF;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_TX;
import static org.jboss.as.messaging.CommonAttributes.MANAGE_NAME;
import static org.jboss.as.messaging.CommonAttributes.PAGING_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.PATH;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.RELATIVE_TO;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;
import static org.jboss.as.messaging.CommonAttributes.SEND_NAME;
import static org.jboss.as.messaging.CommonAttributes.SERVER_ID;
import static org.jboss.as.messaging.CommonAttributes.SOCKET_BINDING;
import static org.jboss.as.messaging.CommonAttributes.STATIC_CONNECTORS;
import static org.jboss.as.messaging.CommonAttributes.SUBSYSTEM;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.messaging.MessagingServices.TransportConfigType;
import org.jboss.as.messaging.jms.JndiEntriesAttribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The messaging subsystem domain parser
 *
 * @author scott.stark@jboss.org
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    private static final MessagingSubsystemParser INSTANCE = new MessagingSubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    private static final EnumSet<Element> SIMPLE_ROOT_RESOURCE_ELEMENTS = EnumSet.noneOf(Element.class);

    static {
        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            SIMPLE_ROOT_RESOURCE_ELEMENTS.add(Element.forName(attr.getXmlName()));
        }
    }

    private MessagingSubsystemParser() {
        //
    }


    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
        list.add(operation);

        EnumSet<Element> seen = EnumSet.noneOf(Element.class);
        // Handle elements
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            if (!seen.add(element)) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }

            switch (element) {
                case ACCEPTORS: {
                    // add acceptors
                    final ModelNode acceptors = processAcceptors(reader);
                    // TODO these should be resources
                    operation.get(ACCEPTOR).set(acceptors);
                    break;
                } case ADDRESS_SETTINGS: {
                    // add address settings
                    final ModelNode addressSettings = processAddressSettings(reader);
                    // TODO these should be resources
                    operation.get(ADDRESS_SETTING).set(addressSettings);
                    break;
                }
                case BINDINGS_DIRECTORY: {
                    final ModelNode directory = parseDirectory(reader);
                    // TODO this should be a resource
                    operation.get(BINDINGS_DIRECTORY).set(directory);
                    break;
                }
                case BRIDGES:
                    processBridges(reader, address, list);
                    break;
                case BROADCAST_GROUPS:
                    processBroadcastGroups(reader, address, list);
                    break;
                case CLUSTER_CONNECTIONS:
                    processClusterConnections(reader, address, list);
                    break;
                case CONNECTORS: {
                    final ModelNode connectors = processConnectors(reader);
                    // TODO these should be resources
                    operation.get(CONNECTOR).set(connectors);
                    break;
                }
                case CONNECTOR_SERVICES:
                    processConnectorServices(reader, address, list);
                    break;
                case DISCOVERY_GROUPS:
                    processDiscoveryGroups(reader, address, list);
                    break;
                case DIVERTS:
                    parseDiverts(reader, address, list);
                    break;
                case FILE_DEPLOYMENT_ENABLED:
                    // This isn't an element in the xsd as there is no filesystem support in AS
                    unhandledElement(reader, element);
                    break;
                case GROUPING_HANDLER:
                    processGroupingHandler(reader, address, list);
                    break;
                case JOURNAL_DIRECTORY: {
                    final ModelNode directory = parseDirectory(reader);
                    // TODO this should be a resource
                    operation.get(JOURNAL_DIRECTORY).set(directory);
                    break;
                }
                case LARGE_MESSAGES_DIRECTORY: {
                    final ModelNode dir = parseDirectory(reader);
                    operation.get(LARGE_MESSAGES_DIRECTORY).set(dir);
                    break;
                }
                case LIVE_CONNECTOR_REF: {
                    Location location = reader.getLocation();
                    String string = readStringAttributeElement(reader, CommonAttributes.CONNECTOR_NAME);
                    LIVE_CONNECTOR_REF.parseAndSetParameter(string, operation, location);
                    break;
                }
                case PAGING_DIRECTORY: {
                    final ModelNode directory = parseDirectory(reader);
                    // TODO this should be a resource
                    operation.get(PAGING_DIRECTORY).set(directory);
                    break;
                }
                case REMOTING_INTERCEPTORS:
                    processRemotingInterceptors(reader, operation);
                    break;
                case SECURITY_SETTINGS: {
                    // process security settings
                    final ModelNode securitySettings = processSecuritySettings(reader);
                    // TODO these should be resources
                    operation.get(SECURITY_SETTING).set(securitySettings);
                    break;
                }
                case CORE_QUEUES: {
                    parseQueues(reader, address, list);
                    break;
                }
                case CONNECTION_FACTORIES: {
                    processConnectionFactories(reader, address, list);
                    break;
                }
                case JMS_DESTINATIONS: {
                    processJmsDestinations(reader, address, list);
                    break;
                }
                case SCHEDULED_THREAD_POOL_MAX_SIZE:
                case THREAD_POOL_MAX_SIZE: {
                    // Use the "server" variant
                    handleElementText(reader, element, "server", operation);
                    break;
                }
                case SUBSYSTEM:
                    // The end of the subsystem element
                    break;
                default:
                    if (SIMPLE_ROOT_RESOURCE_ELEMENTS.contains(element)) {
                        AttributeDefinition attributeDefinition = element.getDefinition();
                        if (attributeDefinition instanceof SimpleAttributeDefinition) {
                            handleElementText(reader, element, operation);
                        } else {
                            // These should be handled in specific case blocks above, e.g. case REMOTING_INTERCEPTORS:
                            throw new UnsupportedOperationException(String.format("Implement support for element %s", element.getLocalName()));
                        }
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
            }
        } while (reader.hasNext() && localName.equals(ModelDescriptionConstants.SUBSYSTEM) == false);
    }

    private static void processConnectorServices(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR_SERVICE: {
                    processConnectorService(reader, address, updates);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private static void processConnectorService(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        final ModelNode serviceAddress = address.clone().add(CommonAttributes.CONNECTOR_SERVICE, name);
        final ModelNode add = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, serviceAddress);
        updates.add(add);

        EnumSet<Element> required = EnumSet.of(Element.FACTORY_CLASS);
        Set<Element> seen = EnumSet.noneOf(Element.class);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!seen.add(element) && element != Element.PARAM) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }
            required.remove(element);
            switch (element) {
                case FACTORY_CLASS: {
                    handleElementText(reader, element, add);
                    break;
                }
                case PARAM: {
                    final Location location = reader.getLocation();
                    String[] attrs = ParseUtils.requireAttributes(reader, Attribute.KEY.getLocalName(), Attribute.VALUE.getLocalName());
                    requireNoContent(reader);
                    final ModelNode paramAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, serviceAddress.clone().add(CommonAttributes.PARAM, attrs[0]));
                    CommonAttributes.VALUE.parseAndSetParameter(attrs[1], paramAdd, location);
                    updates.add(paramAdd);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }


        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

    }

    private static void processClusterConnections(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CLUSTER_CONNECTION: {
                    processClusterConnection(reader, address, updates);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private static void processClusterConnection(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        ModelNode bridgeAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.CLUSTER_CONNECTION, name));

        EnumSet<Element> required = EnumSet.of(Element.ADDRESS, Element.CONNECTOR_REF);
        Set<Element> seen = EnumSet.noneOf(Element.class);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!seen.add(element)) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }
            required.remove(element);
            switch (element) {
                case FORWARD_WHEN_NO_CONSUMERS:
                case MAX_HOPS:
                case CONFIRMATION_WINDOW_SIZE:
                    handleElementText(reader, element, bridgeAdd);
                    break;
                case ADDRESS:  {
                    handleElementText(reader, element, CommonAttributes.CLUSTER_CONNECTION_ADDRESS.getName(), bridgeAdd);
                    break;
                }
                case CONNECTOR_REF:  {
                    // Use the "simple" variant
                    handleElementText(reader, element, "simple", bridgeAdd);
                    break;
                }
                case USE_DUPLICATE_DETECTION:
                case RETRY_INTERVAL:
                    // Use the "cluster" variant
                    handleElementText(reader, element, "cluster", bridgeAdd);
                    break;
                case STATIC_CONNECTORS:
                    if (seen.contains(Element.DISCOVERY_GROUP_REF)) {
                        throw new XMLStreamException(String.format("Illegal element %s: cannot be used when %s is used",
                                STATIC_CONNECTORS, DISCOVERY_GROUP_REF), reader.getLocation());
                    }
                    processStaticConnectors(reader, bridgeAdd, true);
                    break;
                case DISCOVERY_GROUP_REF: {
                    if (seen.contains(Element.STATIC_CONNECTORS)) {
                        throw new XMLStreamException(String.format("Illegal element %s: cannot be used when %s is used",
                                DISCOVERY_GROUP_REF, STATIC_CONNECTORS), reader.getLocation());
                    }
                    final Location location = reader.getLocation();
                    final String groupRef = readStringAttributeElement(reader, DISCOVERY_GROUP_NAME.getXmlName());
                    DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, bridgeAdd, location);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

        updates.add(bridgeAdd);
    }

    private static void processBridges(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case BRIDGE: {
                    processBridge(reader, address, updates);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private static void processBridge(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        ModelNode bridgeAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.BRIDGE, name));

        EnumSet<Element> required = EnumSet.of(Element.QUEUE_NAME);
        Set<Element> seen = EnumSet.noneOf(Element.class);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!seen.add(element)) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }
            required.remove(element);
            switch (element) {
                case QUEUE_NAME:
                case HA:
                case TRANSFORMER_CLASS_NAME:
                case RETRY_INTERVAL_MULTIPLIER:
                case FAILOVER_ON_SERVER_SHUTDOWN:
                case USE_DUPLICATE_DETECTION:
                case CONFIRMATION_WINDOW_SIZE:
                case USER:
                case PASSWORD:
                    handleElementText(reader, element, bridgeAdd);
                    break;
                case FILTER:  {
                    final Location location = reader.getLocation();
                    String string = readStringAttributeElement(reader, CommonAttributes.STRING);
                    FILTER.parseAndSetParameter(string, bridgeAdd, location);
                    break;
                }
                case RETRY_INTERVAL:
                    // Use the "default" variant
                    handleElementText(reader, element, "default", bridgeAdd);
                    break;
                case FORWARDING_ADDRESS:
                case RECONNECT_ATTEMPTS:
                    handleElementText(reader, element, "bridge", bridgeAdd);
                    break;
                case STATIC_CONNECTORS:
                    if (seen.contains(Element.DISCOVERY_GROUP_REF)) {
                        throw new XMLStreamException(String.format("Illegal element %s: cannot be used when %s is used",
                                STATIC_CONNECTORS, DISCOVERY_GROUP_REF), reader.getLocation());
                    }
                    processStaticConnectors(reader, bridgeAdd, false);
                    break;
                case DISCOVERY_GROUP_REF: {
                    if (seen.contains(Element.STATIC_CONNECTORS)) {
                        throw new XMLStreamException(String.format("Illegal element %s: cannot be used when %s is used",
                                DISCOVERY_GROUP_REF, STATIC_CONNECTORS), reader.getLocation());
                    }
                    final Location location = reader.getLocation();
                    final String groupRef = readStringAttributeElement(reader, DISCOVERY_GROUP_NAME.getXmlName());
                    DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, bridgeAdd, location);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        if (!seen.contains(Element.STATIC_CONNECTORS) && !seen.contains(Element.DISCOVERY_GROUP_REF)) {
            throw new XMLStreamException(String.format("Either %s or %s is required", Element.STATIC_CONNECTORS.getLocalName(),
                    Element.DISCOVERY_GROUP_REF.getLocalName()), reader.getLocation());
        }

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

        updates.add(bridgeAdd);
    }

    private static void processStaticConnectors(XMLExtendedStreamReader reader, ModelNode addOperation, boolean cluster) throws XMLStreamException {

        if (cluster) {

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ALLOW_DIRECT_CONNECTIONS_ONLY: {
                        final Location location = reader.getLocation();
                        final String attrValue = reader.getAttributeValue(i);
                        CommonAttributes.ALLOW_DIRECT_CONNECTIONS_ONLY.parseAndSetParameter(attrValue, addOperation, location);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }

        } else {
            requireNoAttributes(reader);
        }

        EnumSet<Element> required = EnumSet.of(Element.CONNECTOR_REF);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            switch (element) {
                case CONNECTOR_REF: {
                    handleElementText(reader, element, cluster ? "cluster-connection" : "bridge", addOperation);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }
    }

    private void processGroupingHandler(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        ModelNode groupingHandlerAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.GROUPING_HANDLER, name));

        EnumSet<Element> required = EnumSet.of(Element.ADDRESS, Element.TYPE);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            switch (element) {
                case TYPE:
                case TIMEOUT: {
                    handleElementText(reader, element, groupingHandlerAdd);
                    break;
                }
                case ADDRESS: {
                    handleElementText(reader, element, CommonAttributes.GROUPING_HANDLER_ADDRESS.getName(), groupingHandlerAdd);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

        updates.add(groupingHandlerAdd);
    }

    private void processRemotingInterceptors(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CLASS_NAME: {
                    final Location location = reader.getLocation();
                    final String value = reader.getElementText();
                    REMOTING_INTERCEPTORS.parseAndAddParameterElement(value, operation, location);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static void processBroadcastGroups(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case BROADCAST_GROUP: {
                    parseBroadcastGroup(reader, address, updates);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseBroadcastGroup(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        ModelNode broadcastGroupAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.BROADCAST_GROUP, name));

        EnumSet<Element> required = EnumSet.of(Element.GROUP_ADDRESS, Element.GROUP_PORT);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            switch (element) {
                case LOCAL_BIND_ADDRESS:
                case LOCAL_BIND_PORT:
                case GROUP_ADDRESS:
                case GROUP_PORT:
                case BROADCAST_PERIOD:
                    handleElementText(reader, element, broadcastGroupAdd);
                    break;
                case CONNECTOR_REF:
                    handleElementText(reader, element, "broadcast-group", broadcastGroupAdd);
                    break;
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

        updates.add(broadcastGroupAdd);
    }

    static void processDiscoveryGroups(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case DISCOVERY_GROUP: {
                    parseDiscoveryGroup(reader, address, updates);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseDiscoveryGroup(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        ModelNode broadcastGroupAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.DISCOVERY_GROUP, name));

        EnumSet<Element> required = EnumSet.of(Element.GROUP_ADDRESS, Element.GROUP_PORT);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            switch (element) {
                case LOCAL_BIND_ADDRESS:
                case GROUP_ADDRESS:
                case GROUP_PORT:
                case REFRESH_TIMEOUT:
                case INITIAL_WAIT_TIMEOUT:
                    handleElementText(reader, element, broadcastGroupAdd);
                    break;
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

        updates.add(broadcastGroupAdd);
    }

    static void processConnectionFactories(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
       while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
          final Element element = Element.forName(reader.getLocalName());
          switch(element) {
             case CONNECTION_FACTORY:
                processConnectionFactory(reader, address, updates);
               break;
             case POOLED_CONNECTION_FACTORY:
                processPooledConnectionFactory(reader, address, updates);
               break;
             default:
                    throw ParseUtils.unexpectedElement(reader);
          }
       }
    }

   static void processJmsDestinations(final XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> updates) throws XMLStreamException {
       while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
          final Element element = Element.forName(reader.getLocalName());
          switch(element) {
             case JMS_QUEUE:
                processJMSQueue(reader, address, updates);
               break;
             case JMS_TOPIC:
                processJMSTopic(reader, address, updates);
               break;
             default:
                    throw ParseUtils.unexpectedElement(reader);
          }
       }
    }

    static ModelNode processAcceptors(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode acceptors = new ModelNode();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            int serverId = 0;

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    }
                    case SOCKET_BINDING: {
                        socketBinding = attrValue;
                        break;
                    }
                    case SERVER_ID: {
                        serverId = Integer.valueOf(attrValue);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ACCEPTOR: {
                    final ModelNode acceptor = acceptors.get(name);
                    acceptor.get(TYPE).set(TransportConfigType.Generic.toString());
                    if(socketBinding != null) acceptor.get(SOCKET_BINDING).set(socketBinding);
                    parseTransportConfigurationParams(reader, acceptor, true);
                    break;
                } case NETTY_ACCEPTOR: {
                    final ModelNode acceptor = acceptors.get(name);
                    acceptor.get(TYPE).set(TransportConfigType.Remote.toString());
                    if(socketBinding != null) acceptor.get(SOCKET_BINDING).set(socketBinding);
                    parseTransportConfigurationParams(reader, acceptor, false);
                    break;
                } case IN_VM_ACCEPTOR: {
                    final ModelNode acceptor = acceptors.get(name);
                    acceptor.get(TYPE).set(TransportConfigType.InVM.toString());
                    acceptor.get(SERVER_ID).set(serverId);
                    parseTransportConfigurationParams(reader, acceptor, false);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return acceptors;
    }

    static void parseQueues(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case QUEUE: {
                    if(name == null) {
                        throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME.getLocalName()));
                    }
                    final ModelNode op = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.QUEUE, name));
                    parseQueue(reader, op);
                    if(! op.hasDefined(QUEUE_ADDRESS.getName())) {
                        throw ParseUtils.missingRequired(reader, Collections.singleton(Element.ADDRESS.getLocalName()));
                    }
                    list.add(op);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static void parseQueue(final XMLExtendedStreamReader reader, final ModelNode queue) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDRESS: {
                    handleElementText(reader, element, QUEUE_ADDRESS.getName(), queue);
                    break;
                } case FILTER: {
                    Location location = reader.getLocation();
                    String string = readStringAttributeElement(reader, CommonAttributes.STRING);
                    FILTER.parseAndSetParameter(string, queue, location);
                    break;
                } case DURABLE: {
                    handleElementText(reader, element, queue);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static ModelNode processSecuritySettings(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode security = new ModelNode();
        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());

            switch (element) {
                case SECURITY_SETTING:
                    String match = reader.getAttributeValue(0);
                    parseSecurityRoles(reader, security.get(match));
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.SECURITY_SETTING.getLocalName()));
        return security;
    }

    static void parseSecurityRoles(final XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {

        ArrayList<String> send = new ArrayList<String>();
        ArrayList<String> consume = new ArrayList<String>();
        ArrayList<String> createDurableQueue = new ArrayList<String>();
        ArrayList<String> deleteDurableQueue = new ArrayList<String>();
        ArrayList<String> createNonDurableQueue = new ArrayList<String>();
        ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
        ArrayList<String> manageRoles = new ArrayList<String>();
        ArrayList<String> allRoles = new ArrayList<String>();

        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (element != Element.PERMISSION_ELEMENT_NAME) {
                break;
            }

            List<String> roles = null;
            String type = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                requireNoNamespaceAttribute(reader, i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ROLES_ATTR_NAME:
                        roles = reader.getListAttributeValue(i);
                        break;
                    case TYPE_ATTR_NAME:
                        type = reader.getAttributeValue(i);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }

            for (String role : roles) {
                if (Attribute.SEND_NAME.getLocalName().equals(type)) {
                    send.add(role.trim());
                } else if (Attribute.CONSUME_NAME.getLocalName().equals(type)) {
                    consume.add(role.trim());
                } else if (Attribute.CREATEDURABLEQUEUE_NAME.getLocalName().equals(type)) {
                    createDurableQueue.add(role);
                } else if (Attribute.DELETEDURABLEQUEUE_NAME.getLocalName().equals(type)) {
                    deleteDurableQueue.add(role);
                } else if (Attribute.CREATE_NON_DURABLE_QUEUE_NAME.getLocalName().equals(type)) {
                    createNonDurableQueue.add(role);
                } else if (Attribute.DELETE_NON_DURABLE_QUEUE_NAME.getLocalName().equals(type)) {
                    deleteNonDurableQueue.add(role);
                } else if (Attribute.CREATETEMPQUEUE_NAME.getLocalName().equals(type)) {
                    createNonDurableQueue.add(role);
                } else if (Attribute.DELETETEMPQUEUE_NAME.getLocalName().equals(type)) {
                    deleteNonDurableQueue.add(role);
                } else if (Attribute.MANAGE_NAME.getLocalName().equals(type)) {
                    manageRoles.add(role);
                }
                if (!allRoles.contains(role.trim())) {
                    allRoles.add(role.trim());
                }
            }
            // Scan to element end
            reader.discardRemainder();
        } while (reader.hasNext());

        for (String role : allRoles) {
            node.get(role, SEND_NAME).set(send.contains(role));
            node.get(role, CONSUME_NAME).set(consume.contains(role));
            node.get(role, CREATEDURABLEQUEUE_NAME).set(createDurableQueue.contains(role));
            node.get(role, DELETEDURABLEQUEUE_NAME).set(deleteDurableQueue.contains(role));
            node.get(role, CREATE_NON_DURABLE_QUEUE_NAME).set(createNonDurableQueue.contains(role));
            node.get(role, DELETE_NON_DURABLE_QUEUE_NAME).set(deleteNonDurableQueue.contains(role));
            node.get(role, MANAGE_NAME).set(manageRoles.contains(role));
        }
    }

    public static ModelNode processConnectors(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode connectors = new ModelNode();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            int serverId = 0;

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    }
                    case SOCKET_BINDING: {
                        socketBinding = attrValue;
                        break;
                    }
                    case SERVER_ID: {
                        serverId = Integer.valueOf(attrValue);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR: {
                    final ModelNode connector = connectors.get(name);
                    connector.get(TYPE).set(TransportConfigType.Generic.toString());
                    if(socketBinding != null) connector.get(SOCKET_BINDING).set(socketBinding);
                    parseTransportConfigurationParams(reader, connector, true);
                    break;
                } case NETTY_CONNECTOR: {
                    final ModelNode connector = connectors.get(name);
                    connector.get(TYPE).set(TransportConfigType.Remote.toString());
                    if(socketBinding != null) connector.get(SOCKET_BINDING).set(socketBinding);
                    parseTransportConfigurationParams(reader, connector, false);
                    break;
                } case IN_VM_CONNECTOR: {
                    final ModelNode connector = connectors.get(name);
                    connector.get(TYPE).set(TransportConfigType.InVM.toString());
                    connector.get(SERVER_ID).set(serverId);
                    parseTransportConfigurationParams(reader, connector, false);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return connectors;
    }

    static ModelNode processAddressSettings(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode settings = new ModelNode();

        String localName = null;
        int tag = reader.getEventType();
        do {
            reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            switch (element) {
                case ADDRESS_SETTING:
                    // Add address settings
                    String match = reader.getAttributeValue(0);
                    settings.get(match).set(parseAddressSettings(reader));
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.ADDRESS_SETTING.getLocalName()));

        return settings;
    }

    static ModelNode parseAddressSettings(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode addressSettingsSpec = new ModelNode();

        String localName = null;
        int tag;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            SWITCH:
            switch (element) {
                case DEAD_LETTER_ADDRESS_NODE_NAME:
                case EXPIRY_ADDRESS_NODE_NAME:
                case REDELIVERY_DELAY_NODE_NAME:
                case MAX_SIZE_BYTES_NODE_NAME:
                case PAGE_SIZE_BYTES_NODE_NAME:
                case MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME:
                case ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME:
                case LVQ_NODE_NAME:
                case MAX_DELIVERY_ATTEMPTS:
                case REDISTRIBUTION_DELAY_NODE_NAME:
                case SEND_TO_DLA_ON_NO_ROUTE: {
                    handleElementText(reader, element, addressSettingsSpec);
                    break SWITCH;
                } default: {
                    break;
                }
            }
        } while (!reader.getLocalName().equals(Element.ADDRESS_SETTING.getLocalName()) && reader.getEventType() == XMLExtendedStreamReader.END_ELEMENT);

        return addressSettingsSpec;
    }

    static void parseTransportConfigurationParams(final XMLExtendedStreamReader reader, final ModelNode transportConfig, final boolean generic) throws XMLStreamException {
        final ModelNode params = new ModelNode();

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
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

            Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case FACTORY_CLASS: {
                    if(! generic) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    transportConfig.get(FACTORY_CLASS.getName()).set(reader.getElementText().trim());
                    break;
                }
                case PARAM: {
                    params.add(key, value);
                    ParseUtils.requireNoContent(reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        transportConfig.get(PARAM).set(params);
    }

    static ModelNode parseDirectory(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode directory = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {

            requireNoNamespaceAttribute(reader, i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            final String value = reader.getAttributeValue(i);
            switch (attribute) {
                case RELATIVE_TO:
                    directory.get(RELATIVE_TO).set(value);
                    break;
                case PATH:
                    directory.get(PATH).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        return directory;
    }

    private static void parseDiverts(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case DIVERT: {
                    parseDivert(reader, address, list);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseDivert(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        ModelNode divertAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.DIVERT, name));

        EnumSet<Element> required = EnumSet.of(Element.ADDRESS, Element.FORWARDING_ADDRESS);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            switch (element) {
                case ROUTING_NAME: {
                    handleElementText(reader, element, divertAdd);
                    break;
                }
                case ADDRESS: {
                    handleElementText(reader, element, CommonAttributes.DIVERT_ADDRESS.getName(), divertAdd);
                    break;
                }
                case FORWARDING_ADDRESS: {
                    handleElementText(reader, element, "divert", divertAdd);
                    break;
                }
                case FILTER: {
                    Location location = reader.getLocation();
                    String string = readStringAttributeElement(reader, CommonAttributes.STRING);
                    FILTER.parseAndSetParameter(string, divertAdd, location);
                    break;
                }
                case TRANSFORMER_CLASS_NAME: {
                    handleElementText(reader, element, divertAdd);
                    break;
                }
                case EXCLUSIVE: {
                    handleElementText(reader, element, divertAdd);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

        list.add(divertAdd);
    }

    static void unhandledElement(XMLExtendedStreamReader reader, Element element) throws XMLStreamException {
        throw new XMLStreamException(String.format("Ignoring unhandled element: %s, at: %s", element, reader.getLocation().toString()));
    }

    static void handleElementText(final XMLExtendedStreamReader reader, final Element element, final ModelNode node) throws XMLStreamException {
        handleElementText(reader, element, null, node);
    }

    static void handleElementText(final XMLExtendedStreamReader reader, final Element element, final String modelName, final ModelNode node) throws XMLStreamException {
        AttributeDefinition attributeDefinition = modelName == null ? element.getDefinition() : element.getDefinition(modelName);
        if (attributeDefinition != null) {
            Location location = reader.getLocation();
            final String value = reader.getElementText();
            if (attributeDefinition instanceof SimpleAttributeDefinition) {
                ((SimpleAttributeDefinition) attributeDefinition).parseAndSetParameter(value, node, location);
            } else if (attributeDefinition instanceof ListAttributeDefinition) {
                ((ListAttributeDefinition) attributeDefinition).parseAndAddParameterElement(value, node, location);
            }
        } else {
            handleElementText(reader, element, node, ModelType.STRING, true, false);
        }
    }

    /** @deprecated use AttributeDefinition */
    @Deprecated
    static void handleElementText(final XMLExtendedStreamReader reader, final Element element, final ModelNode node, final ModelType expectedType,
                                  final boolean allowNull, final boolean allowExpression) throws XMLStreamException {
        Location location = reader.getLocation();
        final String value = reader.getElementText();
        if(value != null && value.length() > 0) {
            ModelNode toSet = node.get(element.getLocalName());
            ModelNode modelValue = allowExpression ? parsePossibleExpression(value.trim()) : new ModelNode().set(value.trim());
            if (!allowExpression || modelValue.getType() != ModelType.EXPRESSION) {
                toSet.set(modelValue);
            }
            else {
                try {
                    switch (expectedType) {
                        case BOOLEAN:
                            toSet.set(modelValue.asBoolean());
                            break;
                        case BIG_DECIMAL:
                            toSet.set(modelValue.asBigDecimal());
                            break;
                        case BIG_INTEGER:
                            toSet.set(modelValue.asBigInteger());
                            break;
                        case BYTES:
                            toSet.set(modelValue.asBytes());
                            break;
                        case DOUBLE:
                            toSet.set(modelValue.asDouble());
                            break;
                        case INT:
                            toSet.set(modelValue.asInt());
                            break;
                        case LONG:
                            toSet.set(modelValue.asLong());
                            break;
                        case STRING:
                            toSet.set(modelValue.asString());
                            break;
                        default:
                            throw new XMLStreamException(String.format("Illegal value %s for element %s", value, element.getLocalName()), location);
                    }
                } catch (IllegalArgumentException iae) {
                    throw new XMLStreamException(String.format("Illegal value %s for element %s as it could not be converted to required type %s", value, element.getLocalName(), expectedType), location);
                }
            }
        } else if (!allowNull) {
            throw new XMLStreamException(String.format("Illegal value %s for element %s", value, element.getLocalName()), location);
        }
    }

    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        final ModelNode node = context.getModelNode();

        for (AttributeDefinition simpleAttribute : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            simpleAttribute.marshallAsElement(node, writer);
        }

        if (has(node, ACCEPTOR)) {
            writeAcceptors(writer, node.get(ACCEPTOR));
        }
        if (has(node, ADDRESS_SETTING)) {
            writeAddressSettings(writer, node.get(ADDRESS_SETTING));
        }
        if (has(node, BINDINGS_DIRECTORY)) {
            writeDirectory(writer, Element.BINDINGS_DIRECTORY, node);
        }
        if (has(node, CONNECTOR)) {
            writeConnectors(writer, node.get(CONNECTOR));
        }
        if (node.hasDefined(BROADCAST_GROUP)) {
            writeBroadcastGroups(writer, node.get(BROADCAST_GROUP));
        }
        if (node.hasDefined(DISCOVERY_GROUP)) {
            writeDiscoveryGroups(writer, node.get(DISCOVERY_GROUP));
        }
        if (node.hasDefined(DIVERT)) {
            writeDiverts(writer, node.get(DIVERT));
        }
        if (node.hasDefined(CommonAttributes.BRIDGE)) {
            writeBridges(writer, node.get(CommonAttributes.BRIDGE));
        }
        if (node.hasDefined(CommonAttributes.CLUSTER_CONNECTION)) {
            writeClusterConnections(writer, node.get(CommonAttributes.CLUSTER_CONNECTION));
        }
        if (node.hasDefined(CommonAttributes.GROUPING_HANDLER)) {
            writeGroupingHandler(writer, node.get(GROUPING_HANDLER));
        }
        if (has(node, CommonAttributes.JOURNAL_DIRECTORY)) {
            writeDirectory(writer, Element.JOURNAL_DIRECTORY, node);
        }
        if (has(node, CommonAttributes.LARGE_MESSAGES_DIRECTORY)) {
            writeDirectory(writer, Element.LARGE_MESSAGES_DIRECTORY, node);
        }
        if (has(node, CommonAttributes.PAGING_DIRECTORY)) {
            writeDirectory(writer, Element.PAGING_DIRECTORY, node);
        }
        if (has(node, CommonAttributes.SECURITY_SETTING)) {
            writeSecuritySettings(writer, node.get(CommonAttributes.SECURITY_SETTING));
        }
        if (has(node, CommonAttributes.QUEUE)) {
            writeQueues(writer, node.get(CommonAttributes.QUEUE));
        }
        if (node.hasDefined(CommonAttributes.CONNECTOR_SERVICE)) {
            writeConnectorServices(writer, node.get(CommonAttributes.CONNECTOR_SERVICE));
        }
        if (node.has(CONNECTION_FACTORY) || node.has(POOLED_CONNECTION_FACTORY)) {
           writer.writeStartElement(JMS_CONNECTION_FACTORIES);
           if (node.has(CONNECTION_FACTORY)) {
               writeConnectionFactories(writer, node.get(CONNECTION_FACTORY));
           }
           if (node.has(POOLED_CONNECTION_FACTORY)) {
               writePooledConnectionFactories(writer, node.get(POOLED_CONNECTION_FACTORY));
           }
           writer.writeEndElement();
        }
        if (node.has(JMS_QUEUE) || node.has(JMS_TOPIC)) {
           writer.writeStartElement(JMS_DESTINATIONS);
           if(node.has(JMS_QUEUE)) {
            writeJmsQueues(writer, node.get(JMS_QUEUE));
           }
           if (node.has(JMS_TOPIC)) {
               writeTopics(writer, node.get(JMS_TOPIC));
           }
           writer.writeEndElement();
        }


        writer.writeEndElement();
    }

    private void writeConnectorServices(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.CONNECTOR_SERVICES.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.CONNECTOR_SERVICE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                final ModelNode service = property.getValue();
                for (AttributeDefinition attribute : CommonAttributes.CONNECTOR_SERVICE_ATTRIBUTES) {
                    attribute.marshallAsElement(property.getValue(), writer);
                }
                if (service.hasDefined(CommonAttributes.PARAM)) {
                    for (Property param : service.get(CommonAttributes.PARAM).asPropertyList()) {
                        writer.writeEmptyElement(Element.PARAM.getLocalName());
                        writer.writeAttribute(Attribute.KEY.getLocalName(), param.getName());
                        writer.writeAttribute(Attribute.VALUE.getLocalName(), param.getValue().get(CommonAttributes.VALUE.getName()).asString());
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeBridges(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.BRIDGES.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.BRIDGE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                final ModelNode bridge = property.getValue();
                for (AttributeDefinition attribute : CommonAttributes.BRIDGE_ATTRIBUTES) {
                    if (CommonAttributes.FILTER == attribute) {
                        writeFilter(writer, property.getValue());
                    } else if (attribute == CommonAttributes.DISCOVERY_GROUP_NAME) {
                        if (CommonAttributes.DISCOVERY_GROUP_NAME.isMarshallable(bridge, false)) {
                            writer.writeStartElement(Element.DISCOVERY_GROUP_REF.getLocalName());
                            CommonAttributes.DISCOVERY_GROUP_NAME.marshallAsAttribute(bridge, writer);
                            writer.writeEndElement();
                        }
                    } else {
                        attribute.marshallAsElement(property.getValue(), writer);
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeClusterConnections(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.CLUSTER_CONNECTIONS.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.CLUSTER_CONNECTION.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                final ModelNode cluster = property.getValue();
                for (AttributeDefinition attribute : CommonAttributes.CLUSTER_CONNECTION_ATTRIBUTES) {
                    if (attribute == CommonAttributes.ALLOW_DIRECT_CONNECTIONS_ONLY) {
                        // we nest it in static-connectors
                        continue;
                    }
                    if (attribute == ConnectorRefsAttribute.CLUSTER_CONNECTION_CONNECTORS) {
                        if (ConnectorRefsAttribute.CLUSTER_CONNECTION_CONNECTORS.isMarshallable(cluster, false)) {
                            writer.writeStartElement(Element.STATIC_CONNECTORS.getLocalName());
                            CommonAttributes.ALLOW_DIRECT_CONNECTIONS_ONLY.marshallAsAttribute(cluster, writer);
                            ConnectorRefsAttribute.CLUSTER_CONNECTION_CONNECTORS.marshallAsElement(cluster, writer);
                            writer.writeEndElement();
                        } else if (CommonAttributes.ALLOW_DIRECT_CONNECTIONS_ONLY.isMarshallable(cluster, false)) {
                            writer.writeEmptyElement(Element.STATIC_CONNECTORS.getLocalName());
                            CommonAttributes.ALLOW_DIRECT_CONNECTIONS_ONLY.marshallAsAttribute(cluster, writer);
                        }
                    }
                    else if (attribute == CommonAttributes.DISCOVERY_GROUP_NAME) {
                        if (CommonAttributes.DISCOVERY_GROUP_NAME.isMarshallable(cluster, false)) {
                            writer.writeStartElement(Element.DISCOVERY_GROUP_REF.getLocalName());
                            CommonAttributes.DISCOVERY_GROUP_NAME.marshallAsAttribute(cluster, writer);
                            writer.writeEndElement();
                        }
                    } else {
                        attribute.marshallAsElement(property.getValue(), writer);
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeGroupingHandler(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {

        boolean wroteHandler = false;
        for (Property handler : node.asPropertyList()) {
            if (wroteHandler) {
                throw new IllegalStateException(String.format("Multiple %s children found; only one is allowed", GROUPING_HANDLER));
            }
            writer.writeStartElement(Element.GROUPING_HANDLER.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), handler.getName());
            final ModelNode resourceModel = handler.getValue();
            for (AttributeDefinition attr : CommonAttributes.GROUPING_HANDLER_ATTRIBUTES) {
                attr.marshallAsElement(resourceModel, writer);
            }
            writer.writeEndElement();
        }
    }


    private void writeAcceptors(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.ACCEPTORS.getLocalName());
        for(final Property property : node.asPropertyList()) {
            final ModelNode value = property.getValue();
            if (!has(value, TYPE)) {
                continue;
            }
            switch( Enum.valueOf(TransportConfigType.class, value.get(TYPE).asString())) {
                case Generic:
                    writer.writeStartElement(Element.ACCEPTOR.getLocalName());
                    break;
                case Remote:
                    writer.writeStartElement(Element.NETTY_ACCEPTOR.getLocalName());
                    break;
                case InVM:
                    writer.writeStartElement(Element.IN_VM_ACCEPTOR.getLocalName());
                    break;
            }

            writeAcceptorAndConnectorContent(writer, property);

            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeConnectors(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.CONNECTORS.getLocalName());
        for(final Property property : node.asPropertyList()) {
            final ModelNode value = property.getValue();
            if (!has(value, TYPE)) {
                continue;
            }
            switch( Enum.valueOf(TransportConfigType.class, value.get(TYPE).asString())) {
                case Generic:
                    writer.writeStartElement(Element.CONNECTOR.getLocalName());
                    break;
                case Remote:
                    writer.writeStartElement(Element.NETTY_CONNECTOR.getLocalName());
                    break;
                case InVM:
                    writer.writeStartElement(Element.IN_VM_CONNECTOR.getLocalName());
                    break;
            }
            writeAcceptorAndConnectorContent(writer, property);

            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeAcceptorAndConnectorContent(final XMLExtendedStreamWriter writer, final Property property) throws XMLStreamException {
        writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
        final ModelNode value = property.getValue();

        if (has(value, SOCKET_BINDING)) {
            writeAttribute(writer, Attribute.SOCKET_BINDING, value.get(SOCKET_BINDING));
        }
        if (has(value, SERVER_ID)) {
            writeAttribute(writer, Attribute.SERVER_ID, value.get(SERVER_ID));
        }

        if (has(value, FACTORY_CLASS.getName())) {
            writeSimpleElement(writer, Element.FACTORY_CLASS, value);
        }
        if (has(value, PARAM)) {
            for(final Property parameter : value.get(PARAM).asPropertyList()) {
                writer.writeStartElement(Element.PARAM.getLocalName());
                writer.writeAttribute(Attribute.KEY.getLocalName(), parameter.getName());
                writeAttribute(writer, Attribute.VALUE, parameter.getValue());
                writer.writeEndElement();
            }
        }
    }

    private void writeSecuritySettings(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.SECURITY_SETTINGS.getLocalName());

        for (Property matchRoles : node.asPropertyList()) {
            writer.writeStartElement(Element.SECURITY_SETTING.getLocalName());
            writer.writeAttribute(Attribute.MATCH.getLocalName(), matchRoles.getName());

            ArrayList<String> send = new ArrayList<String>();
            ArrayList<String> consume = new ArrayList<String>();
            ArrayList<String> createDurableQueue = new ArrayList<String>();
            ArrayList<String> deleteDurableQueue = new ArrayList<String>();
            ArrayList<String> createNonDurableQueue = new ArrayList<String>();
            ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
            ArrayList<String> manageRoles = new ArrayList<String>();

            for (Property rolePerms : matchRoles.getValue().asPropertyList()) {
                final String role = rolePerms.getName();
                final ModelNode perms = rolePerms.getValue();
                if (perms.get(CommonAttributes.SEND_NAME).asBoolean()) {
                    send.add(role);
                }
                if (perms.get(CommonAttributes.CONSUME_NAME).asBoolean()) {
                    consume.add(role);
                }
                if (perms.get(CommonAttributes.CREATEDURABLEQUEUE_NAME).asBoolean()) {
                    createDurableQueue.add(role);
                }
                if (perms.get(CommonAttributes.DELETEDURABLEQUEUE_NAME).asBoolean()) {
                    deleteDurableQueue.add(role);
                }
                if (perms.get(CommonAttributes.CREATE_NON_DURABLE_QUEUE_NAME).asBoolean()) {
                    createNonDurableQueue.add(role);
                }
                if (perms.get(CommonAttributes.DELETE_NON_DURABLE_QUEUE_NAME).asBoolean()) {
                    deleteNonDurableQueue.add(role);
                }
                if (perms.get(CommonAttributes.MANAGE_NAME).asBoolean()) {
                    manageRoles.add(role);
                }
            }

            writePermission(writer, SEND_NAME, send);
            writePermission(writer, CONSUME_NAME, consume);
            writePermission(writer, CREATEDURABLEQUEUE_NAME, createDurableQueue);
            writePermission(writer, DELETEDURABLEQUEUE_NAME, deleteDurableQueue);
            writePermission(writer, CREATE_NON_DURABLE_QUEUE_NAME, createNonDurableQueue);
            writePermission(writer, DELETE_NON_DURABLE_QUEUE_NAME, deleteNonDurableQueue);
            writePermission(writer, MANAGE_NAME, manageRoles);

            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writePermission(final XMLExtendedStreamWriter writer, final String type, final List<String> roles) throws XMLStreamException {
        if (roles.size() == 0) {
            return;
        }
        writer.writeStartElement(Element.PERMISSION_ELEMENT_NAME.getLocalName());
        StringBuilder sb = new StringBuilder();
        for (String role : roles) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(role);
        }
        writer.writeAttribute(Attribute.TYPE_ATTR_NAME.getLocalName(), type);
        writer.writeAttribute(Attribute.ROLES_ATTR_NAME.getLocalName(), sb.toString());
        writer.writeEndElement();
    }

    private void writeAddressSettings(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.ADDRESS_SETTINGS.getLocalName());
        for (Property matchSetting : node.asPropertyList()) {
            writer.writeStartElement(Element.ADDRESS_SETTING.getLocalName());
            writer.writeAttribute(Attribute.MATCH.getLocalName(), matchSetting.getName());
            final ModelNode setting = matchSetting.getValue();
            if (has(setting, CommonAttributes.DEAD_LETTER_ADDRESS)) {
                writeSimpleElement(writer, Element.DEAD_LETTER_ADDRESS_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.EXPIRY_ADDRESS)) {
                writeSimpleElement(writer, Element.EXPIRY_ADDRESS_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.REDELIVERY_DELAY)) {
                writeSimpleElement(writer, Element.REDELIVERY_DELAY_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.MAX_SIZE_BYTES_NODE_NAME)) {
                writeSimpleElement(writer, Element.MAX_SIZE_BYTES_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.PAGE_SIZE_BYTES_NODE_NAME)) {
                writeSimpleElement(writer, Element.PAGE_SIZE_BYTES_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.MESSAGE_COUNTER_HISTORY_DAY_LIMIT)) {
                writeSimpleElement(writer, Element.MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.ADDRESS_FULL_MESSAGE_POLICY)) {
                writeSimpleElement(writer, Element.ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.LVQ)) {
                writeSimpleElement(writer, Element.LVQ_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.MAX_DELIVERY_ATTEMPTS)) {
                writeSimpleElement(writer, Element.MAX_DELIVERY_ATTEMPTS, setting);
            }
            if (has(setting, CommonAttributes.REDISTRIBUTION_DELAY)) {
                writeSimpleElement(writer, Element.REDISTRIBUTION_DELAY_NODE_NAME, setting);
            }
            if (has(setting, CommonAttributes.SEND_TO_DLA_ON_NO_ROUTE)) {
                writeSimpleElement(writer, Element.SEND_TO_DLA_ON_NO_ROUTE, setting);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeQueues(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.CORE_QUEUES.getLocalName());
        for (Property queueProp : node.asPropertyList()) {
            writer.writeStartElement(Element.QUEUE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), queueProp.getName());
            final ModelNode queue = queueProp.getValue();
            QUEUE_ADDRESS.marshallAsElement(queue, writer);
            writeFilter(writer, queue);
            DURABLE.marshallAsElement(queue, writer);

            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeBroadcastGroups(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.BROADCAST_GROUPS.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.BROADCAST_GROUP.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                for (AttributeDefinition attribute : CommonAttributes.BROADCAST_GROUP_ATTRIBUTES) {
                    attribute.marshallAsElement(property.getValue(), writer);
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeDiscoveryGroups(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.DISCOVERY_GROUPS.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.DISCOVERY_GROUP.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                for (AttributeDefinition attribute : CommonAttributes.DISCOVERY_GROUP_ATTRIBUTES) {
                    attribute.marshallAsElement(property.getValue(), writer);
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeDiverts(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.DIVERTS.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.DIVERT.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                for (AttributeDefinition attribute : CommonAttributes.DIVERT_ATTRIBUTES) {
                    if (CommonAttributes.FILTER == attribute) {
                        writeFilter(writer, property.getValue());
                    } else {
                        attribute.marshallAsElement(property.getValue(), writer);
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeFilter(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(CommonAttributes.FILTER.getName())) {
            writer.writeEmptyElement(CommonAttributes.FILTER.getXmlName());
            writer.writeAttribute(CommonAttributes.STRING, node.get(CommonAttributes.FILTER.getName()).asString());
        }
    }

    static void writeSimpleElement(final XMLExtendedStreamWriter writer, final Element element, final ModelNode node) throws XMLStreamException {
        final String localName = element.getLocalName();
        if(node.has(localName)) {
            final String content = node.get(localName).asString();
            if(content != null) {
                writer.writeStartElement(localName);
                writer.writeCharacters(content);
                writer.writeEndElement();
            }
        }
    }

    static void writeDirectory(final XMLExtendedStreamWriter writer, final Element element, final ModelNode node) throws XMLStreamException {
        final String localName = element.getLocalName();
        if(node.has(localName)) {
            final String path = node.get(localName).has(PATH) ? node.get(localName, PATH).asString() : null;
            final String relativeTo = node.get(localName).has(RELATIVE_TO) ? node.get(localName, RELATIVE_TO).asString() : null;
            if(path != null || relativeTo != null) {
                writer.writeEmptyElement(localName);
                if(path != null) writer.writeAttribute(PATH, path);
                if(relativeTo != null) writer.writeAttribute(RELATIVE_TO, relativeTo);
            }
        }
    }


    private void writeConnectionFactories(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode factory = prop.getValue();
            if (factory.isDefined()) {
               writer.writeStartElement(Element.CONNECTION_FACTORY.getLocalName());
               writer.writeAttribute(Attribute.NAME.getLocalName(), name);
               writeConnectionFactory(writer, node, name, factory);
            }
        }
    }

    private void writePooledConnectionFactories(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode factory = prop.getValue();
            if (factory.isDefined()) {
               writer.writeStartElement(Element.POOLED_CONNECTION_FACTORY.getLocalName());
               writer.writeAttribute(Attribute.NAME.getLocalName(), name);
               writeConnectionFactory(writer, node, name, factory);
            }
        }
    }

    private void writeConnectionFactory(XMLExtendedStreamWriter writer, ModelNode node, String name, ModelNode factory) throws XMLStreamException
    {
        if (CommonAttributes.DISCOVERY_GROUP_NAME.isMarshallable(node, false)) {
            writer.writeStartElement(Element.DISCOVERY_GROUP_REF.getLocalName());
            CommonAttributes.DISCOVERY_GROUP_NAME.marshallAsAttribute(node, writer);
            writer.writeEndElement();
        }
        if (has(factory, CONNECTOR)) {
            writer.writeStartElement(Element.CONNECTORS.getLocalName());
            for (Property connProp : factory.get(CONNECTOR).asPropertyList()) {
                final ModelNode conn = connProp.getValue();
                if (conn.isDefined()) {
                    writer.writeStartElement(Element.CONNECTOR_REF.getLocalName());
                    writer.writeAttribute(Attribute.CONNECTOR_NAME.getLocalName(), connProp.getName());
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
        JndiEntriesAttribute.CONNECTION_FACTORY.marshallAsElement(factory, writer);
        if(has(factory, TRANSACTION)) {
            writer.writeStartElement(Element.TRANSACTION.getLocalName());
            writeTransactionTypeAttribute(writer, Element.MODE, factory.get(TRANSACTION));
            writer.writeEndElement();
        }
        if(has(factory, INBOUND_CONFIG)) {
            final ModelNode inboundConfigs = factory.get(INBOUND_CONFIG);
            if (inboundConfigs.getType() == ModelType.LIST) {
                writer.writeStartElement(Element.INBOUND_CONFIG.getLocalName());
                for (ModelNode config : inboundConfigs.asList()) {
                    if (config.isDefined()) {
                        CommonAttributes.USE_JNDI.marshallAsElement(config, writer);
                        CommonAttributes.JNDI_PARAMS.marshallAsElement(config, writer);
                        CommonAttributes.SETUP_ATTEMPTS.marshallAsElement(config, writer);
                        CommonAttributes.SETUP_INTERVAL.marshallAsElement(config, writer);
                        CommonAttributes.USE_LOCAL_TX.marshallAsElement(config, writer);
                    }
                }
                writer.writeEndElement();
            }
        }
        //ENTRIES

        CommonAttributes.DISCOVERY_INITIAL_WAIT_TIMEOUT.marshallAsElement(node, writer);
        CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD.marshallAsElement(node, writer);
        CommonAttributes.CONNECTION_TTL.marshallAsElement(node, writer);
        CommonAttributes.CALL_TIMEOUT.marshallAsElement(node, writer);
        CommonAttributes.CONSUMER_WINDOW_SIZE.marshallAsElement(node, writer);
        CommonAttributes.CONSUMER_MAX_RATE.marshallAsElement(node, writer);
        CommonAttributes.CONFIRMATION_WINDOW_SIZE.marshallAsElement(node, writer);
        CommonAttributes.PRODUCER_WINDOW_SIZE.marshallAsElement(node, writer);
        CommonAttributes.PRODUCER_MAX_RATE.marshallAsElement(node, writer);
        CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT.marshallAsElement(node, writer);
        CommonAttributes.MIN_LARGE_MESSAGE_SIZE.marshallAsElement(node, writer);
        CommonAttributes.CLIENT_ID.marshallAsElement(node, writer);
        CommonAttributes.DUPS_OK_BATCH_SIZE.marshallAsElement(node, writer);
        CommonAttributes.TRANSACTION_BATCH_SIZE.marshallAsElement(node, writer);
        CommonAttributes.BLOCK_ON_ACK.marshallAsElement(node, writer);
        CommonAttributes.BLOCK_ON_NON_DURABLE_SEND.marshallAsElement(node, writer);
        CommonAttributes.BLOCK_ON_DURABLE_SEND.marshallAsElement(node, writer);
        CommonAttributes.AUTO_GROUP.marshallAsElement(node, writer);
        CommonAttributes.PRE_ACK.marshallAsElement(node, writer);
        CommonAttributes.RETRY_INTERVAL_MULTIPLIER.marshallAsElement(node, writer);
        CommonAttributes.MAX_RETRY_INTERVAL.marshallAsElement(node, writer);
        CommonAttributes.CONNECTION_FACTORY_RECONNECT_ATTEMPTS.marshallAsElement(node, writer);
        CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION.marshallAsElement(node, writer);
        CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN.marshallAsElement(node, writer);
        CommonAttributes.LOAD_BALANCING_CLASS_NAME.marshallAsElement(node, writer);
        CommonAttributes.USE_GLOBAL_POOLS.marshallAsElement(node, writer);
        CommonAttributes.CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE.marshallAsElement(factory, false, writer);
        CommonAttributes.CONNECTION_THREAD_POOL_MAX_SIZE.marshallAsElement(factory, false, writer);
        CommonAttributes.GROUP_ID.marshallAsElement(node, writer);

        writer.writeEndElement();
    }

    private void writeJmsQueues(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode queue = prop.getValue();
            if (queue.isDefined()) {
                writer.writeStartElement(Element.JMS_QUEUE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                ENTRIES.marshallAsElement(queue, writer);
                DURABLE.marshallAsElement(queue, writer);
                SELECTOR.marshallAsElement(queue, writer);
                writer.writeEndElement();
            }
        }
    }

    private void writeTopics(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode topic = prop.getValue();
            if (topic.isDefined()) {
                writer.writeStartElement(Element.JMS_TOPIC.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                ENTRIES.marshallAsElement(topic, writer);
                writer.writeEndElement();
            }
        }
    }

    private void writeTransactionTypeAttribute(final XMLExtendedStreamWriter writer, final Element attr, final ModelNode value) throws XMLStreamException {
        String xaType = value.asString();
        final String txSupport;
        if(LOCAL_TX.equals(xaType)) {
            txSupport = CommonAttributes.LOCAL;
        } else if (CommonAttributes.NONE.equals(xaType)) {
             txSupport = CommonAttributes.NONE;
        } else {
            txSupport = CommonAttributes.XA;
        }
        writer.writeAttribute(attr.getLocalName(), txSupport);
    }
    private boolean has(ModelNode node, String name) {
        return node.has(name) && node.get(name).isDefined();
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }


    static void processJMSTopic(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode topic = new ModelNode();
        topic.get(OP).set(ADD);
        topic.get(OP_ADDR).set(address).add(JMS_TOPIC, name);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final Location location = reader.getLocation();
                    final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                    ENTRIES.parseAndAddParameterElement(entry, topic, location);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        updates.add(topic);
    }

    static void processJMSQueue(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        final String name = reader.getAttributeValue(0);

        final ModelNode queue = new ModelNode();
        queue.get(OP).set(ADD);
        queue.get(OP_ADDR).set(address).add(JMS_QUEUE, name);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final Location location = reader.getLocation();
                    final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                    ENTRIES.parseAndAddParameterElement(entry, queue, location);
                    break;
                } case SELECTOR: {
                    if(queue.has(SELECTOR.getName())) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.SELECTOR.getLocalName());
                    }
                    handleElementText(reader, element, queue);
                    break;
                } case DURABLE: {
                    if(queue.has(DURABLE.getName())) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.DURABLE.getLocalName());
                    }
                    handleElementText(reader, element, queue);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
       updates.add(queue);
    }

    static void processConnectionFactory(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        final String name = reader.getAttributeValue(0);

        final ModelNode connectionFactory = new ModelNode();
        connectionFactory.get(OP).set(ADD);
        connectionFactory.get(OP_ADDR).set(address).add(CONNECTION_FACTORY, name);

        updates.add(createConnectionFactory(reader, connectionFactory));
    }

    static void processPooledConnectionFactory(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode connectionFactory = new ModelNode();
        connectionFactory.get(OP).set(ADD);
        connectionFactory.get(OP_ADDR).set(address).add(POOLED_CONNECTION_FACTORY, name);

        updates.add(createConnectionFactory(reader, connectionFactory));
    }

    static ModelNode processJmsConnectors(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode connectors = new ModelNode();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case CONNECTOR_NAME: {
                        name = value.trim();
                        break;
                    } default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.CONNECTOR_NAME));
            }
            final Element element = Element.forName(reader.getLocalName());
            if(element != Element.CONNECTOR_REF) {
                throw ParseUtils.unexpectedElement(reader);
            }
            ParseUtils.requireNoContent(reader);
            final ModelNode connector = connectors.get(name).setEmptyObject();
        }
        return connectors;
    }

    private static ModelNode createConnectionFactory(XMLExtendedStreamReader reader, ModelNode connectionFactory) throws XMLStreamException
    {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case DISCOVERY_GROUP_REF: {
                    final Location location = reader.getLocation();
                    final String groupRef = readStringAttributeElement(reader, DISCOVERY_GROUP_NAME.getXmlName());
                    DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, connectionFactory, location);
                    break;
                } case CONNECTORS: {
                    connectionFactory.get(CONNECTOR).set(processJmsConnectors(reader));
                    break;
                } case ENTRIES: {
                    while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final Element local = Element.forName(reader.getLocalName());
                        if(local != Element.ENTRY ) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        final Location location = reader.getLocation();
                        final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                        JndiEntriesAttribute.CONNECTION_FACTORY.parseAndAddParameterElement(entry, connectionFactory, location);
                    }
                    break;
                } case INBOUND_CONFIG: {
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
                                throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                } case TRANSACTION: {
                    final String txType = reader.getAttributeValue(0);
                    if( txType != null) {
                        connectionFactory.get(TRANSACTION).set(txType);
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case DISCOVERY_INITIAL_WAIT_TIMEOUT:
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
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return connectionFactory;
    }

}
