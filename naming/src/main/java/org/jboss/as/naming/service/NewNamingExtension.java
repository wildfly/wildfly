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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class NewNamingExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "naming";
    public static final String NAMESPACE = "urn:jboss:domain:naming:1.0";
    private static final NamingSubsystemParser parser = new NamingSubsystemParser();

    /** {@inheritDoc} */
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(NewNamingSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewNamingSubsystemAdd.INSTANCE, NewNamingSubsystemProviders.SUBSYSTEM_ADD, false);
    }

    /** {@inheritDoc} */
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(NAMESPACE, parser, parser);
    }

    static class NamingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

        /** {@inheritDoc} */
        public void writeContent(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
            writer.writeEndElement();
        }

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            ParseUtils.requireNoContent(reader);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(SUBSYSTEM, SUBSYSTEM_NAME);
            list.add(subsystem);
        }
    }

}
