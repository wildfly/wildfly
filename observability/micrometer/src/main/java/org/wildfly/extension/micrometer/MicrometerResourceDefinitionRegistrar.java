/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;

public class MicrometerResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {
    private static final String MICROMETER_MODULE = "org.wildfly.extension.micrometer";
    private static final String MICROMETER_API_MODULE = "org.wildfly.micrometer.deployment";
    static final String CLIENT_FACTORY_CAPABILITY = "org.wildfly.management.model-controller-client-factory";
    static final String MANAGEMENT_EXECUTOR = "org.wildfly.management.executor";
    static final String PROCESS_STATE_NOTIFIER = "org.wildfly.management.process-state-notifier";
    public static final String COLLECTOR_NAME = MICROMETER_MODULE + ".wildfly-collector";
    public static final String WILDFLY_REGISTRY_NAME = MICROMETER_MODULE + ".registry";

    public static final RuntimeCapability<Void> MICROMETER_COLLECTOR_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(COLLECTOR_NAME, MicrometerCollector.class)
                    .addRequirements(CLIENT_FACTORY_CAPABILITY, MANAGEMENT_EXECUTOR, PROCESS_STATE_NOTIFIER)
                    .build();
    public static final RuntimeCapability<Void> MICROMETER_REGISTRY_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(WILDFLY_REGISTRY_NAME, WildFlyRegistry.class)
                    .build();

    @Deprecated
    static final SimpleAttributeDefinition ENDPOINT = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.ENDPOINT, ModelType.STRING)
            .setAttributeGroup(MicrometerConfigurationConstants.OTLP_REGISTRY)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    @Deprecated
    static final SimpleAttributeDefinition STEP = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.STEP, ModelType.LONG, true)
            .setAttributeGroup(MicrometerConfigurationConstants.OTLP_REGISTRY)
            .setDefaultValue(new ModelNode(TimeUnit.MINUTES.toSeconds(1)))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final StringListAttributeDefinition EXPOSED_SUBSYSTEMS =
            new StringListAttributeDefinition.Builder("exposed-subsystems")
                    .setDefaultValue(ModelNode.fromJSONString("[\"*\"]"))
                    .setRequired(false)
                    .setRestartAllServices()
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = {
            ENDPOINT, STEP, EXPOSED_SUBSYSTEMS
    };
    private WildFlyCompositeRegistry wildFlyRegistry;
    static final ParentResourceDescriptionResolver RESOLVER =
            new SubsystemResourceDescriptionResolver(MicrometerConfigurationConstants.NAME,
                    MicrometerResourceDefinitionRegistrar.class);

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent,
                                                   ManagementResourceRegistrationContext context) {
        ResourceDescriptor descriptor =
                ResourceDescriptor.builder(RESOLVER.createChildResolver(MicrometerSubsystemRegistrar.PATH))
                        .addCapability(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY)
                        .addCapability(MICROMETER_REGISTRY_RUNTIME_CAPABILITY)
                        .build();
        ResourceDefinition definition = ResourceDefinition.builder(
                        ResourceRegistration.of(MicrometerSubsystemRegistrar.PATH),
                        descriptor.getResourceDescriptionResolver())
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(definition);
        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }
    /*
    protected MicrometerResourceDefinitionRegistrar(WildFlyCompositeRegistry wildFlyRegistry) {
        super(new SimpleResourceDefinition.Parameters(MicrometerExtension.SUBSYSTEM_PATH,
                MicrometerExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new MicrometerSubsystemAdd(wildFlyRegistry))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE));
        this.wildFlyRegistry = wildFlyRegistry;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new OtlpRegistryDefinitionRegistrar(wildFlyRegistry));
        resourceRegistration.registerSubModel(new PrometheusRegistryDefinitionRegistrar(wildFlyRegistry));
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(
                RuntimePackageDependency.required("io.micrometer" )
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null,
                    ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }
    */
}
