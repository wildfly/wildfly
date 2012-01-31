/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.jaxr.JAXRConstants.Attribute;
import org.jboss.as.jaxr.JAXRConstants.Element;
import org.jboss.as.jaxr.JAXRConstants.Namespace;
import org.jboss.as.jaxr.JAXRConstants;
import org.jboss.as.jaxr.ModelConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The subsystem parser.
 * @author Thomas.Diesler@jboss.com
 * @author Kurt Stam
 * @since 26-Oct-2011
 */
public class JAXRSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME);
        address.protect();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JAXR_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTION_FACTORY: {
                            ModelNode connectionFactory = parseBinding(reader, address);
                            operations.add(connectionFactory);
                            break;
                        }
                        case PROPERTIES: {
                            List<ModelNode> result = parseProperties(reader, address);
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

    private ModelNode parseBinding(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {
        final ModelNode result = new ModelNode();
        result.get(OP).set(ADD);
        result.get(OP_ADDR).set(address);
        // Handle attributes
        String jndiName = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case JNDI_NAME: {
                    jndiName = attrValue;
                    result.get(ModelConstants.CONNECTION_FACTORY).set(attrValue);
                    break;
                }
                case CLASS: {
                    jndiName = attrValue;
                    result.get(ModelConstants.CONNECTION_FACTORY_IMPL).set(attrValue);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (jndiName == null)
            throw missingRequired(reader, Collections.singleton(Attribute.JNDI_NAME));

        requireNoContent(reader);

        return result;
    }

    private List<ModelNode> parseProperties(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        requireNoAttributes(reader);

        List<ModelNode> result = new ArrayList<ModelNode>();
        // Handle properties
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JAXR_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case PROPERTY: {
                            ModelNode propNode = parseProperty(reader, address);
                            result.add(propNode);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                }
            }
        }
        return result;
    }

    private ModelNode parseProperty(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        // Handle attributes
        String name = null;
        String value = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = attrValue;
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

        ModelNode propNode = new ModelNode();
        propNode.get(OP).set(ADD);
        propNode.get(OP_ADDR).set(address).add(ModelConstants.PROPERTY, name);
        propNode.get(ModelConstants.VALUE).set(value);
        //propNode.get(name).set(value);
        return propNode;
    }

}