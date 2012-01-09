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
package org.jboss.as.osgi.parser;

import java.util.TreeSet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.osgi.parser.Namespace11.Attribute;
import org.jboss.as.osgi.parser.Namespace11.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Parse subsystem configuration.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 */
class OSGiSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    static OSGiSubsystemWriter INSTANCE = new OSGiSubsystemWriter();

    // hide ctor
    private OSGiSubsystemWriter() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();

        if (has(node, ModelConstants.ACTIVATION)) {
            writeAttribute(writer, Attribute.ACTIVATION, node.get(ModelConstants.ACTIVATION));
        }

        if (has(node, ModelConstants.PROPERTY)) {
            writer.writeStartElement(Element.PROPERTIES.getLocalName());
            ModelNode properties = node.get(ModelConstants.PROPERTY);
            for (String key : new TreeSet<String>(properties.keys())) {
                String val = properties.get(key).get(ModelConstants.VALUE).asString();
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), key);
                writer.writeCharacters(val);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if (has(node, ModelConstants.CAPABILITY)) {
            writer.writeStartElement(Element.CAPABILITIES.getLocalName());
            ModelNode modules = node.get(ModelConstants.CAPABILITY);
            for (String key : modules.keys()) {
                ModelNode moduleNode = modules.get(key);
                writer.writeEmptyElement(Element.CAPABILITY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), key);
                if (moduleNode.has(ModelConstants.STARTLEVEL)) {
                    writeAttribute(writer, Attribute.STARTLEVEL, moduleNode.require(ModelConstants.STARTLEVEL));
                }
            }
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