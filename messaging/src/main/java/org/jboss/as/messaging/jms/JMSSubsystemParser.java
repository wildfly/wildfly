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

package org.jboss.as.messaging.jms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ExtensionContext;
import org.jboss.as.ExtensionContext.SubsystemConfiguration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The JMS subsystem configuration parser.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSSubsystemParser implements XMLStreamConstants, XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<JMSSubsystemElement>>> {

    private static final JMSSubsystemParser INSTANCE = new JMSSubsystemParser();

    static JMSSubsystemParser getInstance() {
        return INSTANCE;
    }

    private JMSSubsystemParser() {
        //
    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, ParseResult<SubsystemConfiguration<JMSSubsystemElement>> result) throws XMLStreamException {

        final JMSSubsystemAdd subsystemAdd = new JMSSubsystemAdd();
        final List<AbstractSubsystemUpdate<JMSSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<JMSSubsystemElement,?>>();

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case CONNECTION_FACTORY: {
                    processConnectionFactory(reader, updates);
                    break;
                } case QUEUE: {
                    processJMSQueue(reader, updates);
                    break;
                } case TOPIC: {
                    processJMSTopic(reader, updates);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        result.setResult(new SubsystemConfiguration<JMSSubsystemElement>(subsystemAdd, updates));
    }

    static void processConnectionFactory(final XMLExtendedStreamReader reader, List<AbstractSubsystemUpdate<JMSSubsystemElement, ?>> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }
        final ConnectionFactoryElement cf = new ConnectionFactoryElement(name);
        final Set<String> bindings = new HashSet<String>();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case DISCOVERY_GROUP_REF: {
                    final String groupRef = reader.getAttributeValue(0);
                    if(groupRef != null) {
                        cf.setDiscoveryGroupName(groupRef.trim());
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                } case DISCOVERY_INITIAL_WAIT_TIMEOUT: {
                    cf.setInitialWaitTimeout(textAsLong(reader));
                    break;
                } case CONNECTORS: {
                    List<ConnectionFactoryConnectorRef> connectors = processConnectors(reader);
                    if(! connectors.isEmpty()) {
                        cf.setConnectorRef(connectors);
                    }
                    break;
                } case ENTRIES: {
                    while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final Element local = Element.forName(reader.getLocalName());
                        if(local != Element.ENTRY ) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        final String entry = reader.getAttributeValue(0);
                        bindings.add(entry.trim());
                        ParseUtils.requireNoContent(reader);
                    }
                    break;
                } case CLIENT_FAILURE_CHECK_PERIOD: {
                    cf.setClientFailureCheckPeriod(textAsLong(reader));
                    break;
                } case CONNECTION_TTL: {
                    cf.setConnectionTTL(textAsLong(reader));
                    break;
                } case CALL_TIMEOUT: {
                    cf.setCallTimeout(textAsLong(reader));
                    break;
                } case CONSUMER_WINDOW_SIZE: {
                    cf.setConsumerWindowSize(textAsInt(reader));
                    break;
                } case CONSUMER_MAX_RATE: {
                    cf.setConsumerMaxRate(textAsInt(reader));
                    break;
                } case CONFIRMATION_WINDOW_SIZE: {
                    cf.setConfirmationWindowSize(textAsInt(reader));
                    break;
                } case PRODUCER_WINDOW_SIZE: {
                    cf.setProducerWindowSize(textAsInt(reader));
                    break;
                } case PRODUCER_MAX_RATE: {
                    cf.setProducerMaxRate(textAsInt(reader));
                    break;
                } case CACHE_LARGE_MESSAGE_CLIENT: {
                    cf.setCacheLargeMessagesClient(textAsBoolean(reader));
                    break;
                } case MIN_LARGE_MESSAGE_SIZE: {
                    cf.setMinLargeMessageSize(textAsInt(reader));
                    break;
                } case CLIENT_ID: {
                    cf.setClientID(elementText(reader));
                    break;
                } case DUPS_OK_BATCH_SIZE: {
                    cf.setDupsOKBatchSize(textAsInt(reader));
                    break;
                } case TRANSACTION_BATH_SIZE: {
                    cf.setTransactionBatchSize(textAsInt(reader));
                    break;
                } case BLOCK_ON_ACK: {
                    cf.setBlockOnAcknowledge(textAsBoolean(reader));
                    break;
                } case BLOCK_ON_NON_DURABLE_SEND: {
                    cf.setBlockOnNonDurableSend(textAsBoolean(reader));
                    break;
                } case BLOCK_ON_DURABLE_SEND: {
                    cf.setBlockOnDurableSend(textAsBoolean(reader));
                    break;
                } case AUTO_GROUP: {
                    cf.setAutoGroup(textAsBoolean(reader));
                    break;
                } case PRE_ACK: {
                    cf.setPreAcknowledge(textAsBoolean(reader));
                    break;
                } case RETRY_INTERVAL: {
                    cf.setRetryInterval(textAsLong(reader));
                    break;
                } case RETRY_INTERVAL_MULTIPLIER: {
                    cf.setRetryIntervalMultiplier(Double.valueOf(elementText(reader)));
                    break;
                } case MAX_RETRY_INTERVAL: {
                    cf.setMaxRetryInterval(textAsLong(reader));
                    break;
                } case RECONNECT_ATTEMPTS: {
                    cf.setReconnectAttempts(textAsInt(reader));
                    break;
                } case FAILOVER_ON_INITIAL_CONNECTION: {
                    cf.setFailoverOnInitialConnection(textAsBoolean(reader));
                    break;
                } case FAILOVER_ON_SERVER_SHUTDOWN: {
                    cf.setFailoverOnServerShutdown(textAsBoolean(reader));
                    break;
                } case LOAD_BALANCING_CLASS_NAME: {
                    cf.setLoadBalancingPolicyClassName(elementText(reader));
                    break;
                } case USE_GLOBAL_POOLS: {
                    cf.setUseGlobalPools(textAsBoolean(reader));
                    break;
                } case SCHEDULED_THREAD_POOL_MAX_SIZE: {
                    cf.setScheduledThreadPoolMaxSize(textAsInt(reader));
                    break;
                } case THREAD_POOL_MAX_SIZE: {
                    cf.setThreadPoolMaxSize(textAsInt(reader));
                    break;
                } case GROUP_ID: {
                    cf.setGroupID(elementText(reader));
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        cf.setBindings(bindings);
        updates.add(new ConnectionFactoryAdd(cf));
    }

    static void processJMSQueue(final XMLExtendedStreamReader reader, List<AbstractSubsystemUpdate<JMSSubsystemElement, ?>> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }
        final Set<String> bindings = new HashSet<String>();
        String selector = null;
        Boolean durable = null;
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final String entry = reader.getAttributeValue(0);
                    bindings.add(entry.trim());
                    ParseUtils.requireNoContent(reader);
                    break;
                } case SELECTOR: {
                    if(selector != null) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.SELECTOR.getLocalName());
                    }
                    selector = reader.getElementText().trim();
                    break;
                } case DURABLE: {
                    if(durable != null) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.DURABLE.getLocalName());
                    }
                    durable = Boolean.valueOf(reader.getElementText());
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        final JMSQueueAdd action = new JMSQueueAdd(name);
        action.setSelector(selector);
        action.setBindings(bindings);
        action.setDurable(durable);

        updates.add(action);
    }

    static void processJMSTopic(final XMLExtendedStreamReader reader, List<AbstractSubsystemUpdate<JMSSubsystemElement, ?>> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }
        final Set<String> bindings = new HashSet<String>();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final String entry = reader.getAttributeValue(0);
                    bindings.add(entry.trim());
                    ParseUtils.requireNoContent(reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        final JMSTopicAdd action = new JMSTopicAdd(name);
        action.setBindings(bindings);
        updates.add(action);
    }

    static List<ConnectionFactoryConnectorRef> processConnectors(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final List<ConnectionFactoryConnectorRef> connectors = new ArrayList<ConnectionFactoryConnectorRef>();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String connector = null;
            String backup = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case CONNECTOR_NAME: {
                        connector = value.trim();
                        break;
                    } case CONNECTOR_BACKUP_NAME: {
                        backup = value.trim();
                        break;
                    } default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if(connector == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.CONNECTOR_NAME));
            }
            final Element element = Element.forName(reader.getLocalName());
            if(element != Element.CONNECTOR_REF) {
                throw ParseUtils.unexpectedElement(reader);
            }
            ParseUtils.requireNoContent(reader);
            final ConnectionFactoryConnectorRef ref = new ConnectionFactoryConnectorRef();
            ref.setConnectorName(connector);
            if(backup != null) {
                ref.setBackupName(backup);
            }
            connectors.add(ref);
        }
        return connectors;
    }

    static Boolean textAsBoolean(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return Boolean.valueOf(elementText(reader));
    }

    static Integer textAsInt(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return Integer.valueOf(elementText(reader));
    }

    static Long textAsLong(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return Long.valueOf(elementText(reader));
    }

    static String elementText(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return reader.getElementText().trim();
    }
}
