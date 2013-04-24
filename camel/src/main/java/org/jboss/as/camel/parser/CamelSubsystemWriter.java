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
package org.jboss.as.camel.parser;

import java.util.TreeSet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.camel.parser.Namespace10.Attribute;
import org.jboss.as.camel.parser.Namespace10.Element;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Write Camel subsystem configuration.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 23-Aug-2013
 */
final class CamelSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    static CamelSubsystemWriter INSTANCE = new CamelSubsystemWriter();

    // hide ctor
    private CamelSubsystemWriter() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();

        if (node.hasDefined(ModelConstants.CONTEXT)) {
            ModelNode properties = node.get(ModelConstants.CONTEXT);
            for (String key : new TreeSet<String>(properties.keys())) {
                String val = properties.get(key).get(ModelConstants.VALUE).asString();
                writer.writeStartElement(Element.CAMEL_CONTEXT.getLocalName());
                writer.writeAttribute(Attribute.ID.getLocalName(), key);
                writer.writeCharacters(val);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement();
    }
}