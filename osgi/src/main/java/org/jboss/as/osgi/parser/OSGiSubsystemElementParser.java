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

package org.jboss.as.osgi.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ExtensionContext.SubsystemConfiguration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.osgi.parser.OSGiSubsystemState.Activation;
import org.jboss.as.osgi.parser.OSGiSubsystemState.OSGiModule;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser responsible for handling the OSGi subsystem schema.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public final class OSGiSubsystemElementParser implements XMLStreamConstants,
        XMLElementReader<ParseResult<SubsystemConfiguration<OSGiSubsystemElement>>> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<SubsystemConfiguration<OSGiSubsystemElement>> result)
            throws XMLStreamException {

        OSGiSubsystemAdd add = new OSGiSubsystemAdd();
        OSGiSubsystemState subsystemState = add.getSubsystemState();

        // Handle attributes
        ParseUtils.requireNoAttributes(reader);

        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ACTIVATION: {
                            parseActivationElement(reader, subsystemState);
                            break;
                        }
                        case PROPERTIES: {
                            parsePropertiesElement(reader, subsystemState);
                            break;
                        }
                        case MODULES: {
                            parseModulesElement(reader, subsystemState);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        List<AbstractSubsystemUpdate<OSGiSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<OSGiSubsystemElement, ?>>();
        updates.add(new OSGiSubsystemUpdate(subsystemState));

        result.setResult(new SubsystemConfiguration<OSGiSubsystemElement>(add, updates));
    }

    private void parseActivationElement(XMLExtendedStreamReader reader, OSGiSubsystemState subsystemState) throws XMLStreamException {

        switch (Namespace.forUri(reader.getNamespaceURI())) {
            case OSGI_1_0: {
                    // Handle attributes
                    Activation value = null;
                    int count = reader.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        final String attrValue = reader.getAttributeValue(i);
                        if (reader.getAttributeNamespace(i) != null) {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        } else {
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case POLICY: {
                                    value = Activation.valueOf(attrValue.toUpperCase());
                                    break;
                                }
                                default:
                                    throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                        }
                    }
                    if (value == null)
                        throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.POLICY));

                    subsystemState.setActivation(value);
                    ParseUtils.requireNoContent(reader);
                    break;
            }
            default:
                throw ParseUtils.unexpectedElement(reader);
        }
    }

    void parsePropertiesElement(XMLExtendedStreamReader reader, final OSGiSubsystemState subsystemState) throws XMLStreamException {

        // Handle attributes
        ParseUtils.requireNoAttributes(reader);

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.PROPERTY) {
                        // Handle attributes
                        String name = null;
                        String value = null;
                        int count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            final String attrValue = reader.getAttributeValue(i);
                            if (reader.getAttributeNamespace(i) != null) {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            } else {
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case NAME: {
                                        name = attrValue;
                                        if (subsystemState.getProperties().containsKey(name)) {
                                            throw new XMLStreamException("Property " + name + " already exists",
                                                    reader.getLocation());
                                        }
                                        break;
                                    }
                                    default:
                                        throw ParseUtils.unexpectedAttribute(reader, i);
                                }
                            }
                        }
                        if (name == null) {
                            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
                        }
                        value = reader.getElementText().trim();
                        if (value == null || value.length() == 0) {
                            throw new XMLStreamException("Value for property " + name + " is null", reader.getLocation());
                        }
                        subsystemState.addProperty(name, value);
                        break;
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        if (subsystemState.getProperties().size() == 0) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.PROPERTY));
        }
    }

    void parseModulesElement(XMLExtendedStreamReader reader, final OSGiSubsystemState subsystemState) throws XMLStreamException {

        // Handle attributes
        ParseUtils.requireNoAttributes(reader);

        // Handle elements
        Set<ModuleIdentifier> identifiers = new HashSet<ModuleIdentifier>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.MODULE) {
                        ModuleIdentifier identifier = null;
                        boolean start = false;
                        final int count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            if (reader.getAttributeNamespace(i) != null) {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case IDENTIFIER: {
                                    identifier = ModuleIdentifier.fromString(reader.getAttributeValue(i));
                                    break;
                                }
                                case START: {
                                    start = Boolean.parseBoolean(reader.getAttributeValue(i));
                                    break;
                                }
                                default:
                                    throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                        }
                        if (identifier == null)
                            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.IDENTIFIER));
                        if (identifiers.contains(identifier))
                            throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());

                        subsystemState.addModule(new OSGiModule(identifier, start));
                        identifiers.add(identifier);

                        ParseUtils.requireNoContent(reader);
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        if (subsystemState.getModules().size() == 0) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.MODULE));
        }
    }
}
