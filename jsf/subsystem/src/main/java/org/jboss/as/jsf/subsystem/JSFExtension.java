/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jsf.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.jsf.logging.JSFLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Domain extension used to initialize the jsf subsystem.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 * @author Stan Silvert
 */
public class JSFExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jsf";
    public static final String NAMESPACE = "urn:jboss:domain:jsf:1.0";

    private static final JSFSubsystemParser PARSER = new JSFSubsystemParser();
    static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);


    private static final String RESOURCE_NAME = JSFExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JSFExtension.class.getClassLoader(), true, false);
    }

    private static final ResourceDefinition JSF_SUBSYSTEM_RESOURCE = new JSFResourceDefinition();

    /** {@inheritDoc} */
    @Override
    public void initialize(final ExtensionContext context) {
        JSFLogger.ROOT_LOGGER.debug("Activating JSF(Mojarra) Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerSubsystemModel(JSF_SUBSYSTEM_RESOURCE);
        subsystem.registerXMLElementWriter(PARSER);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JSFExtension.NAMESPACE, PARSER);
    }

    static class JSFSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
            ModelNode addJSFSub = Util.createAddOperation(PathAddress.pathAddress(PATH_SUBSYSTEM));
            for (int i=0; i < reader.getAttributeCount(); i++) {
                if (!reader.getAttributeLocalName(i).equals(JSFResourceDefinition.DEFAULT_SLOT_ATTR_NAME)) continue;
                JSFResourceDefinition.DEFAULT_JSF_IMPL_SLOT.parseAndSetParameter(reader.getAttributeValue(i), addJSFSub, reader);
            }
            list.add(addJSFSub);
            reader.nextTag();
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(JSFExtension.NAMESPACE, false);
            JSFResourceDefinition.DEFAULT_JSF_IMPL_SLOT.marshallAsAttribute(context.getModelNode(), writer);
            writer.writeEndElement();
        }

    }

}
