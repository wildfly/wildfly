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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.wildfly.extension.security.manager.deployment;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.security.ModularPermissionFactory;
import org.jboss.modules.security.PermissionFactory;
import org.wildfly.extension.security.manager.logging.SecurityManagerLogger;

/**
 * This class implements a parser for the {@code permissions.xml} and {@code jboss-permissions.xml} descriptors. The
 * parsed permissions are returned as a collection of {@code PermissionFactory} objects.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class PermissionsParser extends MetaDataElementParser {

    public static List<PermissionFactory> parse(final XMLStreamReader reader, final ModuleLoader loader, final ModuleIdentifier identifier)
            throws XMLStreamException {

        reader.require(XMLStreamConstants.START_DOCUMENT, null, null);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.START_ELEMENT: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case PERMISSIONS: {
                            return parsePermissions(reader, loader, identifier);
                        }
                        default: {
                            throw MetaDataElementParser.unexpectedElement(reader);
                        }
                    }
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static List<PermissionFactory> parsePermissions(final XMLStreamReader reader, final ModuleLoader loader, final ModuleIdentifier identifier)
            throws XMLStreamException {

        List<PermissionFactory> factories = new ArrayList<PermissionFactory>();

        // parse the permissions attributes.
        EnumSet<Attribute> requiredAttributes = EnumSet.of(Attribute.VERSION);
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final String attributeNamespace = reader.getAttributeNamespace(i);
            if (attributeNamespace != null && !attributeNamespace.isEmpty()) {
                continue;
            }
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case VERSION: {
                    String version = reader.getAttributeValue(i);
                    if (!"7".equals(version))
                        throw SecurityManagerLogger.ROOT_LOGGER.invalidPermissionsXMLVersion(version, "7");
                    break;
                }
                default: {
                    throw MetaDataElementParser.unexpectedAttribute(reader, i);
                }
            }
            requiredAttributes.remove(attribute);
        }

        // check if all required attributes were parsed.
        if (!requiredAttributes.isEmpty())
            throw MetaDataElementParser.missingRequired(reader, requiredAttributes);

        // parse the permissions sub-elements.
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return factories;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case PERMISSION: {
                            PermissionFactory factory = parsePermission(reader, loader, identifier);
                            factories.add(factory);
                            break;
                        }
                        default: {
                            throw MetaDataElementParser.unexpectedElement(reader);
                        }
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

    private static PermissionFactory parsePermission(final XMLStreamReader reader, final ModuleLoader loader, final ModuleIdentifier identifier)
            throws XMLStreamException {

        // permission element has no attributes.
        MetaDataElementParser.requireNoAttributes(reader);

        String permissionClass = null;
        String permissionName = null;
        String permissionActions = null;

        EnumSet<Element> requiredElements = EnumSet.of(Element.CLASS_NAME);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    // check if all required permission elements have been processed.
                    if (!requiredElements.isEmpty())
                        throw missingRequiredElement(reader, requiredElements);

                    // build a permission and add it to the list.
                    PermissionFactory factory = new ModularPermissionFactory(loader, identifier, permissionClass,
                            permissionName, permissionActions);
                    return factory;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    Element element = Element.forName(reader.getLocalName());
                    requiredElements.remove(element);
                    switch (element) {
                        case CLASS_NAME: {
                            MetaDataElementParser.requireNoAttributes(reader);
                            permissionClass = reader.getElementText();
                            break;
                        }
                        case NAME: {
                            MetaDataElementParser.requireNoAttributes(reader);
                            permissionName = reader.getElementText();
                            break;
                        }
                        case ACTIONS: {
                            MetaDataElementParser.requireNoAttributes(reader);
                            permissionActions = reader.getElementText();
                            break;
                        }
                        default: {
                            throw MetaDataElementParser.unexpectedElement(reader);
                        }
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
        final StringBuilder b = new StringBuilder("Unexpected content of type '").append(kind).append('\'');
        if (reader.hasName()) {
            b.append(" named '").append(reader.getName()).append('\'');
        }
        if (reader.hasText()) {
            b.append(", text is: '").append(reader.getText()).append('\'');
        }
        return new XMLStreamException(b.toString(), reader.getLocation());
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Unexpected end of document", location);
    }

    /**
     * <p>
     * Enumeration of the persistence.xml configuration elements.
     * </p>
     *
     * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
     */
    enum Element {

        UNKNOWN(null),

        PERMISSIONS("permissions"),
        PERMISSION("permission"),
        CLASS_NAME("class-name"),
        NAME("name"),
        ACTIONS("actions");


        // elements used to configure the ORB.

        private final String name;

        /**
         * <p>
         * {@code Element} constructor. Sets the element name.
         * </p>
         *
         * @param name a {@code String} representing the local name of the element.
         */
        Element(final String name) {
            this.name = name;
        }

        /**
         * <p>
         * Obtains the local name of this element.
         * </p>
         *
         * @return a {@code String} representing the element's local name.
         */
        public String getLocalName() {
            return name;
        }

        // a map that caches all available elements by name.
        private static final Map<String, Element> MAP;

        static {
            final Map<String, Element> map = new HashMap<String, Element>();
            for (Element element : values()) {
                final String name = element.getLocalName();
                if (name != null)
                    map.put(name, element);
            }
            MAP = map;
        }


        /**
         * <p>
         * Gets the {@code Element} identified by the specified name.
         * </p>
         *
         * @param localName a {@code String} representing the local name of the element.
         * @return the {@code Element} identified by the name. If no attribute can be found, the {@code Element.UNKNOWN}
         *         type is returned.
         */
        public static Element forName(String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }

    enum Attribute {
        UNKNOWN(null),
        VERSION("version");

        private final String name;

        /**
         * <p>
         * {@code Attribute} constructor. Sets the attribute name.
         * </p>
         *
         * @param name a {@code String} representing the local name of the attribute.
         */
        Attribute(final String name) {
            this.name = name;
        }

        /**
         * <p>
         * Obtains the local name of this attribute.
         * </p>
         *
         * @return a {@code String} representing the attribute local name.
         */
        public String getLocalName() {
            return this.name;
        }

        // a map that caches all available attributes by name.
        private static final Map<String, Attribute> MAP;

        static {
            final Map<String, Attribute> map = new HashMap<String, Attribute>();
            for (Attribute attribute : values()) {
                final String name = attribute.name;
                if (name != null)
                    map.put(name, attribute);
            }
            MAP = map;
        }

        /**
         * <p>
         * Gets the {@code Attribute} identified by the specified name.
         * </p>
         *
         * @param localName a {@code String} representing the local name of the attribute.
         * @return the {@code Attribute} identified by the name. If no attribute can be found, the {@code Attribute.UNKNOWN}
         *         type is returned.
         */
        public static Attribute forName(String localName) {
            final Attribute attribute = MAP.get(localName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }
}
