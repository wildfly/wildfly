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

package org.jboss.as.messaging.deployment;

import java.util.Collections;
import java.util.EnumSet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.messaging.Attribute;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;

/**
 * The messaging subsystem domain parser
 *
 * @author scott.stark@jboss.org
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingDeploymentParser_1_0 implements XMLStreamConstants, XMLElementReader<ParseResult> {

    static final MessagingDeploymentParser_1_0 INSTANCE = new MessagingDeploymentParser_1_0();


    private static final EnumSet<Element> SIMPLE_ROOT_RESOURCE_ELEMENTS = EnumSet.noneOf(Element.class);

    static {
        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            SIMPLE_ROOT_RESOURCE_ELEMENTS.add(Element.forName(attr.getXmlName()));
        }
    }

    private MessagingDeploymentParser_1_0() {
        //
    }


    public void readElement(final XMLExtendedStreamReader reader, final ParseResult result) throws XMLStreamException {

        final Namespace schemaVer = Namespace.forUri(reader.getNamespaceURI());
        switch (schemaVer) {
            case MESSAGING_DEPLOYMENT_1_0:
                processHornetQServer(reader, result);
                break;
            default:
                throw unexpectedElement(reader);
        }

    }

    private void processHornetQServer(final XMLExtendedStreamReader reader, final ParseResult result) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case HORNETQ_SERVER:
                    processHornetQ(reader, result);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void processHornetQ(final XMLExtendedStreamReader reader, final ParseResult result) throws XMLStreamException {
        String hqServerName = null;

        final int count = reader.getAttributeCount();
        if (count > 0) {
            requireSingleAttribute(reader, Attribute.NAME.getLocalName());
            hqServerName = reader.getAttributeValue(0).trim();
        }

        if (hqServerName == null || hqServerName.length() == 0) {
            hqServerName = "default";
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case JMS_DESTINATIONS:
                    processJmsDestinations(reader, result, hqServerName);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    static void processJmsDestinations(final XMLExtendedStreamReader reader, final ParseResult result, final String hqServerName) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case JMS_QUEUE:
                    processJMSQueue(reader, hqServerName, result);
                    break;
                case JMS_TOPIC:
                    processJMSTopic(reader, hqServerName, result);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    static void processJMSTopic(final XMLExtendedStreamReader reader, String hqServer, ParseResult result) throws XMLStreamException {

        final String name = reader.getAttributeValue(0);
        if (name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode topic = new ModelNode();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ENTRY: {
                    final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                    ENTRIES.parseAndAddParameterElement(entry, topic, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        result.getTopics().add(new JmsDestination(topic, hqServer, name));
    }

    static void processJMSQueue(final XMLExtendedStreamReader reader, String hqServer, ParseResult result) throws XMLStreamException {

        requireSingleAttribute(reader, CommonAttributes.NAME);
        final String name = reader.getAttributeValue(0);

        if (name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }

        final ModelNode queue = new ModelNode();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ENTRY: {
                    final String entry = readStringAttributeElement(reader, CommonAttributes.NAME);
                    ENTRIES.parseAndAddParameterElement(entry, queue, reader);
                    break;
                }
                case SELECTOR: {
                    if (queue.has(SELECTOR.getName())) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.SELECTOR.getLocalName());
                    }
                    SELECTOR.parseAndSetParameter(reader.getElementText(), queue, reader);
                    break;
                }
                case DURABLE: {
                    if (queue.has(DURABLE.getName())) {
                        throw ParseUtils.duplicateNamedElement(reader, Element.DURABLE.getLocalName());
                    }
                    DURABLE.parseAndSetParameter(reader.getElementText(), queue, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        result.getQueues().add(new JmsDestination(queue, hqServer, name));
    }
}
