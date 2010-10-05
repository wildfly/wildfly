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

package org.jboss.as.naming.service;

import java.util.List;

import org.jboss.as.Extension;
import org.jboss.as.ExtensionContext;
import org.jboss.as.SubsystemFactory;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseUtils;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import javax.xml.stream.XMLStreamException;

/**
 * Domain extension used to initialize the naming subsystem element handlers.
 *
 * @author John E. Bailey
 */
public class NamingExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:naming:1.0";

    static final NamingSubSystemElementParser PARSER = new NamingSubSystemElementParser();
    static final SubsystemFactory<NamingSubsystemElement> FACTORY = new SubsystemFactory<NamingSubsystemElement>() {
        public NamingSubsystemElement createSubsystemElement() {
            return new NamingSubsystemElement();
        };
    };

    /** {@inheritDoc} */
    public void initialize(ExtensionContext context) {
        context.registerSubsystem(NAMESPACE, PARSER);
    }

    /**
     * Activate the extension.
     *
     * @param context the service activation context
     */
    public void activate(final ServiceActivatorContext context) {
    }

    static class NamingSubSystemElementParser implements XMLElementReader<List<? super AbstractSubsystemUpdate<NamingSubsystemElement, ?>>> {

        /** {@inheritDocs} */
        public void readElement(XMLExtendedStreamReader reader, List<? super AbstractSubsystemUpdate<NamingSubsystemElement, ?>> updates)
            throws XMLStreamException {

            boolean supportEvents = true;
            boolean bindAppContext = false;
            boolean bindModuleContext = false;
            boolean bindCompContext = false;

            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SUPPORT_EVENTS: {
                        supportEvents = Boolean.parseBoolean(reader.getAttributeValue(i));
                        break;
                    } case BIND_APP_CONTEXT: {
                        bindAppContext = Boolean.parseBoolean(reader.getAttributeValue(i));
                        break;
                    } case BIND_MODULE_CONTEXT: {
                        bindModuleContext = Boolean.parseBoolean(reader.getAttributeValue(i));
                        break;
                    } case BIND_COMP_CONTEXT: {
                        bindCompContext = Boolean.parseBoolean(reader.getAttributeValue(i));
                        break;
                    } default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            ParseUtils.requireNoContent(reader);
            // Add the update
            updates.add(new NamingSubsystemElementUpdate(supportEvents, bindAppContext, bindModuleContext, bindCompContext));
        }
    }

}
