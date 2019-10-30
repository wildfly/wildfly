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

package org.jboss.as.weld;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;


import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.weld.logging.WeldLogger;

/**
 * Domain extension used to initialize the weld subsystem.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 * @author Jozef Hartinger
 */
public class WeldExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "weld";

    static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);


    private static final String RESOURCE_NAME = WeldExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(4, 0, 0);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, WeldExtension.class.getClassLoader(), true, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(final ExtensionContext context) {
        WeldLogger.ROOT_LOGGER.debug("Activating Weld Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WeldResourceDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(WeldSubsystem40Parser.INSTANCE);

    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem10Parser.NAMESPACE, () -> WeldSubsystem10Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem20Parser.NAMESPACE, () -> WeldSubsystem20Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem30Parser.NAMESPACE, () -> WeldSubsystem30Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem40Parser.NAMESPACE, () -> WeldSubsystem40Parser.INSTANCE);
    }

}
