/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.opentracing;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

public class SubsystemDefinition extends ModelOnlyResourceDefinition {
    private static final String OPENTRACING_CAPABILITY_NAME = "org.wildfly.microprofile.opentracing";
    private static final String TRACER_CAPABILITY_NAME = "org.wildfly.microprofile.opentracing.tracer";
    static final String MICROPROFILE_CONFIG_CAPABILITY_NAME = "org.wildfly.microprofile.config";
    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
    public static final String DEFAULT_TRACER_NAME = "default-tracer";

    public static final RuntimeCapability<Void> OPENTRACING_CAPABILITY = RuntimeCapability.Builder
        .of(OPENTRACING_CAPABILITY_NAME)
        .addRequirements(WELD_CAPABILITY_NAME, MICROPROFILE_CONFIG_CAPABILITY_NAME)
        .build();
    public static final RuntimeCapability<Void> TRACER_CAPABILITY = RuntimeCapability.Builder
            .of(TRACER_CAPABILITY_NAME, true)
            .build();
    public static final SimpleAttributeDefinition DEFAULT_TRACER = SimpleAttributeDefinitionBuilder
            .create(DEFAULT_TRACER_NAME, ModelType.STRING, true)
            .setCapabilityReference(TRACER_CAPABILITY_NAME)
            .setRestartAllServices()
            .build();
    static final AttributeDefinition[] ATTRIBUTES = {DEFAULT_TRACER};

    protected SubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(SubsystemExtension.SUBSYSTEM_PATH,
                SubsystemExtension.getResourceDescriptionResolver())
                .setAddHandler(new ModelOnlyAddStepHandler(ATTRIBUTES))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(OPENTRACING_CAPABILITY)
        );
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new JaegerTracerConfigurationDefinition());
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        MigrateOperation.registerOperations(resourceRegistration, getResourceDescriptionResolver());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(DEFAULT_TRACER, null,
                new ModelOnlyWriteAttributeHandler(DEFAULT_TRACER));
    }
}
