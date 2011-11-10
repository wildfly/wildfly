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
import java.util.Collections;
import java.util.List;

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
        final ModelNode addsubsystem = JAXRSubsystemAdd.createAddSubsystemOperation();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JAXR_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTIONFACTORY: {
                            parseBinding(reader, addsubsystem, ModelConstants.CONNECTIONFACTORY);
                            break;
                        }
                        case DATASOURCE: {
                            parseBinding(reader, addsubsystem, ModelConstants.DATASOURCE);
                            break;
                        }
                        case FLAGS: {
                            parseFlags(reader, addsubsystem);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                }
            }
        }
        operations.add(addsubsystem);
    }

    private void parseBinding(XMLExtendedStreamReader reader, ModelNode operation, String modelAttribute) throws XMLStreamException {

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

        operation.get(modelAttribute).set(jndiName);
    }

    private void parseFlags(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

        // Handle attributes
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DROPONSTART: {
                    operation.get(ModelConstants.DROPONSTART).set(attrValue);
                    break;
                }
                case CREATEONSTART: {
                    operation.get(ModelConstants.CREATEONSTART).set(attrValue);
                    break;
                }
                case DROPONSTOP: {
                    operation.get(ModelConstants.DROPONSTOP).set(attrValue);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);
    }
}