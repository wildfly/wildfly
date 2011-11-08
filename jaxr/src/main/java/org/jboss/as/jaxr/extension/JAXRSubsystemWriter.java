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

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import static org.jboss.as.jaxr.extension.JAXRConstants.Attribute;
import static org.jboss.as.jaxr.extension.JAXRConstants.Element;
import static org.jboss.as.jaxr.extension.JAXRConstants.Namespace;

/**
 * The subsystem writer
 */
public class JAXRSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    static final JAXRSubsystemWriter INSTANCE = new JAXRSubsystemWriter();

    // Hide ctor
    private JAXRSubsystemWriter() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();
        if (has(node, ModelConstants.JNDI_NAME)) {
            writer.writeStartElement(Element.DATASOURCE.getLocalName());
            writeAttribute(writer, Attribute.JNDI_NAME, node.get(ModelConstants.JNDI_NAME));
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private boolean has(ModelNode node, String name) {
        if (node.has(name) && node.get(name).isDefined()) {
            ModelNode n = node.get(name);
            switch (n.getType()) {
                case LIST:
                case OBJECT:
                    return n.asList().size() > 0;
                default:
                    return true;
            }
        }
        return false;
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }
}