package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTORS;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_REF;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.MAX_POOL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.MIN_POOL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.STATIC_CONNECTORS;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.messaging.jms.JndiEntriesAttribute;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;


public class Messaging12SubsystemParser extends MessagingSubsystemParser {

    private static final Messaging12SubsystemParser INSTANCE = new Messaging12SubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    protected Messaging12SubsystemParser() {
    }

    @Override
    public Namespace getExpectedNamespace() {
        return Namespace.MESSAGING_1_2;
    }

    @Override
    protected void writePooledConnectionFactoryAttributes(XMLExtendedStreamWriter writer, String name, ModelNode factory)
            throws XMLStreamException {
        super.writePooledConnectionFactoryAttributes(writer, name, factory);

        MIN_POOL_SIZE.marshallAsElement(factory, writer);
        MAX_POOL_SIZE.marshallAsElement(factory, writer);
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
                case CHECK_PERIOD:
                case CONNECTION_TTL:
                case QUEUE_NAME:
                case HA:
                case TRANSFORMER_CLASS_NAME:
                case MIN_LARGE_MESSAGE_SIZE:
                case RETRY_INTERVAL_MULTIPLIER:
                case MAX_RETRY_INTERVAL:
                case FAILOVER_ON_SERVER_SHUTDOWN:
                case CONFIRMATION_WINDOW_SIZE:
                case USER:
                case PASSWORD:
                    handleElementText(reader, element, bridgeAdd);
                    break;
                case FILTER:  {
                    String string = readStringAttributeElement(reader, CommonAttributes.STRING);
                    FILTER.parseAndSetParameter(string, bridgeAdd, reader);
                    break;
                }
                case RETRY_INTERVAL:
                    // Use the "default" variant
                    handleElementText(reader, element, "default", bridgeAdd);
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
                    final String groupRef = readStringAttributeElement(reader, DISCOVERY_GROUP_NAME.getXmlName());
                    DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, bridgeAdd, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        checkOnlyOneOfElements(reader, seen, Element.STATIC_CONNECTORS, Element.DISCOVERY_GROUP_REF);

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

        updates.add(bridgeAdd);
    }

    protected void processClusterConnection(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> updates) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        String name = reader.getAttributeValue(0);

        ModelNode clusterConnectionAdd = getEmptyOperation(ADD, address.clone().add(CommonAttributes.CLUSTER_CONNECTION, name));

        EnumSet<Element> required = EnumSet.of(Element.ADDRESS, Element.CONNECTOR_REF);
        Set<Element> seen = EnumSet.noneOf(Element.class);
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!seen.add(element)) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }
            required.remove(element);
            switch (element) {
                case CALL_TIMEOUT:
                case CONFIRMATION_WINDOW_SIZE:
                case FORWARD_WHEN_NO_CONSUMERS:
                case MAX_HOPS:
                case MIN_LARGE_MESSAGE_SIZE:
                    handleElementText(reader, element, clusterConnectionAdd);
                    break;
                case ADDRESS:  {
                    handleElementText(reader, element, CommonAttributes.CLUSTER_CONNECTION_ADDRESS.getName(), clusterConnectionAdd);
                    break;
                }
                case CONNECTOR_REF:  {
                    // Use the "simple" variant
                    handleElementText(reader, element, "simple", clusterConnectionAdd);
                    break;
                }
                case CHECK_PERIOD:
                case CONNECTION_TTL:
                case MAX_RETRY_INTERVAL:
                case RECONNECT_ATTEMPTS:
                case RETRY_INTERVAL:
                case RETRY_INTERVAL_MULTIPLIER:
                case USE_DUPLICATE_DETECTION:
                    // Use the "cluster" variant
                    handleElementText(reader, element, "cluster", clusterConnectionAdd);
                    break;
                case STATIC_CONNECTORS:
                    if (seen.contains(Element.DISCOVERY_GROUP_REF)) {
                        throw new XMLStreamException(MESSAGES.illegalElement(STATIC_CONNECTORS, DISCOVERY_GROUP_REF), reader.getLocation());
                    }
                    processStaticConnectors(reader, clusterConnectionAdd, true);
                    break;
                case DISCOVERY_GROUP_REF: {
                    if (seen.contains(Element.STATIC_CONNECTORS)) {
                        throw new XMLStreamException(MESSAGES.illegalElement(DISCOVERY_GROUP_REF, STATIC_CONNECTORS), reader.getLocation());
                    }
                    final String groupRef = readStringAttributeElement(reader, DISCOVERY_GROUP_NAME.getXmlName());
                    DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, clusterConnectionAdd, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        checkOnlyOneOfElements(reader, seen, Element.STATIC_CONNECTORS, Element.DISCOVERY_GROUP_REF);

        if(!required.isEmpty()) {
            missingRequired(reader, required);
        }

        updates.add(clusterConnectionAdd);
    }
}
