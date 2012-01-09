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

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.configadmin.parser.Namespace10.Attribute;
import org.jboss.as.configadmin.parser.Namespace10.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.TreeSet;

/**
 * Write subsystem configuration.
 *
 * @author Thomas.Diesler@jboss.com
 */
class ConfigAdminWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    static ConfigAdminWriter INSTANCE = new ConfigAdminWriter();

    // hide ctor
    private ConfigAdminWriter() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();

        if (has(node, ModelConstants.CONFIGURATION)) {
            ModelNode configuration = node.get(ModelConstants.CONFIGURATION);
            for (String pid : new TreeSet<String>(configuration.keys())) {
                writer.writeStartElement(Element.CONFIGURATION.getLocalName());
                writer.writeAttribute(Attribute.PID.getLocalName(), pid);

                ModelNode entries = configuration.get(pid).get(ModelConstants.ENTRIES);
                if (entries.isDefined()) {
                    for (String propKey : entries.keys()) {
                        String propValue = entries.get(propKey).asString();
                        writer.writeStartElement(Element.PROPERTY.getLocalName());
                        writer.writeAttribute(Attribute.NAME.getLocalName(), propKey);
                        writer.writeAttribute(Attribute.VALUE.getLocalName(), propValue);
                        writer.writeEndElement();
                    }
                }
                writer.writeEndElement();
            }
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
}