/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ExtensionContext;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * JMX subsystem parser.
 *
 * @author John Bailey
 */
class JmxSubsystemElementParser implements XMLStreamConstants, XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<JmxSubsystemElement>>> {

    /** {@inheritDoc} */
    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<ExtensionContext.SubsystemConfiguration<JmxSubsystemElement>> result) throws XMLStreamException {
        final List<AbstractSubsystemUpdate<JmxSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<JmxSubsystemElement,?>>();
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch(element){
                case JMX_CONNECTOR: {
                    parseConnector(reader, updates);
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
        result.setResult(new ExtensionContext.SubsystemConfiguration<JmxSubsystemElement>(new JmxSubsystemAdd(), updates));
    }

    static void parseConnector(final XMLExtendedStreamReader reader, final List<AbstractSubsystemUpdate<JmxSubsystemElement, ?>> updates) throws XMLStreamException {
        String serverBinding = null;
        String registryBinding = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SERVER_BINDING: {
                    serverBinding = value;
                    break;
                } case REGISTRY_BINDING: {
                    registryBinding = value;
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Require no content
        ParseUtils.requireNoContent(reader);
        if(serverBinding == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SERVER_BINDING));
        }
        if(registryBinding == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.REGISTRY_BINDING));
        }
        updates.add(new JMXConnectorAdd(serverBinding, registryBinding));
    }
}
