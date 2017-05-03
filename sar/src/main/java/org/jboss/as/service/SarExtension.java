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

package org.jboss.as.service;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Extension used to enable SAR deployments.
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 */
public class SarExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:sar:1.0";
    public static final String SUBSYSTEM_NAME = "sar";

    private static final SarSubsystemParser parser = new SarSubsystemParser();

    private static final String RESOURCE_NAME = SarExtension.class.getPackage().getName() + ".LocalDescriptions";
    private static final ResourceDescriptionResolver RESOLVER = new StandardResourceDescriptionResolver("sar", RESOURCE_NAME, SarExtension.class.getClassLoader());
    private static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SarExtension.SUBSYSTEM_NAME);

    static final String JMX_CAPABILITY = "org.wildfly.management.jmx";
    static final RuntimeCapability<Void> SAR_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.sar-deployment")
            .addRequirements(JMX_CAPABILITY)
            .build();

    private static final ResourceDefinition RESOURCE_DEFINITION = new SimpleResourceDefinition(
                new SimpleResourceDefinition.Parameters(PATH, RESOLVER)
            .setAddHandler(SarSubsystemAdd.INSTANCE)
            .setRemoveHandler(new ReloadRequiredRemoveStepHandler(SAR_CAPABILITY))
            .setCapabilities(SAR_CAPABILITY)
    );

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 0, 0);

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(RESOURCE_DEFINITION);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(parser);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, () -> parser);
    }

    private static ModelNode createAddOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }

    static class SarSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
            //context.startSubsystemElement(NewSarExtension.NAMESPACE, true);
            context.startSubsystemElement(SarExtension.NAMESPACE, false);
            writer.writeEndElement();
}

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // Require no content
            ParseUtils.requireNoContent(reader);
            list.add(createAddOperation());
        }

    }
}
