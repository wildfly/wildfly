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

package org.wildfly.extension.messaging.activemq.deployment;

import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ENTRY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_DESTINATIONS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_QUEUE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_TOPIC;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SELECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER;

import java.util.Collections;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.messaging.activemq.CommonAttributes;


/**
 * The messaging subsystem domain parser
 *
 * @author scott.stark@jboss.org
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */

class MessagingDeploymentParser_1_0 implements XMLStreamConstants, XMLElementReader<ParseResult> {

    private final PropertyReplacer propertyReplacer;

    MessagingDeploymentParser_1_0(final PropertyReplacer propertyReplacer) {
        //
        this.propertyReplacer = propertyReplacer;
    }


    public void readElement(final XMLExtendedStreamReader reader, final ParseResult result) throws XMLStreamException {

        final Namespace schemaVer = Namespace.forUri(reader.getNamespaceURI());
        switch (schemaVer) {
            case MESSAGING_DEPLOYMENT_1_0:
                processDeployment(reader, result);
                break;
            default:
                throw unexpectedElement(reader);
        }

    }

    private void processDeployment(final XMLExtendedStreamReader reader, final ParseResult result) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (reader.getLocalName()) {
                case SERVER:
                    processServer(reader, result);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void processServer(final XMLExtendedStreamReader reader, final ParseResult result) throws XMLStreamException {
        String serverName = null;

        final int count = reader.getAttributeCount();
        if (count > 0) {
            requireSingleAttribute(reader, CommonAttributes.NAME);
            serverName = propertyReplacer.replaceProperties(reader.getAttributeValue(0).trim());
        }

        if (serverName == null || serverName.length() == 0) {
            serverName = "default";
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (reader.getLocalName()) {
                case JMS_DESTINATIONS:
                    processJmsDestinations(reader, result, serverName);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void processJmsDestinations(final XMLExtendedStreamReader reader, final ParseResult result, final String serverName) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (reader.getLocalName()) {
                case JMS_QUEUE:
                    processJMSQueue(reader, serverName, result);
                    break;
                case JMS_TOPIC:
                    processJMSTopic(reader, serverName, result);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void processJMSTopic(final XMLExtendedStreamReader reader, String serverName, ParseResult result) throws XMLStreamException {

        final String name = propertyReplacer.replaceProperties(reader.getAttributeValue(0));
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode topic = new ModelNode();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (reader.getLocalName()) {
                case ENTRY: {
                    final String entry = propertyReplacer.replaceProperties(readStringAttributeElement(reader, CommonAttributes.NAME));
                    CommonAttributes.DESTINATION_ENTRIES.parseAndAddParameterElement(entry, topic, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        result.getTopics().add(new JmsDestination(topic, serverName, name));
    }

    private void processJMSQueue(final XMLExtendedStreamReader reader, String serverName, ParseResult result) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        final String name = propertyReplacer.replaceProperties(reader.getAttributeValue(0));

        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode queue = new ModelNode();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (reader.getLocalName()) {
                case ENTRY: {
                    final String entry = propertyReplacer.replaceProperties(readStringAttributeElement(reader, CommonAttributes.NAME));
                    CommonAttributes.DESTINATION_ENTRIES.parseAndAddParameterElement(entry, queue, reader);
                    break;
                }
                case "selector": {
                    if (queue.has(SELECTOR.getName())) {
                        throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                    }
                    requireSingleAttribute(reader, CommonAttributes.STRING);
                    final String selector = propertyReplacer.replaceProperties(readStringAttributeElement(reader, CommonAttributes.STRING));
                    SELECTOR.parseAndSetParameter(selector, queue, reader);
                    break;
                }
                case "durable": {
                    if (queue.has(DURABLE.getName())) {
                        throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                    }
                    DURABLE.parseAndSetParameter(propertyReplacer.replaceProperties(reader.getElementText()), queue, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        result.getQueues().add(new JmsDestination(queue, serverName, name));
    }
}
