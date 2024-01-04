/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileHealthExtension implements Extension {

    static final String EXTENSION_NAME = "org.wildfly.extension.microprofile.health.smallrye";

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "microprofile-health-smallrye";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, MicroProfileHealthExtension.class);

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    protected static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    protected static final ModelVersion VERSION_3_0_0 = ModelVersion.create(3, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_3_0_0;

    private static final MicroProfileHealthParser_3_0 CURRENT_PARSER = new MicroProfileHealthParser_3_0();

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(CURRENT_PARSER);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new MicroProfileHealthSubsystemDefinition(context.isRuntimeOnlyRegistrationValid()  && context.getRunningMode() == RunningMode.NORMAL));
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MicroProfileHealthParser_1_0.NAMESPACE, MicroProfileHealthParser_1_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MicroProfileHealthParser_2_0.NAMESPACE, MicroProfileHealthParser_2_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MicroProfileHealthParser_3_0.NAMESPACE, CURRENT_PARSER);
    }
}
