package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.messaging.CommonAttributes.FILTER;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;


public class Messaging12SubsystemParser extends MessagingSubsystemParser {

    private static final Messaging12SubsystemParser INSTANCE = new Messaging12SubsystemParser();

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    protected Messaging12SubsystemParser() {
    }

    @Override
    protected void handleUnknownConnectionFactoryAttribute(XMLExtendedStreamReader reader, Element element, ModelNode connectionFactory, boolean pooled) throws XMLStreamException {
        switch (element) {
            case MAX_POOL_SIZE:
            case MIN_POOL_SIZE:
                if (!pooled) {
                    throw unexpectedElement(reader);
                }
                handleElementText(reader, element, connectionFactory);
                break;
            default: {
                super.handleUnknownConnectionFactoryAttribute(reader, element, connectionFactory, pooled);
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
                case CHECK_PERIOD:
                case CONNECTION_TTL:
                case QUEUE_NAME:
                case HA:
                case TRANSFORMER_CLASS_NAME:
                case RETRY_INTERVAL_MULTIPLIER:
                case MAX_RETRY_INTERVAL:
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
                case MIN_LARGE_MESSAGE_SIZE:
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
            missingRequired(reader, required);
        }

        updates.add(bridgeAdd);
    }

    @Override
    protected void handleUnknownClusterConnectionAttribute(XMLExtendedStreamReader reader, Element element, ModelNode clusterConnectionAdd)
            throws XMLStreamException {
        switch (element) {
            case CALL_TIMEOUT:
            case MIN_LARGE_MESSAGE_SIZE:
                handleElementText(reader, element, clusterConnectionAdd);
                break;
            case CHECK_PERIOD:
            case CONNECTION_TTL:
            case MAX_RETRY_INTERVAL:
            case RECONNECT_ATTEMPTS:
            case RETRY_INTERVAL_MULTIPLIER:
                // Use the "cluster" variant
                handleElementText(reader, element, "cluster", clusterConnectionAdd);
                break;

            default: {
                super.handleUnknownClusterConnectionAttribute(reader, element, clusterConnectionAdd);
            }
        }
    }

    @Override
    protected void checkClusterConnectionConstraints(XMLExtendedStreamReader reader, Set<Element> seen) throws XMLStreamException {
        checkOnlyOneOfElements(reader, seen, Element.STATIC_CONNECTORS, Element.DISCOVERY_GROUP_REF);
    }
}
