/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(5, 0, 0);

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
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new WeldResourceDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(WeldSubsystem50Parser.INSTANCE);

    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem10Parser.NAMESPACE, () -> WeldSubsystem10Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem20Parser.NAMESPACE, () -> WeldSubsystem20Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem30Parser.NAMESPACE, () -> WeldSubsystem30Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem40Parser.NAMESPACE, () -> WeldSubsystem40Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WeldSubsystem50Parser.NAMESPACE, () -> WeldSubsystem50Parser.INSTANCE);
    }

}
