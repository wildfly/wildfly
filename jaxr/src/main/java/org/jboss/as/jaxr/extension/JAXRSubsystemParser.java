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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

        Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
        switch (readerNS) {
            case JAXR_1_0:
                readElement1_0(reader, operations);
                break;
            case JAXR_1_1:
                readElement1_1(reader, operations);
                break;
            default:
              throw unexpectedElement(reader);
        }
    }

    private void readElement1_0(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(address);

        operations.add(addOp);

        List<ModelNode> propertiesOps = null;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JAXR_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTION_FACTORY: {
                            parseBinding1_0(reader, addOp);
                            break;
                        }
                        case JUDDI_SERVER: {
                            propertiesOps = parseJuddiServer(reader, address);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                }
            }
        }

        if (propertiesOps != null) {
            operations.addAll(propertiesOps);
        }
    }

    private void readElement1_1(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME);
        address.protect();
        final ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(address);

        operations.add(addOp);

        List<ModelNode> propertiesOps = null;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JAXR_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTION_FACTORY: {
                            parseBinding1_1(reader, addOp);
                            break;
                        }
                        case PROPERTIES: {
                            propertiesOps = parseProperties(reader, address);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                }
            }
        }

        if (propertiesOps != null) {
            operations.addAll(propertiesOps);
        }
    }

    private void parseBinding1_0(XMLExtendedStreamReader reader, ModelNode addOp) throws XMLStreamException {

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
                    JAXRWriteAttributeHandler.CONNECTION_FACTORY_ATTRIBUTE.parseAndSetParameter(jndiName, addOp, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (jndiName == null)
            throw missingRequired(reader, Collections.singleton(Attribute.JNDI_NAME));

        requireNoContent(reader);
    }

    private void parseBinding1_1(XMLExtendedStreamReader reader, ModelNode addOp) throws XMLStreamException {

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
                    JAXRWriteAttributeHandler.CONNECTION_FACTORY_ATTRIBUTE.parseAndSetParameter(jndiName, addOp, reader);
                    break;
                }
                case CLASS: {
                    JAXRWriteAttributeHandler.CONNECTION_FACTORY_IMPL_ATTRIBUTE.parseAndSetParameter(jndiName, addOp, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (jndiName == null)
            throw missingRequired(reader, Collections.singleton(Attribute.JNDI_NAME));

        requireNoContent(reader);
    }

    private List<ModelNode> parseProperties(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        requireNoAttributes(reader);

        List<ModelNode> result = new ArrayList<ModelNode>();
        // Handle properties
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JAXR_1_1: {
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
        return propNode;
    }

    private List<ModelNode> parseJuddiServer(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        List<ModelNode> result = new ArrayList<ModelNode>();

        Set<Attribute> required = EnumSet.of(Attribute.PUBLISH_URL, Attribute.QUERY_URL);
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case PUBLISH_URL: {
                    ModelNode propOp = new ModelNode();
                    propOp.get(OP).set(ADD);
                    propOp.get(OP_ADDR).set(address).add(ModelConstants.PROPERTY, "javax.xml.registry.lifeCycleManagerURL");
                    JAXRPropertyWrite.VALUE.parseAndSetParameter(attrValue, propOp, reader);
                    result.add(propOp);
                    break;
                }
                case QUERY_URL: {
                    ModelNode propOp = new ModelNode();
                    propOp.get(OP).set(ADD);
                    propOp.get(OP_ADDR).set(address).add(ModelConstants.PROPERTY, "javax.xml.registry.queryManagerURL");
                    JAXRPropertyWrite.VALUE.parseAndSetParameter(attrValue, propOp, reader);
                    result.add(propOp);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0)
            throw missingRequired(reader, required);

        requireNoContent(reader);

        return result;
    }

}
