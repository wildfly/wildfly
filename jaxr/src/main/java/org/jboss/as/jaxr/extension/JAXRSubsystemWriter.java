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

import java.util.TreeSet;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.jaxr.ModelConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import static org.jboss.as.jaxr.JAXRConstants.Attribute;
import static org.jboss.as.jaxr.JAXRConstants.Element;
import static org.jboss.as.jaxr.JAXRConstants.Namespace;

/**
 * The subsystem writer
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kurt Stam
 * @since 26-Oct-2011
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

        // write connection-factory
        writer.writeStartElement(Element.CONNECTION_FACTORY.getLocalName());
        writeAttribute(writer, Attribute.JNDI_NAME, node.get(ModelConstants.CONNECTION_FACTORY));
        ModelNode clazz = node.get(ModelConstants.CONNECTION_FACTORY_IMPL);
        if (clazz.isDefined()) {
            writeAttribute(writer, Attribute.CLASS, clazz);
        }
        writer.writeEndElement();

        writer.writeStartElement(Element.PROPERTIES.getLocalName());
        ModelNode properties = node.get(ModelConstants.PROPERTY);
        if (properties.isDefined()) {
            for (String key : new TreeSet<String>(properties.keys())) {
                String val = properties.get(key).get(ModelConstants.VALUE).asString();
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), key);
                writer.writeAttribute(Attribute.VALUE.getLocalName(), val);
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();

        writer.writeEndElement();
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }
}