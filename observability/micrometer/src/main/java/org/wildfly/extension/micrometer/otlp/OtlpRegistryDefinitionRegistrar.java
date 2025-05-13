/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.otlp;

import static org.wildfly.extension.micrometer.MicrometerConfigurationConstants.MICROMETER_MODULE;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.micrometer.MicrometerConfigurationConstants;
import org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar;
import org.wildfly.extension.micrometer.WildFlyMicrometerConfig;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;

public class OtlpRegistryDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {
    static final String NAME = "otlp";
    public static final PathElement PATH = PathElement.pathElement("registry", NAME);

    public static final RuntimeCapability<Void> MICROMETER_OTLP_CONFIG_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".wildfly-otlp-config", WildFlyMicrometerConfig.class)
                    .build();

    public static final SimpleAttributeDefinition ENDPOINT = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.ENDPOINT, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition STEP = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.STEP, ModelType.LONG, true)
            .setDefaultValue(new ModelNode(60L))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(ENDPOINT, STEP);
    private final WildFlyCompositeRegistry compositeRegistry;

    public OtlpRegistryDefinitionRegistrar(WildFlyCompositeRegistry compositeRegistry) {
        this.compositeRegistry = compositeRegistry;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceRegistration registration = ResourceRegistration.of(PATH);

        ResourceDescriptor descriptor = ResourceDescriptor.builder(MicrometerSubsystemRegistrar.RESOLVER.createChildResolver(PATH))
                .addAttributes(ATTRIBUTES)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();
        ManagementResourceRegistration mrr = parent.registerSubModel(
                ResourceDefinition.builder(registration, descriptor.getResourceDescriptionResolver()).build());

        ManagementResourceRegistrar.of(descriptor).register(mrr);

        return mrr;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        WildFlyOtlpRegistry.WildFlyMicrometerOtlpConfig otlpConfig = new WildFlyOtlpRegistry.WildFlyMicrometerOtlpConfig.Builder()
            .endpoint(OtlpRegistryDefinitionRegistrar.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull())
            .step(OtlpRegistryDefinitionRegistrar.STEP.resolveModelAttribute(context, model).asLong())
            .build();

        return ServiceInstaller.builder(
                () -> {
                    if (otlpConfig.url() != null) {
                        compositeRegistry.addRegistry(new WildFlyOtlpRegistry(otlpConfig));
                    }
                },
                () -> {
                    // No-op stop task
                }
            )
            .startWhen(StartWhen.INSTALLED)
            .build();
    }
}
