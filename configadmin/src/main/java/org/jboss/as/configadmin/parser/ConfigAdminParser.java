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

import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.List;
import java.util.TreeSet;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Parse subsystem configuration for namespace {@link Namespace#VERSION_1_0}.
 *
 * @author Thomas.Diesler@jboss.com
 */
class ConfigAdminParser implements Namespace10, XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    static ConfigAdminParser INSTANCE = new ConfigAdminParser();

    // hide ctor
    private ConfigAdminParser() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode model = context.getModelNode();

        if (model.hasDefined(ModelConstants.CONFIGURATION)) {
            ModelNode configuration = model.get(ModelConstants.CONFIGURATION);
            for (String pid : new TreeSet<String>(configuration.keys())) {
                writer.writeStartElement(Element.CONFIGURATION.getLocalName());
                writer.writeAttribute(Attribute.PID.getLocalName(), pid);
                ConfigurationResource.ENTRIES.marshallAsElement(configuration.get(pid), writer);
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }


    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(ConfigAdminExtension.SUBSYSTEM_PATH);

        operations.add(Util.createAddOperation(address));

        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONFIGURATION: {
                            parseConfigurations(reader, address, operations);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                }
            }
        }
    }

    private void parseConfigurations(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> operations) throws XMLStreamException {

        // Handle attributes
        String pid = ParseUtils.requireAttributes(reader, Attribute.PID.getLocalName())[0];

        ModelNode operation = Util.createAddOperation(parent.append(ModelConstants.CONFIGURATION, pid));
        operations.add(operation);
        // Handle elements
        while (reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.PROPERTY) {
                        final String[] array = requireAttributes(reader, org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName());
                        ConfigurationResource.ENTRIES.parseAndAddParameterElement(array[0], array[1], operation, reader);
                        ParseUtils.requireNoContent(reader);
                        break;
                    } else {
                        throw unexpectedElement(reader);
                    }
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }
}