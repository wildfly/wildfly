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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTOR_BACKUP_NAME;
import static org.jboss.as.messaging.jms.CommonAttributes.DISCOVERY_GROUP_REF;
import static org.jboss.as.messaging.jms.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.jms.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.jms.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.jms.CommonAttributes.SELECTOR;
import static org.jboss.as.messaging.jms.CommonAttributes.TOPIC;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class NewJMSSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    private static final NewJMSSubsystemParser INSTANCE = new NewJMSSubsystemParser();

    public static NewJMSSubsystemParser getInstance() {
        return INSTANCE;
    }

    private NewJMSSubsystemParser() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> updates) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, NewJMSExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        updates.add(operation);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case CONNECTION_FACTORY: {
                    processConnectionFactory(reader, address, updates);
                    break;
                } case QUEUE: {
                    processJMSQueue(reader, address, updates);
                    break;
                } case TOPIC: {
                    processJMSTopic(reader, address, updates);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static void processJMSTopic(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode topic = new ModelNode();
        topic.get(OP).set(ADD);
        topic.get(OP_ADDR).set(address).add(TOPIC, name);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final String entry = reader.getAttributeValue(0);
                    topic.get(ENTRIES).add(entry.trim());
                    ParseUtils.requireNoContent(reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        updates.add(topic);

    }

    static void processJMSQueue(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode queue = new ModelNode();
        queue.get(OP).set(ADD);
        queue.get(OP_ADDR).set(address).add(QUEUE, name);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case ENTRY: {
                    final String entry = reader.getAttributeValue(0);
                    queue.get(ENTRIES).add(entry.trim());
                    ParseUtils.requireNoContent(reader);
                    break;
                } case SELECTOR: {
                    if(queue.has(SELECTOR)) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.SELECTOR.getLocalName());
                    }
                    queue.get(SELECTOR).set(reader.getElementText().trim());
                    break;
                } case DURABLE: {
                    if(queue.has(DURABLE)) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.DURABLE.getLocalName());
                    }
                    queue.get(DURABLE).set(Boolean.valueOf(reader.getElementText()));
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        updates.add(queue);
    }

    static void processConnectionFactory(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        final String name = reader.getAttributeValue(0);
        if(name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode connectionFactory = new ModelNode();
        connectionFactory.get(OP).set(ADD);
        connectionFactory.get(OP_ADDR).set(address).add(CONNECTION_FACTORY, name);

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element) {
                case DISCOVERY_GROUP_REF: {
                    final String groupRef = reader.getAttributeValue(0);
                    if(groupRef != null) {
                        connectionFactory.get(DISCOVERY_GROUP_REF).add(groupRef);
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                } case CONNECTORS: {
                    connectionFactory.get(CONNECTOR).set(processConnectors(reader));
                    break;
                } case ENTRIES: {
                    while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final Element local = Element.forName(reader.getLocalName());
                        if(local != Element.ENTRY ) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        final String entry = reader.getAttributeValue(0);
                        connectionFactory.get(ENTRIES).add(entry.trim());
                        ParseUtils.requireNoContent(reader);
                    }
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
                case RETRY_INTERVAL:
                case RETRY_INTERVAL_MULTIPLIER:
                case MAX_RETRY_INTERVAL:
                case RECONNECT_ATTEMPTS:
                case FAILOVER_ON_INITIAL_CONNECTION:
                case FAILOVER_ON_SERVER_SHUTDOWN:
                case LOAD_BALANCING_CLASS_NAME:
                case USE_GLOBAL_POOLS:
                case SCHEDULED_THREAD_POOL_MAX_SIZE:
                case THREAD_POOL_MAX_SIZE:
                case GROUP_ID:
                    parseElementText(reader, element, connectionFactory);
                    break;
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        updates.add(connectionFactory);
    }

    static ModelNode processConnectors(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode connectors = new ModelNode();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String name = null;
            String backup = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case CONNECTOR_NAME: {
                        name = value.trim();
                        break;
                    } case CONNECTOR_BACKUP_NAME: {
                        backup = value.trim();
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
            if(backup != null) {
                connector.get(CONNECTOR_BACKUP_NAME).set(backup);
            }
        }
        return connectors;
    }

    static void parseElementText(final XMLExtendedStreamReader reader, final Element element, final ModelNode node) throws XMLStreamException {
        final String value = reader.getElementText();
        if(value != null && value.length() > 0) {
            node.get(element.getLocalName()).set(value.trim());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        final ModelNode node = context.getModelNode();
        if (node.has(CONNECTION_FACTORY)) {
            writeConnectionFactories(writer, node.get(CONNECTION_FACTORY));
        }
        if (node.has(QUEUE)) {
            writeQueues(writer, node.get(QUEUE));
        }
        if (node.has(TOPIC)) {
            writeTopics(writer, node.get(TOPIC));
        }
        writer.writeEndElement();
    }

    private void writeConnectionFactories(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode factory = prop.getValue();
            if (factory.isDefined()) {
                writer.writeStartElement(Element.CONNECTION_FACTORY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), name);

                if (has(factory, CommonAttributes.DISCOVERY_GROUP_REF)) {
                    writer.writeStartElement(Element.DISCOVERY_GROUP_REF.getLocalName());
                    writeAttribute(writer, Attribute.DISCOVERY_GROUP_NAME, factory.get(DISCOVERY_GROUP_REF));
                    writer.writeEndElement();
                }
                if (has(factory, CONNECTOR)) {
                    writer.writeStartElement(Element.CONNECTORS.getLocalName());
                    for (Property connProp : factory.get(CONNECTOR).asPropertyList()) {
                        final ModelNode conn = connProp.getValue();
                        if (conn.isDefined()) {
                            writer.writeStartElement(Element.CONNECTOR_REF.getLocalName());
                            writer.writeAttribute(Attribute.CONNECTOR_NAME.getLocalName(), connProp.getName());
                            if (has(conn, CONNECTOR_BACKUP_NAME)) {
                                writeAttribute(writer, Attribute.CONNECTOR_BACKUP_NAME, conn.get(CONNECTOR_BACKUP_NAME));
                            }
                            writer.writeEndElement();
                        }
                    }
                    writer.writeEndElement();
                }
                if (has(factory, ENTRIES)) {
                    final ModelNode entries = factory.get(ENTRIES);
                    if (entries.getType() == ModelType.LIST) {
                        writer.writeStartElement(Element.ENTRIES.getLocalName());
                        for (ModelNode entry : entries.asList()) {
                            if (entry.isDefined()) {
                                writer.writeStartElement(Element.ENTRY.getLocalName());
                                writeAttribute(writer, Attribute.NAME, entry);
                                writer.writeEndElement();
                            }
                        }
                        writer.writeEndElement();
                    }
                }
                //ENTRIES

                if (has(factory, CommonAttributes.DISCOVERY_INITIAL_WAIT_TIMEOUT)){
                    writeSimpleElement(writer, Element.DISCOVERY_INITIAL_WAIT_TIMEOUT, node);
                }
                if (has(factory, CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD)){
                    writeSimpleElement(writer, Element.DISCOVERY_INITIAL_WAIT_TIMEOUT, node);
                }
                if (has(factory, CommonAttributes.CONNECTION_TTL)){
                    writeSimpleElement(writer, Element.CONNECTION_TTL, node);
                }
                if (has(factory, CommonAttributes.CALL_TIMEOUT)){
                    writeSimpleElement(writer, Element.CALL_TIMEOUT, node);
                }
                if (has(factory, CommonAttributes.CONSUMER_WINDOW_SIZE)){
                    writeSimpleElement(writer, Element.CONSUMER_WINDOW_SIZE, node);
                }
                if (has(factory, CommonAttributes.CONSUMER_MAX_RATE)){
                    writeSimpleElement(writer, Element.CONSUMER_MAX_RATE, node);
                }
                if (has(factory, CommonAttributes.CONFIRMATION_WINDOW_SIZE)){
                    writeSimpleElement(writer, Element.CONFIRMATION_WINDOW_SIZE, node);
                }
                if (has(factory, CommonAttributes.PRODUCER_WINDOW_SIZE)){
                    writeSimpleElement(writer, Element.PRODUCER_WINDOW_SIZE, node);
                }
                if (has(factory, CommonAttributes.PRODUCER_MAX_RATE)){
                    writeSimpleElement(writer, Element.PRODUCER_MAX_RATE, node);
                }
                if (has(factory, CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT)){
                    writeSimpleElement(writer, Element.CACHE_LARGE_MESSAGE_CLIENT, node);
                }
                if (has(factory, CommonAttributes.MIN_LARGE_MESSAGE_SIZE)){
                    writeSimpleElement(writer, Element.MIN_LARGE_MESSAGE_SIZE, node);
                }
                if (has(factory, CommonAttributes.CLIENT_ID)){
                    writeSimpleElement(writer, Element.CLIENT_ID, node);
                }
                if (has(factory, CommonAttributes.DUPS_OK_BATCH_SIZE)){
                    writeSimpleElement(writer, Element.DUPS_OK_BATCH_SIZE, node);
                }
                if (has(factory, CommonAttributes.TRANSACTION_BATCH_SIZE)){
                    writeSimpleElement(writer, Element.TRANSACTION_BATH_SIZE, node);
                }
                if (has(factory, CommonAttributes.BLOCK_ON_ACK)){
                    writeSimpleElement(writer, Element.BLOCK_ON_ACK, node);
                }
                if (has(factory, CommonAttributes.BLOCK_ON_NON_DURABLE_SEND)){
                    writeSimpleElement(writer, Element.BLOCK_ON_NON_DURABLE_SEND, node);
                }
                if (has(factory, CommonAttributes.BLOCK_ON_DURABLE_SEND)){
                    writeSimpleElement(writer, Element.BLOCK_ON_DURABLE_SEND, node);
                }
                if (has(factory, CommonAttributes.AUTO_GROUP)){
                    writeSimpleElement(writer, Element.AUTO_GROUP, node);
                }
                if (has(factory, CommonAttributes.PRE_ACK)){
                    writeSimpleElement(writer, Element.PRE_ACK, node);
                }
                if (has(factory, CommonAttributes.RETRY_INTERVAL_MULTIPLIER)){
                    writeSimpleElement(writer, Element.RETRY_INTERVAL_MULTIPLIER, node);
                }
                if (has(factory, CommonAttributes.MAX_RETRY_INTERVAL)){
                    writeSimpleElement(writer, Element.MAX_RETRY_INTERVAL, node);
                }
                if (has(factory, CommonAttributes.RECONNECT_ATTEMPTS)){
                    writeSimpleElement(writer, Element.RECONNECT_ATTEMPTS, node);
                }
                if (has(factory, CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION)){
                    writeSimpleElement(writer, Element.FAILOVER_ON_INITIAL_CONNECTION, node);
                }
                if (has(factory, CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN)){
                    writeSimpleElement(writer, Element.FAILOVER_ON_SERVER_SHUTDOWN, node);
                }
                if (has(factory, CommonAttributes.LOAD_BALANCING_CLASS_NAME)){
                    writeSimpleElement(writer, Element.LOAD_BALANCING_CLASS_NAME, node);
                }
                if (has(factory, CommonAttributes.USE_GLOBAL_POOLS)){
                    writeSimpleElement(writer, Element.USE_GLOBAL_POOLS, node);
                }
                if (has(factory, CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE)){
                    writeSimpleElement(writer, Element.SCHEDULED_THREAD_POOL_MAX_SIZE, node);
                }
                if (has(factory, CommonAttributes.THREAD_POOL_MAX_SIZE)){
                    writeSimpleElement(writer, Element.THREAD_POOL_MAX_SIZE, node);
                }
                if (has(factory, CommonAttributes.GROUP_ID)){
                    writeSimpleElement(writer, Element.GROUP_ID, node);
                }

                writer.writeEndElement();
            }
        }
    }

    private void writeQueues(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode queue = prop.getValue();
            if (queue.isDefined()) {
                writer.writeStartElement(Element.QUEUE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                if (queue.has(CommonAttributes.ENTRIES)) {
                    final ModelNode entries = queue.get(ENTRIES);
                    if (entries.getType() == ModelType.LIST) {
                        for (ModelNode entry : entries.asList()) {
                            if (entry.isDefined()) {
                                writer.writeStartElement(Element.ENTRY.getLocalName());
                                writeAttribute(writer, Attribute.NAME, entry);
                                writer.writeEndElement();
                            }
                        }
                    }
                }
                if (has(queue, DURABLE)) {
                    writeSimpleElement(writer, Element.DURABLE, queue);
                }
                if (has(queue, SELECTOR)) {
                    writeSimpleElement(writer, Element.SELECTOR, queue);
                }
                writer.writeEndElement();
            }
        }
    }

    private void writeTopics(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        for (Property prop : node.asPropertyList()) {
            final String name = prop.getName();
            final ModelNode topic = prop.getValue();
            if (topic.isDefined()) {
                writer.writeStartElement(Element.TOPIC.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                if (topic.has(CommonAttributes.ENTRIES)) {
                    final ModelNode entries = topic.get(ENTRIES);
                    if (entries.getType() == ModelType.LIST) {
                        for (ModelNode entry : entries.asList()) {
                            if (entry.isDefined()) {
                                writer.writeStartElement(Element.ENTRY.getLocalName());
                                writeAttribute(writer, Attribute.NAME, entry);
                                writer.writeEndElement();
                            }
                        }
                    }
                }
                writer.writeEndElement();
            }
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

    private boolean has(ModelNode node, String name) {
        return node.has(name) && node.get(name).isDefined();
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }

}
