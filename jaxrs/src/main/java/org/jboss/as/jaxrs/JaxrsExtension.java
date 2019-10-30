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

package org.jboss.as.jaxrs;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Domain extension used to initialize the jaxrs subsystem.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 */
public class JaxrsExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jaxrs";
    public static final String NAMESPACE = "urn:jboss:domain:jaxrs:1.0";

    public static final ModelVersion MODEL_VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    public static final ModelVersion MODEL_VERSION_2_0_0 = ModelVersion.create(2, 0, 0);

    private static final ModelVersion CURRENT_MODEL_VERSION = MODEL_VERSION_2_0_0;

    private static final JaxrsSubsystemParser parser = new JaxrsSubsystemParser();

    private static final String RESOURCE_NAME = JaxrsExtension.class.getPackage().getName() + ".LocalDescriptions";
    static PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    static ResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JaxrsExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final ExtensionContext context) {
        JAXRS_LOGGER.debug("Activating JAX-RS Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(JaxrsSubsystemDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        ManagementResourceRegistration jaxrsResReg = subsystem.registerDeploymentModel(JaxrsDeploymentDefinition.INSTANCE);
        jaxrsResReg.registerSubModel(DeploymentRestResourcesDefintion.INSTANCE);
        subsystem.registerXMLElementWriter(parser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JaxrsExtension.NAMESPACE, () -> parser);
    }

    static class JaxrsSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
            // Require no attributes or content
            requireNoAttributes(reader);
            requireNoContent(reader);
            list.add(Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM_PATH)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter streamWriter, final SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(JaxrsExtension.NAMESPACE, true);
        }

    }
}
