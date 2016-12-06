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

import static java.util.Arrays.asList;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.messaging.CommonAttributes.DEFAULT;
import static org.jboss.as.messaging.CommonAttributes.JMS_BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_INCOMING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;
import static org.jboss.as.messaging.Element.DISCOVERY_GROUP_REF;
import static org.jboss.as.messaging.Element.STATIC_CONNECTORS;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;


/**
 * Messaging subsystem 1.3 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging13SubsystemParser extends Messaging12SubsystemParser {

    protected Messaging13SubsystemParser() {
    }

    @Override
    protected void checkClusterConnectionConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        // AS7-5598 relax constraints on the cluster-connection to accept one without static-connectors or discovery-group-ref
        // however it is still not valid to have both
        checkNotBothElements(reader, seen, STATIC_CONNECTORS, DISCOVERY_GROUP_REF);
    }

    @Override
    protected void checkBroadcastGroupConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        checkNotBothElements(reader, seen, Element.SOCKET_BINDING, Element.JGROUPS_STACK);
        checkNotBothElements(reader, seen, Element.JGROUPS_STACK, Element.GROUP_ADDRESS);
        checkNotBothElements(reader, seen, Element.GROUP_ADDRESS, Element.SOCKET_BINDING);
        if (seen.contains(Element.GROUP_ADDRESS) && !seen.contains(Element.GROUP_PORT)) {
            throw missingRequired(reader, EnumSet.of(Element.GROUP_PORT));
        }
    }

    @Override
    protected void checkDiscoveryGroupConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        checkNotBothElements(reader, seen, Element.SOCKET_BINDING, Element.JGROUPS_STACK);
        checkNotBothElements(reader, seen, Element.JGROUPS_STACK, Element.GROUP_ADDRESS);
        checkNotBothElements(reader, seen, Element.GROUP_ADDRESS, Element.SOCKET_BINDING);
        if (seen.contains(Element.GROUP_ADDRESS) && !seen.contains(Element.GROUP_PORT)) {
            throw missingRequired(reader, EnumSet.of(Element.GROUP_PORT));
        }
    }

    protected void handleUnknownConnectionFactoryAttribute(XMLExtendedStreamReader reader, Element element, ModelNode connectionFactory, boolean pooled)
            throws XMLStreamException {
        switch (element) {
            case CALL_FAILOVER_TIMEOUT:
            case COMPRESS_LARGE_MESSAGES:
                handleElementText(reader, element, connectionFactory);
                break;
            case USE_AUTO_RECOVERY:
            case INITIAL_MESSAGE_PACKET_SIZE:
                if (!pooled) {
                    throw unexpectedElement(reader);
                }
                handleElementText(reader, element, connectionFactory);
                break;
            case INITIAL_CONNECT_ATTEMPTS:
                if (!pooled) {
                    throw unexpectedElement(reader);
                }
                handleElementText(reader, element, "pooled", connectionFactory);
                break;
            default: {
                super.handleUnknownConnectionFactoryAttribute(reader, element, connectionFactory, pooled);
            }
        }
    }

    @Override
    protected void handleUnknownClusterConnectionAttribute(XMLExtendedStreamReader reader, Element element, ModelNode clusterConnectionAdd)
            throws XMLStreamException {
        switch (element) {
            case CALL_FAILOVER_TIMEOUT:
            case NOTIFICATION_ATTEMPTS:
            case NOTIFICATION_INTERVAL:
                handleElementText(reader, element, clusterConnectionAdd);
                break;
            default: {
                super.handleUnknownClusterConnectionAttribute(reader, element, clusterConnectionAdd);
            }
        }
    }

    @Override
    protected void handleUnknownConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch (element) {
            case CHECK_FOR_LIVE_SERVER:
            case BACKUP_GROUP_NAME:
            case REPLICATION_CLUSTERNAME:
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownConfigurationAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void handleComplexConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch (element) {
            case REMOTING_INCOMING_INTERCEPTORS:
                processRemotingIncomingInterceptors(reader, operation);
                break;
            case REMOTING_OUTGOING_INTERCEPTORS:
                processRemotingOutgoingInterceptors(reader, operation);
                break;
            default: {
                super.handleComplexConfigurationAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void handleUnknownBroadcastGroupAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation)
            throws XMLStreamException {
        switch (element) {
            case JGROUPS_STACK:
            case JGROUPS_CHANNEL:
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownBroadcastGroupAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void handleUnknownDiscoveryGroupAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation)
            throws XMLStreamException {
        switch (element) {
            case JGROUPS_STACK:
            case JGROUPS_CHANNEL:
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownDiscoveryGroupAttribute(reader, element, operation);
            }
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

    private void processRemotingIncomingInterceptors(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CLASS_NAME: {
                    final String value = reader.getElementText();
                    REMOTING_INCOMING_INTERCEPTORS.parseAndAddParameterElement(value, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void processRemotingOutgoingInterceptors(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        requireNoAttributes(reader);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CLASS_NAME: {
                    final String value = reader.getElementText();
                    REMOTING_OUTGOING_INTERCEPTORS.parseAndAddParameterElement(value, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
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

    /**
     * [AS7-5808] Support space-separated roles names for backwards compatibility and comma-separated ones for compatibility with
     * HornetQ configuration.
     *
     * Roles are persisted using space character delimiter in {@link MessagingXMLWriter}.
     */
    @Override
    protected List<String> parseRolesAttribute(XMLExtendedStreamReader reader, int index) throws XMLStreamException {
        String roles = reader.getAttributeValue(index);
        return asList(roles.split("[,\\s]+"));
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

    /**
     * Check that not both elements have been defined
     */
    protected static void checkNotBothElements(XMLExtendedStreamReader reader, Set<Element> seen, Element element1, Element element2) throws XMLStreamException {
        if (seen.contains(element1) && seen.contains(element2)) {
            throw new XMLStreamException(MessagingLogger.ROOT_LOGGER.onlyOneRequired(element1.getLocalName(), element2.getLocalName()), reader.getLocation());
        }
    }
}
