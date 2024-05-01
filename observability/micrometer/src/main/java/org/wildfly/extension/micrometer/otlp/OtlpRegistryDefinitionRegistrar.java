/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.otlp;

import static org.wildfly.extension.micrometer.MicrometerConfigurationConstants.MICROMETER_MODULE;

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
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class OtlpRegistryDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {
    static final String NAME = "otlp";
    public static final PathElement PATH = PathElement.pathElement("registry", NAME);

    static final RuntimeCapability<Void> MICROMETER_OTLP_CONFIG_RUNTIME_CAPABILITY =
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
            .setDefaultValue(new ModelNode(TimeUnit.MINUTES.toSeconds(60)))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(ENDPOINT, STEP);
    private final WildFlyCompositeRegistry wildFlyRegistry;

    public OtlpRegistryDefinitionRegistrar(WildFlyCompositeRegistry wildFlyRegistry) {

        this.wildFlyRegistry = wildFlyRegistry;
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
        String endpoint = OtlpRegistryDefinitionRegistrar.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull();
        long step = OtlpRegistryDefinitionRegistrar.STEP.resolveModelAttribute(context, model).asLong();

        AtomicReference<WildFlyMicrometerConfig> captor = new AtomicReference<>();

        context.addStep((operationContext, modelNode) -> {
            WildFlyMicrometerConfig micrometerConfig = captor.get();
            if (micrometerConfig != null && micrometerConfig.url() != null) {
                wildFlyRegistry.addRegistry(new WildFlyOtlpRegistry(micrometerConfig));
            }
        }, OperationContext.Stage.VERIFY);

        return CapabilityServiceInstaller.builder(MICROMETER_OTLP_CONFIG_RUNTIME_CAPABILITY,
                        () -> new WildFlyMicrometerConfig(endpoint, step))
                .withCaptor(captor::set) // capture the provided value
                .asActive() // Start actively
                .build();
    }
}
