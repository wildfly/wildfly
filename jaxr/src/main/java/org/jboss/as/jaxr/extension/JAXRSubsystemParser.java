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

import org.jboss.as.jaxr.extension.JAXRConstants.Attribute;
import org.jboss.as.jaxr.extension.JAXRConstants.Element;
import org.jboss.as.jaxr.extension.JAXRConstants.Namespace;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * The subsystem parser.
 */
public class JAXRSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        final ModelNode addop = JAXRSubsystemAdd.createAddSubsystemOperation();
        operations.add(addop);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JAXR_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTIONFACTORY: {
                            ModelNode result = parseBinding(reader, addop, ModelConstants.CONNECTIONFACTORY);
                            operations.add(result);
                            break;
                        }
                        case DATASOURCE: {
                            ModelNode result = parseBinding(reader, addop, ModelConstants.DATASOURCE);
                            operations.add(result);
                            break;
                        }
                        case FLAGS: {
                            List<ModelNode> result = parseFlags(reader, addop);
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

    private ModelNode parseBinding(XMLExtendedStreamReader reader, ModelNode addop, String modelAttribute) throws XMLStreamException {

        final ModelNode result = new ModelNode();
        result.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        result.get(OP_ADDR).add(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME);

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
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (jndiName == null)
            throw missingRequired(reader, Collections.singleton(Attribute.JNDI_NAME));

        requireNoContent(reader);

        addop.get(modelAttribute).set(jndiName);

        result.get(NAME).set(modelAttribute);
        result.get(VALUE).set(jndiName);

        return result;
    }

    private List<ModelNode> parseFlags(XMLExtendedStreamReader reader, ModelNode addop) throws XMLStreamException {

        List<ModelNode> result = new ArrayList<ModelNode>();
        final ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME);

        // Handle attributes
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DROPONSTART: {
                    addop.get(ModelConstants.DROPONSTART).set(attrValue);
                    op.get(NAME).set(ModelConstants.DROPONSTART);
                    op.get(VALUE).set(attrValue);
                    result.add(op);
                    break;
                }
                case CREATEONSTART: {
                    addop.get(ModelConstants.CREATEONSTART).set(attrValue);
                    op.get(NAME).set(ModelConstants.CREATEONSTART);
                    op.get(VALUE).set(attrValue);
                    result.add(op);
                    break;
                }
                case DROPONSTOP: {
                    addop.get(ModelConstants.DROPONSTOP).set(attrValue);
                    op.get(NAME).set(ModelConstants.DROPONSTOP);
                    op.get(VALUE).set(attrValue);
                    result.add(op);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);
        return result;
    }
}