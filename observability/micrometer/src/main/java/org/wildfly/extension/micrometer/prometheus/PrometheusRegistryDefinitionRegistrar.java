/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.micrometer.MicrometerConfigurationConstants;
import org.wildfly.extension.micrometer.MicrometerExtensionLogger;
import org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

public class PrometheusRegistryDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {
    private static final Stability FEATURE_STABILITY = Stability.DEFAULT;

    public static final String NAME = "prometheus";
    public static final PathElement PATH = PathElement.pathElement("registry", NAME);
    public static final ResourceRegistration RESOURCE_REGISTRATION = ResourceRegistration.of(PATH, FEATURE_STABILITY);

    public static final String HTTP_EXTENSIBILITY_CAPABILITY = "org.wildfly.management.http.extensible";
    public static final SimpleAttributeDefinition CONTEXT = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.CONTEXT, ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.SECURITY_ENABLED, ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();
    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(CONTEXT, SECURITY_ENABLED);

    private final WildFlyCompositeRegistry wildFlyRegistry;

    public PrometheusRegistryDefinitionRegistrar(WildFlyCompositeRegistry wildFlyRegistry) {
        this.wildFlyRegistry = wildFlyRegistry;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {

        ResourceDescriptor description = ResourceDescriptor.builder(MicrometerSubsystemRegistrar.RESOLVER.createChildResolver(PATH))
            .addAttributes(ATTRIBUTES)
            .withOperationTransformation(ModelDescriptionConstants.ADD, new AddHandler())
            .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
            .build();
        ManagementResourceRegistration resourceRegistration = parent.registerSubModel(
                ResourceDefinition.builder(RESOURCE_REGISTRATION, description.getResourceDescriptionResolver()).build());
        if (resourceRegistration != null) {
            resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required("io.prometheus"));
        }
        ManagementResourceRegistrar.of(description).register(resourceRegistration);

        return resourceRegistration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String serviceContext = CONTEXT.resolveModelAttribute(context, model).asString();
        boolean securityEnabled = SECURITY_ENABLED.resolveModelAttribute(context, model).asBoolean();

        return ServiceInstaller.builder(ServiceDependency.on(HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class))
                .onStart(ehm -> {
                    WildFlyPrometheusRegistry prometheusRegistry = new WildFlyPrometheusRegistry();
                    wildFlyRegistry.add(prometheusRegistry);
                    ehm.addManagementHandler(serviceContext, securityEnabled,
                            exchange -> {
                                exchange.getResponseSender().send(prometheusRegistry.scrape());
                            }
                    );
                })
                .startWhen(StartWhen.INSTALLED)
                .build();
    }

    private static class AddHandler implements UnaryOperator<OperationStepHandler> {
        @Override
        public OperationStepHandler apply(OperationStepHandler operationStepHandler) {
            return (operationContext, operation) -> {
                if (operationContext.getProcessType().isHostController()) {
                    throw MicrometerExtensionLogger.MICROMETER_LOGGER.prometheusNotSupportedOnHostControllers();
                }
                operationStepHandler.execute(operationContext, operation);

                ModelNode model = operationContext.readResource(PathAddress.EMPTY_ADDRESS, false).getModel();
                String context = CONTEXT.resolveModelAttribute(operationContext, model).asString();

                if (context.startsWith("/")) {
                    context = context.substring(1);
                }

                operationContext.registerCapability(
                    RuntimeCapability.Builder.of("org.wildfly.management.context", true).build()
                        .fromBaseCapability(context)
                );

            };
        }
    }
}
