/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.structure;

import static javax.xml.stream.XMLStreamConstants.ATTRIBUTE;
import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.DTD;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.ENTITY_DECLARATION;
import static javax.xml.stream.XMLStreamConstants.ENTITY_REFERENCE;
import static javax.xml.stream.XMLStreamConstants.NAMESPACE;
import static javax.xml.stream.XMLStreamConstants.NOTATION_DECLARATION;
import static javax.xml.stream.XMLStreamConstants.PROCESSING_INSTRUCTION;
import static javax.xml.stream.XMLStreamConstants.SPACE;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.util.EnumSet;
import java.util.Set;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for urn:jboss:ejb-client:1.0:jboss-ejb-client
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
class EJBClientDescriptor10Parser implements XMLElementReader<EJBClientDescriptorMetaData> {

    public static final String NAMESPACE_1_0 = "urn:jboss:ejb-client:1.0";

    protected final PropertyReplacer propertyReplacer;

    protected EJBClientDescriptor10Parser(final PropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }


    @Override
    public void readElement(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());

                    switch (element) {
                        case CLIENT_CONTEXT:
                            this.parseClientContext(reader, ejbClientDescriptorMetaData);
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
    }

    protected void parseClientContext(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        final Set<EJBClientDescriptorXMLElement> visited = EnumSet.noneOf(EJBClientDescriptorXMLElement.class);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    if (visited.contains(element)) {
                        unexpectedElement(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case EJB_RECEIVERS:
                            this.parseEJBReceivers(reader, ejbClientDescriptorMetaData);
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
    }

    protected void parseEJBReceivers(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {

        // initialize the local-receiver-pass-by-value to the default true
        Boolean localReceiverPassByValue = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final EJBClientDescriptorXMLAttribute attribute = EJBClientDescriptorXMLAttribute.forName(reader.getAttributeLocalName(i));
            final String value = readResolveValue(reader, i);
            switch (attribute) {
                case EXCLUDE_LOCAL_RECEIVER:
                    final boolean excludeLocalReceiver = Boolean.parseBoolean(value);
                    ejbClientDescriptorMetaData.setExcludeLocalReceiver(excludeLocalReceiver);
                    break;
                case LOCAL_RECEIVER_PASS_BY_VALUE:
                    localReceiverPassByValue = Boolean.parseBoolean(value);
                    break;
                default:
                    unexpectedContent(reader);
            }
        }
        // set the local receiver pass by value into the metadata
        ejbClientDescriptorMetaData.setLocalReceiverPassByValue(localReceiverPassByValue);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    switch (element) {
                        case REMOTING_EJB_RECEIVER:
                            this.parseRemotingReceiver(reader, ejbClientDescriptorMetaData);
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
    }

    protected void parseRemotingReceiver(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        String outboundConnectionRef = null;
        final Set<EJBClientDescriptorXMLAttribute> required = EnumSet.of(EJBClientDescriptorXMLAttribute.OUTBOUND_CONNECTION_REF);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final EJBClientDescriptorXMLAttribute attribute = EJBClientDescriptorXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case OUTBOUND_CONNECTION_REF:
                    outboundConnectionRef = readResolveValue(reader, i);
                    ejbClientDescriptorMetaData.addRemotingReceiverConnectionRef(outboundConnectionRef);
                    break;
                default:
                    unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            missingAttributes(reader.getLocation(), required);
        }
        // This element is just composed of attributes which we already processed, so no more content
        // is expected
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            unexpectedContent(reader);
        }
    }

    protected static void unexpectedEndOfDocument(final Location location) throws XMLStreamException {
        throw EeLogger.ROOT_LOGGER.errorParsingEJBClientDescriptor("Unexpected end of document", location);
    }

    protected static void missingAttributes(final Location location, final Set<EJBClientDescriptorXMLAttribute> required) throws XMLStreamException {
        final StringBuilder b = new StringBuilder("Missing one or more required attributes:");
        for (EJBClientDescriptorXMLAttribute attribute : required) {
            b.append(' ').append(attribute);
        }
        throw EeLogger.ROOT_LOGGER.errorParsingEJBClientDescriptor(b.toString(), location);
    }

    /**
     * Throws a XMLStreamException for the unexpected element that was encountered during the parse
     *
     * @param reader the stream reader
     * @throws XMLStreamException
     */
    protected static void unexpectedElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        throw EeLogger.ROOT_LOGGER.unexpectedElement(reader.getName(), reader.getLocation());
    }

    protected static void unexpectedContent(final XMLStreamReader reader) throws XMLStreamException {
        final String kind;
        switch (reader.getEventType()) {
            case ATTRIBUTE:
                kind = "attribute";
                break;
            case CDATA:
                kind = "cdata";
                break;
            case CHARACTERS:
                kind = "characters";
                break;
            case COMMENT:
                kind = "comment";
                break;
            case DTD:
                kind = "dtd";
                break;
            case END_DOCUMENT:
                kind = "document end";
                break;
            case END_ELEMENT:
                kind = "element end";
                break;
            case ENTITY_DECLARATION:
                kind = "entity declaration";
                break;
            case ENTITY_REFERENCE:
                kind = "entity ref";
                break;
            case NAMESPACE:
                kind = "namespace";
                break;
            case NOTATION_DECLARATION:
                kind = "notation declaration";
                break;
            case PROCESSING_INSTRUCTION:
                kind = "processing instruction";
                break;
            case SPACE:
                kind = "whitespace";
                break;
            case START_DOCUMENT:
                kind = "document start";
                break;
            case START_ELEMENT:
                kind = "element start";
                break;
            default:
                kind = "unknown";
                break;
        }
        final StringBuilder b = new StringBuilder("Unexpected content of type '").append(kind).append('\'');
        if (reader.hasName()) {
            b.append(" named '").append(reader.getName()).append('\'');
        }
        if (reader.hasText()) {
            b.append(", text is: '").append(reader.getText()).append('\'');
        }
        throw EeLogger.ROOT_LOGGER.errorParsingEJBClientDescriptor(b.toString(), reader.getLocation());
    }

    protected String readResolveValue(final XMLExtendedStreamReader reader, final int index) {
        return propertyReplacer.replaceProperties(reader.getAttributeValue(index).trim());
    }

}
