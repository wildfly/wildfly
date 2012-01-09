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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.configadmin.parser;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * Parse subsystem configuration for namespace {@link Namespace#VERSION_1_0}.
 *
 * @author Thomas.Diesler@jboss.com
 */
class ConfigAdminParser implements Namespace10, XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    static XMLElementReader<List<ModelNode>> INSTANCE = new ConfigAdminParser();

    // hide ctor
    private ConfigAdminParser() {
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME);
        address.protect();

        requireNoAttributes(reader);

        final ModelNode subsystem = ConfigAdminAdd.createAddSubsystemOperation();
        operations.add(subsystem);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONFIGURATION: {
                            List<ModelNode> result = parseConfigurations(reader, address);
                            operations.addAll(result);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                }
            }
        }
    }

    private List<ModelNode> parseConfigurations(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        // Handle attributes
        String pid = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PID: {
                    pid = attrValue;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (pid == null)
            throw missingRequired(reader, Collections.singleton(Attribute.PID));

        ModelNode configuration = new ModelNode();
        configuration.get(OP).set(ADD);
        configuration.get(OP_ADDR).set(address).add(ModelConstants.CONFIGURATION, pid);

        List<ModelNode> result = new ArrayList<ModelNode>();
        result.add(configuration);

        ModelNode propNode = configuration.get(ModelConstants.ENTRIES);

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.PROPERTY) {
                        // Handle attributes
                        String name = null;
                        String value = null;
                        count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            requireNoNamespaceAttribute(reader, i);
                            final String attrValue = reader.getAttributeValue(i);

                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case NAME: {
                                    name = attrValue;
                                    // if (configuration.has(name))
                                    // throw new XMLStreamException(MESSAGES.propertyAlreadyExists(name), reader.getLocation());
                                    break;
                                }
                                case VALUE: {
                                    value = attrValue;
                                    break;
                                }
                                default:
                                    throw unexpectedAttribute(reader, i);
                            }
                        }
                        if (name == null)
                            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
                        if (value == null)
                            throw missingRequired(reader, Collections.singleton(Attribute.VALUE));

                        requireNoContent(reader);

                        propNode.get(name).set(value);

                        break;
                    } else {
                        throw unexpectedElement(reader);
                    }
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

        return result;
    }
}