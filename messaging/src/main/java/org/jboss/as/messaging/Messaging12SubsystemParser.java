package org.jboss.as.messaging;

import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.MAX_POOL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.MIN_POOL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.messaging.jms.JndiEntriesAttribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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
    protected void writePooledConnectionFactories(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            for (Property prop : properties) {
                final String name = prop.getName();
                final ModelNode factory = prop.getValue();
                if (factory.isDefined()) {
                    writer.writeStartElement(Element.POOLED_CONNECTION_FACTORY.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                    MIN_POOL_SIZE.marshallAsElement(factory, writer);
                    MAX_POOL_SIZE.marshallAsElement(factory, writer);

                    writeConnectionFactory(writer, name, factory);
                }
            }
        }
    }

    protected ModelNode createConnectionFactory(XMLExtendedStreamReader reader, ModelNode connectionFactory, boolean pooled) throws XMLStreamException
    {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case DISCOVERY_GROUP_REF: {
                    final String groupRef = readStringAttributeElement(reader, DISCOVERY_GROUP_NAME.getXmlName());
                    DISCOVERY_GROUP_NAME.parseAndSetParameter(groupRef, connectionFactory, reader);
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
                        final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                        JndiEntriesAttribute.CONNECTION_FACTORY.parseAndAddParameterElement(entry, connectionFactory, reader);
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
                case HA:
                case CONNECTION_FACTORY_TYPE:
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
                case MAX_POOL_SIZE:
                case MIN_POOL_SIZE:
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
                case USER:
                    // Element name is overloaded, handleElementText can not be used, we must use the correct attribute
                    CommonAttributes.PCF_USER.parseAndSetParameter(reader.getElementText(), connectionFactory, reader);
                    break;
                case PASSWORD:
                    // Element name is overloaded, handleElementText can not be used, we must use the correct attribute
                    CommonAttributes.PCF_PASSWORD.parseAndSetParameter(reader.getElementText(), connectionFactory, reader);
                    break;
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        return connectionFactory;
    }

}
