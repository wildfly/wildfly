/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.jbossjpaparser;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.config.JPADeploymentSettings;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parse the jboss-jpa settings in jboss-all.xml.
 *
 * @author Scott Marlow
 */
public class JBossJPAParser  {

    public static final String NAMESPACE_1_0 = "http://www.jboss.com/xml/ns/javaee";

    private static final JBossJPAParser INSTANCE = new JBossJPAParser();

    private static final String INHERITANCE_CONSTANT = "inheritance";

    public static JPADeploymentSettings parser(XMLExtendedStreamReader reader, PropertyReplacer propertyReplacer) throws XMLStreamException {
        JPADeploymentSettings result = new JPADeploymentSettings();

        INSTANCE.readElement(reader, result, propertyReplacer);
        return result;
    }

    enum Element {
        JBOSS_JPA_DESCRIPTOR,
        EXTENDED_PERSISTENCE,

        // default unknown element
        UNKNOWN;

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, "jboss-jpa"), Element.JBOSS_JPA_DESCRIPTOR);
            elementsMap.put(new QName(NAMESPACE_1_0, "extended-persistence"), Element.EXTENDED_PERSISTENCE);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NAMESPACE_1_0, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }
    }


    enum Version {
        JBOSS_JPA_1_0,
        UNKNOWN
    }

    private JBossJPAParser() {
    }

    public void readElement(final XMLExtendedStreamReader reader, final JPADeploymentSettings result, PropertyReplacer propertyReplacer) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count != 0) {
            throw unexpectedContent(reader);
        }
        // xsd:sequence
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case EXTENDED_PERSISTENCE:
                            final String value = getAttributeValue(reader, null, INHERITANCE_CONSTANT, propertyReplacer);
                            if (value == null || value.isEmpty()) {
                                result.setExtendedPersistenceInheritanceType(ExtendedPersistenceInheritance.SHALLOW);
                            } else {
                                result.setExtendedPersistenceInheritanceType(ExtendedPersistenceInheritance.valueOf(value));
                            }
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE:
                kind = "attribute";
                break;
            case XMLStreamConstants.CDATA:
                kind = "cdata";
                break;
            case XMLStreamConstants.CHARACTERS:
                kind = "characters";
                break;
            case XMLStreamConstants.COMMENT:
                kind = "comment";
                break;
            case XMLStreamConstants.DTD:
                kind = "dtd";
                break;
            case XMLStreamConstants.END_DOCUMENT:
                kind = "document end";
                break;
            case XMLStreamConstants.END_ELEMENT:
                kind = "element end";
                break;
            case XMLStreamConstants.ENTITY_DECLARATION:
                kind = "entity declaration";
                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                kind = "entity ref";
                break;
            case XMLStreamConstants.NAMESPACE:
                kind = "namespace";
                break;
            case XMLStreamConstants.NOTATION_DECLARATION:
                kind = "notation declaration";
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                kind = "processing instruction";
                break;
            case XMLStreamConstants.SPACE:
                kind = "whitespace";
                break;
            case XMLStreamConstants.START_DOCUMENT:
                kind = "document start";
                break;
            case XMLStreamConstants.START_ELEMENT:
                kind = "element start";
                break;
            default:
                kind = "unknown";
                break;
        }

        return ServerLogger.ROOT_LOGGER.unexpectedContent(kind, (reader.hasName() ? reader.getName() : null),
                (reader.hasText() ? reader.getText() : null), reader.getLocation());
    }

    private static String getAttributeValue(final XMLStreamReader reader,final String namespaceURI, final String localName, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        return propertyReplacer.replaceProperties(reader.getAttributeValue(namespaceURI,localName));
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return ServerLogger.ROOT_LOGGER.unexpectedEndOfDocument(location);
    }

}
