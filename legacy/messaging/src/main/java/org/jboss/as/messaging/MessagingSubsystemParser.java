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
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.parsePossibleExpression;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedEndElement;
import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.DEFAULT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.ROLE;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;
import static org.jboss.as.messaging.CommonAttributes.SUBSYSTEM;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The messaging subsystem domain parser
 *
 * @author scott.stark@jboss.org
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {


    private static final EnumSet<Element> SIMPLE_ROOT_RESOURCE_ELEMENTS = EnumSet.noneOf(Element.class);

    static {
        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            SIMPLE_ROOT_RESOURCE_ELEMENTS.add(Element.forName(attr.getXmlName()));
        }
    }

    protected MessagingSubsystemParser() {
        //
    }


    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(address);
        list.add(subsystemAdd);

        final Namespace schemaVer = Namespace.forUri(reader.getNamespaceURI());
        switch (schemaVer) {
            case MESSAGING_1_0:
                processHornetQServer(reader, address, list, schemaVer);
                break;
            case MESSAGING_1_1:
            case MESSAGING_1_2:
            case MESSAGING_1_3:
            case MESSAGING_1_4:
            case MESSAGING_1_5:
            case MESSAGING_2_0:
            case MESSAGING_3_0:
                processHornetQServers(reader, address, list);
                break;
            default:
                throw unexpectedElement(reader);
        }

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
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }
    }

    protected void processHornetQServer(final XMLExtendedStreamReader reader, final ModelNode subsystemAddress, final List<ModelNode> list, Namespace namespace) throws XMLStreamException {

        String hqServerName = null;
        String elementName = null;
        switch (namespace) {
            case MESSAGING_1_0:
                // We're parsing the 1.0 xsd's <subsystem> element
                requireNoAttributes(reader);
                elementName = ModelDescriptionConstants.SUBSYSTEM;
                break;
            default: {
                final int count = reader.getAttributeCount();
                if (count > 0) {
                    requireSingleAttribute(reader, Attribute.NAME.getLocalName());
                    hqServerName = reader.getAttributeValue(0).trim();
                }
                elementName = CommonAttributes.HORNETQ_SERVER;
            }
        }

        if (hqServerName == null || hqServerName.length() == 0) {
            hqServerName = DEFAULT;
        }

        final ModelNode address = subsystemAddress.clone();
        address.add(HORNETQ_SERVER, hqServerName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        list.add(operation);

        EnumSet<Element> seen = EnumSet.noneOf(Element.class);
        // Handle elements
        String localName = null;
        do {
            reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());
            if (!seen.add(element)) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }

            switch (element) {
                case ACCEPTORS:
                    processAcceptors(reader, address, list);
                    break;
                case ADDRESS_SETTINGS:
                    processAddressSettings(reader, address, list);
                    break;
                case BINDINGS_DIRECTORY:
                    parseDirectory(reader, CommonAttributes.BINDINGS_DIRECTORY, address, list);
                    break;
                case BRIDGES:
                    processBridges(reader, address, list);
                    break;
                case BROADCAST_GROUPS:
                    processBroadcastGroups(reader, address, list);
                    break;
                case CLUSTER_CONNECTIONS:
                    processClusterConnections(reader, address, list);
                    break;
                case CONNECTORS:
                    processConnectors(reader, address, list);
                    break;
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
                case JOURNAL_DIRECTORY:
                    parseDirectory(reader, CommonAttributes.JOURNAL_DIRECTORY, address, list);
                    break;
                case LARGE_MESSAGES_DIRECTORY:
                    parseDirectory(reader, CommonAttributes.LARGE_MESSAGES_DIRECTORY, address, list);
                    break;
                case LIVE_CONNECTOR_REF: {
                    MessagingLogger.ROOT_LOGGER.deprecatedXMLElement(element.toString());
                    skipElementText(reader);
                    break;
                }
                case PAGING_DIRECTORY:
                    parseDirectory(reader, CommonAttributes.PAGING_DIRECTORY, address, list);
                    break;
                case REMOTING_INTERCEPTORS:
                    processRemotingInterceptors(reader, operation);
                    break;
                case SECURITY_DOMAIN:
                    handleElementText(reader, element, null, operation);
                    break;
                case SECURITY_SETTINGS: {
                    // process security settings
                    processSecuritySettings(reader, address, list);
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
                case MESSAGE_COUNTER_ENABLED: {
                    MessagingLogger.ROOT_LOGGER.deprecatedXMLElement(element.toString(), Element.STATISTICS_ENABLED.toString());
                    handleElementText(reader, element, operation);
                    break;
                }
                case HORNETQ_SERVER:
                    // The end of the hornetq-server element
                    if (namespace == Namespace.MESSAGING_1_0) {
                        throw unexpectedEndElement(reader);
                    }
                    break;
                case SUBSYSTEM:
                    // The end of the subsystem element
                    if (namespace != Namespace.MESSAGING_1_0) {
                        throw unexpectedEndElement(reader);
                    }
                    break;
                case CLUSTERED:
                    // log that the attribute is deprecated but handle it anyway
                    MessagingLogger.ROOT_LOGGER.deprecatedXMLElement(element.toString());
                default:
                    if (SIMPLE_ROOT_RESOURCE_ELEMENTS.contains(element)) {
                        AttributeDefinition attributeDefinition = element.getDefinition();
                        if (attributeDefinition instanceof SimpleAttributeDefinition) {
                            handleElementText(reader, element, operation);
                        } else {
                            // These should be handled in specific case blocks above, e.g. case REMOTING_INTERCEPTORS:
                            handleComplexConfigurationAttribute(reader, element, operation);
                        }
                    } else {
                        handleUnknownConfigurationAttribute(reader, element, operation);
                    }
            }
        } while (reader.hasNext() && localName.equals(elementName) == false);
    }

    protected void handleComplexConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        throw MessagingLogger.ROOT_LOGGER.unsupportedElement(element.getLocalName());
    }

    protected void handleUnknownConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation)
            throws XMLStreamException {
        throw ParseUtils.unexpectedElement(reader);
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
                    String[] attrs = ParseUtils.requireAttributes(reader, Attribute.KEY.getLocalName(), Attribute.VALUE.getLocalName());
                    final String key = attrs[0];
                    final String value = attrs[1];
                    final ModelNode paramAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, serviceAddress.clone().add(CommonAttributes.PARAM, key));
                    ConnectorServiceParamDefinition.VALUE.parseAndSetParameter(value, paramAdd, reader);
                    updates.add(paramAdd);
                    requireNoContent(reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }


        if(!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

    }

    private void processClusterConnections(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
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

    protected void processClusterConnection(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        ModelNode clusterConnectionAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.CLUSTER_CONNECTION, name));

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
                    handleElementText(reader, element, clusterConnectionAdd);
                    break;
                case ADDRESS:  {
                    handleElementText(reader, element, ClusterConnectionDefinition.ADDRESS.getName(), clusterConnectionAdd);
                    break;
                }
                case CONNECTOR_REF:  {
                    // Use the "simple" variant
                    handleElementText(reader, element, "simple", clusterConnectionAdd);
                    break;
                }
                case CONFIRMATION_WINDOW_SIZE:
                    handleElementText(reader, element, "bridge", clusterConnectionAdd);
                    break;
                case USE_DUPLICATE_DETECTION:
                case RETRY_INTERVAL:
                    // Use the "cluster" variant
                    handleElementText(reader, element, "cluster", clusterConnectionAdd);
                    break;
                case STATIC_CONNECTORS:
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.STATIC_CONNECTORS, Element.DISCOVERY_GROUP_REF);
                    processStaticConnectors(reader, clusterConnectionAdd, true);
                    break;
                case DISCOVERY_GROUP_REF: {
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.DISCOVERY_GROUP_REF, Element.STATIC_CONNECTORS);
                    final String groupRef = readStringAttributeElement(reader, ClusterConnectionDefinition.DISCOVERY_GROUP_NAME.getXmlName());
                    ClusterConnectionDefinition.DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, clusterConnectionAdd, reader);
                    break;
                }
                default: {
                    handleUnknownClusterConnectionAttribute(reader, element, clusterConnectionAdd);
                }
            }
        }

        if(!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        checkClusterConnectionConstraints(reader, seen);

        updates.add(clusterConnectionAdd);
    }

    protected void handleUnknownClusterConnectionAttribute(XMLExtendedStreamReader reader, Element element, ModelNode clusterConnectionAdd)
            throws XMLStreamException {
        throw ParseUtils.unexpectedElement(reader);
    }

    protected void checkClusterConnectionConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
    }

    protected void checkBroadcastGroupConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        checkOnlyOneOfElements(reader, seen, Element.GROUP_ADDRESS, Element.SOCKET_BINDING);
        if (seen.contains(Element.GROUP_ADDRESS) && !seen.contains(Element.GROUP_PORT)) {
            throw missingRequired(reader, EnumSet.of(Element.GROUP_PORT));
        }
    }

    private void processBridges(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
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

    protected void processBridge(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

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
                case USER:
                case PASSWORD:
                    handleElementText(reader, element, bridgeAdd);
                    break;
                case CONFIRMATION_WINDOW_SIZE:
                    handleElementText(reader, element, "bridge", bridgeAdd);
                    break;
                case FILTER:  {
                    String string = readStringAttributeElement(reader, CommonAttributes.STRING);
                    FILTER.parseAndSetParameter(string, bridgeAdd, reader);
                    break;
                }
                case RETRY_INTERVAL_MULTIPLIER:
                case RETRY_INTERVAL:
                    // Use the "default" variant
                    handleElementText(reader, element, DEFAULT, bridgeAdd);
                    break;
                case FORWARDING_ADDRESS:
                case RECONNECT_ATTEMPTS:
                case USE_DUPLICATE_DETECTION:
                    handleElementText(reader, element, "bridge", bridgeAdd);
                    break;
                case STATIC_CONNECTORS:
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.STATIC_CONNECTORS, Element.DISCOVERY_GROUP_REF);
                    processStaticConnectors(reader, bridgeAdd, false);
                    break;
                case DISCOVERY_GROUP_REF: {
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.DISCOVERY_GROUP_REF, Element.STATIC_CONNECTORS);
                    final String groupRef = readStringAttributeElement(reader, BridgeDefinition.DISCOVERY_GROUP_NAME.getXmlName());
                    BridgeDefinition.DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, bridgeAdd, reader);
                    break;
                }
                case FAILOVER_ON_SERVER_SHUTDOWN: {
                    MessagingLogger.ROOT_LOGGER.deprecatedXMLElement(element.toString());
                    skipElementText(reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        checkOnlyOneOfElements(reader, seen, Element.STATIC_CONNECTORS, Element.DISCOVERY_GROUP_REF);

        if(!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        updates.add(bridgeAdd);
    }

    protected void processStaticConnectors(XMLExtendedStreamReader reader, ModelNode addOperation, boolean cluster) throws XMLStreamException {

        if (cluster) {

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ALLOW_DIRECT_CONNECTIONS_ONLY: {
                        final String attrValue = reader.getAttributeValue(i);
                        ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY.parseAndSetParameter(attrValue, addOperation, reader);
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
            throw missingRequired(reader, required);
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
                    handleElementText(reader, element, GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS.getName(), groupingHandlerAdd);
                    break;
                }
                default: {
                    handleUnknownGroupingHandlerAttribute(reader, element, groupingHandlerAdd);
                }
            }
        }

        if(!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        updates.add(groupingHandlerAdd);
    }

    protected void handleUnknownGroupingHandlerAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation)
            throws XMLStreamException {
        throw ParseUtils.unexpectedElement(reader);
    }

    private void processRemotingInterceptors(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CLASS_NAME: {
                    final String value = reader.getElementText();
                    REMOTING_INTERCEPTORS.parseAndAddParameterElement(value, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    void processBroadcastGroups(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
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

    protected void parseBroadcastGroup(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

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
        if(name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        ModelNode broadcastGroupAdd = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.BROADCAST_GROUP, name));

        Set<Element> seen = EnumSet.noneOf(Element.class);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            seen.add(element);
            switch (element) {
                case LOCAL_BIND_ADDRESS:
                case LOCAL_BIND_PORT:
                case GROUP_ADDRESS:
                case GROUP_PORT:
                case SOCKET_BINDING:
                case BROADCAST_PERIOD:
                    handleElementText(reader, element, broadcastGroupAdd);
                    break;
                case CONNECTOR_REF:
                    handleElementText(reader, element, "broadcast-group", broadcastGroupAdd);
                    break;
                default: {
                    handleUnknownBroadcastGroupAttribute(reader, element, broadcastGroupAdd);
                }
            }
        }

        checkBroadcastGroupConstraints(reader, seen);

        updates.add(broadcastGroupAdd);
    }

    protected void handleUnknownBroadcastGroupAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation)
            throws XMLStreamException {
        throw ParseUtils.unexpectedElement(reader);
    }

    void processDiscoveryGroups(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
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

    protected void parseDiscoveryGroup(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

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
        if(name == null) {
            throw  missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        ModelNode discoveryGroup = org.jboss.as.controller.operations.common.Util.getEmptyOperation(ADD, address.clone().add(CommonAttributes.DISCOVERY_GROUP, name));

        Set<Element> seen = EnumSet.noneOf(Element.class);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            seen.add(element);
            switch (element) {
                case LOCAL_BIND_ADDRESS:
                case GROUP_ADDRESS:
                case GROUP_PORT:
                case REFRESH_TIMEOUT:
                case SOCKET_BINDING:
                case INITIAL_WAIT_TIMEOUT:
                    handleElementText(reader, element, discoveryGroup);
                    break;
                default: {
                    handleUnknownDiscoveryGroupAttribute(reader, element, discoveryGroup);
                }
            }
        }

        checkDiscoveryGroupConstraints(reader, seen);

        updates.add(discoveryGroup);
    }

    protected void handleUnknownDiscoveryGroupAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation)
            throws XMLStreamException {
        throw ParseUtils.unexpectedElement(reader);
    }

    protected void checkDiscoveryGroupConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        checkOnlyOneOfElements(reader, seen, Element.GROUP_ADDRESS, Element.SOCKET_BINDING);
        if (seen.contains(Element.GROUP_ADDRESS) && !seen.contains(Element.GROUP_PORT)) {
            throw missingRequired(reader, EnumSet.of(Element.GROUP_PORT));
        }
    }

    void processConnectionFactories(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
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

   void processAcceptors(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            String serverId = null;

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
                        serverId = attrValue;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            final ModelNode acceptorAddress = address.clone();
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);

            boolean generic = false;
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ACCEPTOR: {
                    operation.get(OP_ADDR).set(acceptorAddress.add(ACCEPTOR, name));
                    if(socketBinding != null) {
                        operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    }
                    generic = true;
                    break;
                } case NETTY_ACCEPTOR: {
                    operation.get(OP_ADDR).set(acceptorAddress.add(REMOTE_ACCEPTOR, name));
                    if(socketBinding == null) {
                        throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SOCKET_BINDING));
                    }
                    operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    break;
                } case IN_VM_ACCEPTOR: {
                    operation.get(OP_ADDR).set(acceptorAddress.add(IN_VM_ACCEPTOR, name));
                    if (serverId != null) {
                        InVMTransportDefinition.SERVER_ID.parseAndSetParameter(serverId, operation, reader);
                    }
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }

            updates.add(operation);
            parseTransportConfiguration(reader, operation, generic, updates);
        }
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
                    if(! op.hasDefined(QueueDefinition.ADDRESS.getName())) {
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
                    handleElementText(reader, element, QueueDefinition.ADDRESS.getName(), queue);
                    break;
                } case FILTER: {
                    String string = readStringAttributeElement(reader, CommonAttributes.STRING);
                    FILTER.parseAndSetParameter(string, queue, reader);
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

    private ModelNode processSecuritySettings(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> operations) throws XMLStreamException {
        final ModelNode security = new ModelNode();
        String localName = null;
        do {
            reader.nextTag();
            localName = reader.getLocalName();
            final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());

            switch (element) {
                case SECURITY_SETTING:
                    final String match = reader.getAttributeValue(0);

                    final ModelNode addr = address.clone();
                    addr.add(SECURITY_SETTING, match);
                    final ModelNode operation = new ModelNode();
                    operation.get(OP).set(ADD);
                    operation.get(OP_ADDR).set(addr);
                    operations.add(operation);

                    parseSecurityRoles(reader, addr, operations);
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.SECURITY_SETTING.getLocalName()));
        return security;
    }

    private void parseSecurityRoles(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> operations) throws XMLStreamException {

        final Map<String, Set<AttributeDefinition>> permsByRole = new HashMap<String, Set<AttributeDefinition>>();
        String localName = null;
        do {
            reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (element != Element.PERMISSION_ELEMENT_NAME) {
                break;
            }

            final Set<Attribute> required = EnumSet.of(Attribute.ROLES_ATTR_NAME, Attribute.TYPE_ATTR_NAME);
            List<String> roles = null;
            AttributeDefinition perm = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                requireNoNamespaceAttribute(reader, i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case ROLES_ATTR_NAME:
                        roles = parseRolesAttribute(reader, i);
                        break;
                    case TYPE_ATTR_NAME:
                        perm = SecurityRoleDefinition.ROLE_ATTRIBUTES_BY_XML_NAME.get(reader.getAttributeValue(i));
                        if (perm == null) {
                            throw ControllerLogger.ROOT_LOGGER.invalidAttributeValue(reader.getAttributeValue(i), reader.getAttributeName(i), SecurityRoleDefinition.ROLE_ATTRIBUTES_BY_XML_NAME.keySet(), reader.getLocation());
                        }
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }

            if (!required.isEmpty()) {
                throw missingRequired(reader, required);
            }

            for (String role : roles) {
                role = role.trim();
                Set<AttributeDefinition> perms = permsByRole.get(role);
                if (perms == null) {
                    perms = new HashSet<AttributeDefinition>();
                    permsByRole.put(role, perms);
                }
                perms.add(perm);
            }
            // Scan to element end
            reader.discardRemainder();
        } while (reader.hasNext());

        for (Map.Entry<String, Set<AttributeDefinition>> entry : permsByRole.entrySet()) {
            final String role = entry.getKey();
            final Set<AttributeDefinition> perms = entry.getValue();

            final ModelNode addr = address.clone();
            addr.add(ROLE, role);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(addr);

            for (AttributeDefinition perm : SecurityRoleDefinition.ATTRIBUTES) {
                operation.get(perm.getName()).set(perms.contains(perm));
            }

            operations.add(operation);
        }
    }

    protected List<String> parseRolesAttribute(final XMLExtendedStreamReader reader, int index) throws XMLStreamException {
        return reader.getListAttributeValue(index);
    }

     void processConnectors(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String socketBinding = null;
            String serverId = null;

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
                        serverId = attrValue;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            final ModelNode connectorAddress = address.clone();
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);

            boolean generic = false;
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTOR: {
                    operation.get(OP_ADDR).set(connectorAddress.add(CONNECTOR, name));
                    if(socketBinding != null) {
                        operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    }
                    generic = true;
                    break;
                } case NETTY_CONNECTOR: {
                    operation.get(OP_ADDR).set(connectorAddress.add(REMOTE_CONNECTOR, name));
                    if(socketBinding == null) {
                        throw missingRequired(reader, Collections.singleton(Attribute.SOCKET_BINDING));
                    }
                    operation.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).set(socketBinding);
                    break;
                } case IN_VM_CONNECTOR: {
                    operation.get(OP_ADDR).set(connectorAddress.add(IN_VM_CONNECTOR, name));
                    if (serverId != null) {
                        InVMTransportDefinition.SERVER_ID.parseAndSetParameter(serverId, operation, reader);
                    }
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }

            updates.add(operation);
            parseTransportConfiguration(reader, operation, generic, updates);
        }
    }

    protected void processAddressSettings(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> operations) throws XMLStreamException {
        String localName = null;
        do {
            reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            switch (element) {
                case ADDRESS_SETTING:
                    // Add address settings
                    final String match = reader.getAttributeValue(0);
                    final ModelNode operation = parseAddressSettings(reader);
                    operation.get(OP).set(ADD);
                    operation.get(OP_ADDR).set(address);
                    operation.get(OP_ADDR).add(CommonAttributes.ADDRESS_SETTING, match);

                    operations.add(operation);
                    break;
            }
        } while (reader.hasNext() && localName.equals(Element.ADDRESS_SETTING.getLocalName()));
    }

    protected ModelNode parseAddressSettings(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode addressSettingsSpec = new ModelNode();

        String localName;
        do {
            reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case DEAD_LETTER_ADDRESS:
                case EXPIRY_ADDRESS:
                case REDELIVERY_DELAY:
                case MAX_SIZE_BYTES:
                case PAGE_MAX_CACHE_SIZE:
                case PAGE_SIZE_BYTES:
                case MESSAGE_COUNTER_HISTORY_DAY_LIMIT:
                case ADDRESS_FULL_MESSAGE_POLICY:
                case LVQ:
                case MAX_DELIVERY_ATTEMPTS:
                case REDISTRIBUTION_DELAY:
                case SEND_TO_DLA_ON_NO_ROUTE: {
                    handleElementText(reader, element, addressSettingsSpec);
                    break;
                } default: {
                    handleUnknownAddressSetting(reader, element, addressSettingsSpec);
                    break;
                }
            }
        } while (!reader.getLocalName().equals(Element.ADDRESS_SETTING.getLocalName()) && reader.getEventType() == XMLExtendedStreamReader.END_ELEMENT);

        return addressSettingsSpec;
    }

    protected void handleUnknownAddressSetting(XMLExtendedStreamReader reader, Element element, ModelNode addressSettingsAdd)
            throws XMLStreamException {
        // do nothing
    }

    void parseTransportConfiguration(final XMLExtendedStreamReader reader, final ModelNode operation, final boolean generic, List<ModelNode> updates) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case FACTORY_CLASS: {
                    if(! generic) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    handleElementText(reader, element, operation);
                    break;
                }
                case PARAM: {
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
                    ModelNode addParam = new ModelNode();
                    addParam.get(OP).set(ADD);
                    ModelNode transportAddress = operation.get(OP_ADDR).clone();
                    addParam.get(OP_ADDR).set(transportAddress.add(CommonAttributes.PARAM, key));
                    TransportParamDefinition.VALUE.parseAndSetParameter(value, addParam, reader);
                    updates.add(addParam);
                    ParseUtils.requireNoContent(reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static void parseDirectory(final XMLExtendedStreamReader reader, final String name, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        ModelNode path = null;
        String relativeTo = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            final String value = reader.getAttributeValue(i);
            switch (attribute) {
                case RELATIVE_TO:
                    relativeTo = value;
                    break;
                case PATH:
                    path = PathDefinition.PATHS.get(name).parse(value, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if(path == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PATH));
        }
        requireNoContent(reader);
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get(OP_ADDR).add(ModelDescriptionConstants.PATH, name);
        operation.get(ModelDescriptionConstants.PATH).set(path);
        if(relativeTo != null) operation.get(ModelDescriptionConstants.RELATIVE_TO).set(relativeTo);

        updates.add(operation);
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
                    handleElementText(reader, element, DivertDefinition.ADDRESS.getName(), divertAdd);
                    break;
                }
                case FORWARDING_ADDRESS: {
                    handleElementText(reader, element, "divert", divertAdd);
                    break;
                }
                case FILTER: {
                    String string = readStringAttributeElement(reader, CommonAttributes.STRING);
                    FILTER.parseAndSetParameter(string, divertAdd, reader);
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
            throw missingRequired(reader, required);
        }

        list.add(divertAdd);
    }

    static void unhandledElement(XMLExtendedStreamReader reader, Element element) throws XMLStreamException {
        throw MessagingLogger.ROOT_LOGGER.ignoringUnhandledElement(element, reader.getLocation().toString());
    }

    static void handleElementText(final XMLExtendedStreamReader reader, final Element element, final ModelNode node) throws XMLStreamException {
        handleElementText(reader, element, null, node);
    }

    static void handleElementText(final XMLExtendedStreamReader reader, final Element element, final String modelName, final ModelNode node) throws XMLStreamException {
        AttributeDefinition attributeDefinition = modelName == null ? element.getDefinition() : element.getDefinition(modelName);
        if (attributeDefinition != null) {
            final String value = reader.getElementText();
            if (attributeDefinition instanceof SimpleAttributeDefinition) {
                ((SimpleAttributeDefinition) attributeDefinition).parseAndSetParameter(value, node, reader);
            } else if (attributeDefinition instanceof ListAttributeDefinition) {
                ((ListAttributeDefinition) attributeDefinition).parseAndAddParameterElement(value, node, reader);
            }
        } else {
            handleElementText(reader, element, node, ModelType.STRING, true, false);
        }
    }

    static void skipElementText(final XMLExtendedStreamReader reader) throws XMLStreamException {
        reader.getElementText();
    }

    /** @deprecated use AttributeDefinition */
    @Deprecated
    static void handleElementText(final XMLExtendedStreamReader reader, final Element element, final ModelNode node, final ModelType expectedType,
                                  final boolean allowNull, final boolean allowExpression) throws XMLStreamException {
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
                            throw new XMLStreamException(MessagingLogger.ROOT_LOGGER.illegalValue(value, element.getLocalName()), reader.getLocation());
                    }
                } catch (IllegalArgumentException iae) {
                    throw new XMLStreamException(MessagingLogger.ROOT_LOGGER.illegalValue(value, element.getLocalName(), expectedType), reader.getLocation());
                }
            }
        } else if (!allowNull) {
            throw new XMLStreamException(MessagingLogger.ROOT_LOGGER.illegalValue(value, element.getLocalName()), reader.getLocation());
        }
    }

    static void processJMSTopic(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        final String name = reader.getAttributeValue(0);
        if(name == null) {
            throw missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode topic = new ModelNode();
        topic.get(OP).set(ADD);
        topic.get(OP_ADDR).set(address).add(JMS_TOPIC, name);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                    CommonAttributes.DESTINATION_ENTRIES.parseAndAddParameterElement(entry, topic, reader);
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
                    final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                    CommonAttributes.DESTINATION_ENTRIES.parseAndAddParameterElement(entry, queue, reader);
                    break;
                } case SELECTOR: {
                    if(queue.has(SELECTOR.getName())) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.SELECTOR.getLocalName());
                    }
                    requireSingleAttribute(reader, CommonAttributes.STRING);
                    final String selector = readStringAttributeElement(reader, CommonAttributes.STRING);
                    SELECTOR.parseAndSetParameter(selector, queue, reader);
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

    void processConnectionFactory(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        final String name = reader.getAttributeValue(0);

        final ModelNode connectionFactory = new ModelNode();
        connectionFactory.get(OP).set(ADD);
        connectionFactory.get(OP_ADDR).set(address).add(CONNECTION_FACTORY, name);

        updates.add(createConnectionFactory(reader, connectionFactory, false));
    }

    void processPooledConnectionFactory(final XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            throw missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode connectionFactory = new ModelNode();
        connectionFactory.get(OP).set(ADD);
        connectionFactory.get(OP_ADDR).set(address).add(POOLED_CONNECTION_FACTORY, name);

        updates.add(createConnectionFactory(reader, connectionFactory, true));
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
                    } case BACKUP_CONNECTOR_NAME: {
                        MessagingLogger.ROOT_LOGGER.deprecatedXMLAttribute(attribute.toString());
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

            // create the connector node
            connectors.get(name);
        }
        return connectors;
    }

    protected ModelNode createConnectionFactory(XMLExtendedStreamReader reader, ModelNode connectionFactory, boolean pooled) throws XMLStreamException {
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
                    final String groupRef = readStringAttributeElement(reader, Common.DISCOVERY_GROUP_NAME.getXmlName());
                    Common.DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, connectionFactory, reader);
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
                        Common.ENTRIES.parseAndAddParameterElement(entry, connectionFactory, reader);
                    }
                    break;
                }
                case HA:
                case CLIENT_FAILURE_CHECK_PERIOD:
                case CALL_TIMEOUT:
                case CONSUMER_WINDOW_SIZE:
                case CONSUMER_MAX_RATE:
                case PRODUCER_WINDOW_SIZE:
                case PRODUCER_MAX_RATE:
                case CACHE_LARGE_MESSAGE_CLIENT:
                case CLIENT_ID:
                case DUPS_OK_BATCH_SIZE:
                case TRANSACTION_BATH_SIZE:
                case BLOCK_ON_ACK:
                case BLOCK_ON_NON_DURABLE_SEND:
                case BLOCK_ON_DURABLE_SEND:
                case AUTO_GROUP:
                case PRE_ACK:
                case FAILOVER_ON_INITIAL_CONNECTION:
                case FAILOVER_ON_SERVER_SHUTDOWN:
                case LOAD_BALANCING_CLASS_NAME:
                case USE_GLOBAL_POOLS:
                case GROUP_ID:
                    handleElementText(reader, element, connectionFactory);
                    break;
                case CONFIRMATION_WINDOW_SIZE:
                case CONNECTION_TTL:
                case MAX_RETRY_INTERVAL:
                case MIN_LARGE_MESSAGE_SIZE:
                case RECONNECT_ATTEMPTS:
                case RETRY_INTERVAL:
                case RETRY_INTERVAL_MULTIPLIER:
                case SCHEDULED_THREAD_POOL_MAX_SIZE:
                case THREAD_POOL_MAX_SIZE:
                    // Use the "connection" variant
                    handleElementText(reader, element, "connection", connectionFactory);
                    break;
                case DISCOVERY_INITIAL_WAIT_TIMEOUT:
                    MessagingLogger.ROOT_LOGGER.deprecatedXMLElement(element.toString());
                    skipElementText(reader);
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
                        connectionFactory.get(Pooled.TRANSACTION.getName()).set(txType);
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                case USER:
                    if(!pooled) {
                        throw unexpectedElement(reader);
                    }
                    // Element name is overloaded, handleElementText can not be used, we must use the correct attribute
                    Pooled.USER.parseAndSetParameter(reader.getElementText(), connectionFactory, reader);
                    break;
                case PASSWORD:
                    if(!pooled) {
                        throw unexpectedElement(reader);
                    }
                    // Element name is overloaded, handleElementText can not be used, we must use the correct attribute
                    Pooled.PASSWORD.parseAndSetParameter(reader.getElementText(), connectionFactory, reader);
                    break;
                // end of pooled CF elements
                // =========================================================
                default: {
                    handleUnknownConnectionFactoryAttribute(reader, element, connectionFactory, pooled);
                }
            }
        }

        checkOnlyOneOfElements(reader, seen, Element.CONNECTORS, Element.DISCOVERY_GROUP_REF);

        return connectionFactory;
    }

    protected void handleUnknownConnectionFactoryAttribute(XMLExtendedStreamReader reader, Element element, ModelNode connectionFactory, boolean pooled) throws XMLStreamException {
        throw ParseUtils.unexpectedElement(reader);
    }

    protected void checkOtherElementIsNotAlreadyDefined(XMLStreamReader reader, Set<Element> seen, Element currentElement, Element otherElement) throws XMLStreamException {
        if (seen.contains(otherElement)) {
            throw new XMLStreamException(MessagingLogger.ROOT_LOGGER.illegalElement(currentElement.getLocalName(), otherElement.getLocalName()), reader.getLocation());
        }
    }

    /**
     * Check one and only one of the 2 elements has been defined
     */
    protected static void checkOnlyOneOfElements(XMLExtendedStreamReader reader, Set<Element> seen, Element element1, Element element2) throws XMLStreamException {
        if (!seen.contains(element1) && !seen.contains(element2)) {
            throw new XMLStreamException(MessagingLogger.ROOT_LOGGER.required(element1.getLocalName(), element2.getLocalName()), reader.getLocation());
        }
        if (seen.contains(element1) && seen.contains(element2)) {
            throw new XMLStreamException(MessagingLogger.ROOT_LOGGER.onlyOneRequired(element1.getLocalName(), element2.getLocalName()), reader.getLocation());
        }
    }
}
