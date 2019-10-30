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


import static org.jboss.as.jaxr.extension.JAXRConstants.Attribute;
import static org.jboss.as.jaxr.extension.JAXRConstants.Element;
import static org.jboss.as.jaxr.extension.JAXRConstants.Namespace;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

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
        JAXRSubsystemRootResource.CONNECTION_FACTORY_ATTRIBUTE.marshallAsAttribute(node, writer);
        JAXRSubsystemRootResource.CONNECTION_FACTORY_IMPL_ATTRIBUTE.marshallAsAttribute(node, writer);
        writer.writeEndElement();


        ModelNode properties = node.get(ModelDescriptionConstants.PROPERTY);
        if (properties.isDefined()) {
            writer.writeStartElement(Element.PROPERTIES.getLocalName());
            for (String key : properties.keys()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), key);
                JAXRPropertyDefinition.VALUE.marshallAsAttribute(properties.get(key), writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

}
