/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * <p>
 * The IIOP extension implementation.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "iiop-openjdk";

    protected static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME = IIOPExtension.class.getPackage().getName() + ".LocalDescriptions";

    static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(2,1,0);
    static final ModelVersion VERSION_2 = ModelVersion.create(2,0,0);
    static final ModelVersion VERSION_1 = ModelVersion.create(1);


    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(IIOPExtension.SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append(".").append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME,
                IIOPExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(IIOPRootDefinition.INSTANCE);
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(new IIOPSubsystemParser_2_1());

    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,Namespace.IIOP_OPENJDK_1_0.getUriString(), IIOPSubsystemParser_1::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,Namespace.IIOP_OPENJDK_2_0.getUriString(), IIOPSubsystemParser_2_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,Namespace.IIOP_OPENJDK_2_1.getUriString(), IIOPSubsystemParser_2_1::new);
    }

}
