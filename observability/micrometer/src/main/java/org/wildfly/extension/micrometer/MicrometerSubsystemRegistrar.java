/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_MICROMETER;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;
import static org.jboss.as.server.deployment.Phase.POST_MODULE_MICROMETER;
import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.registry.NoOpRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyOtlpRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

class MicrometerSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar, ResourceServiceConfigurator {
    private static final String MICROMETER_MODULE = "org.wildfly.extension.micrometer";
    private static final String MICROMETER_API_MODULE = "org.wildfly.micrometer.deployment";

    static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(MicrometerConfigurationConstants.NAME);
    public static final ParentResourceDescriptionResolver RESOLVER =
            new SubsystemResourceDescriptionResolver(MicrometerConfigurationConstants.NAME, MicrometerSubsystemRegistrar.class);

    static final RuntimeCapability<Void> MICROMETER_COLLECTOR_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".wildfly-collector", MicrometerCollector.class)
                    .addRequirements(ModelControllerClientFactory.SERVICE_DESCRIPTOR, Capabilities.MANAGEMENT_EXECUTOR, ProcessStateNotifier.SERVICE_DESCRIPTOR)
                    .build();

    static final String[] EXPORTED_MODULES = {
            MICROMETER_API_MODULE,
            "io.opentelemetry.otlp",
            "io.micrometer"
    };

    public static final SimpleAttributeDefinition ENDPOINT = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.ENDPOINT, ModelType.STRING)
            .setAttributeGroup(MicrometerConfigurationConstants.OTLP_REGISTRY)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition STEP = SimpleAttributeDefinitionBuilder
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
            EXPOSED_SUBSYSTEMS,
            ENDPOINT,
            STEP
    };

    private final AtomicReference<MicrometerDeploymentConfiguration> deploymentConfig = new AtomicReference<>();

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent,
                                                   ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration =
                parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(PATH), RESOLVER).build());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .addCapability(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY)
                .addAttributes(List.of(ENDPOINT, STEP, EXPOSED_SUBSYSTEMS))
                .withDeploymentChainContributor(target -> {
                    target.addDeploymentProcessor(MicrometerConfigurationConstants.NAME, DEPENDENCIES, DEPENDENCIES_MICROMETER,
                            new MicrometerDependencyProcessor());
                    target.addDeploymentProcessor(MicrometerConfigurationConstants.NAME, POST_MODULE, POST_MODULE_MICROMETER,
                            new MicrometerDeploymentProcessor(deploymentConfig.get()));
                })
                .withAddOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .withRemoveOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .build();

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<String> exposedSubsystems = MicrometerSubsystemRegistrar.EXPOSED_SUBSYSTEMS.unwrap(context, model);
        boolean exposeAnySubsystem = exposedSubsystems.remove("*");
        String endpoint = MicrometerSubsystemRegistrar.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull();
        Long step = MicrometerSubsystemRegistrar.STEP.resolveModelAttribute(context, model).asLong();

        WildFlyRegistry wildFlyRegistry = endpoint != null ?
                new WildFlyOtlpRegistry(new WildFlyMicrometerConfig(endpoint, step)) :
                new NoOpRegistry();

        try {
            new JmxMicrometerCollector(wildFlyRegistry).init();
        } catch (IOException e) {
            throw MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }

        ServiceDependency<ModelControllerClientFactory> mccf = ServiceDependency.on(ModelControllerClientFactory.SERVICE_DESCRIPTOR);
        ServiceDependency<Executor> executor = ServiceDependency.on(Capabilities.MANAGEMENT_EXECUTOR);
        ServiceDependency<ProcessStateNotifier> processStateNotifier = ServiceDependency.on(ProcessStateNotifier.SERVICE_DESCRIPTOR);

        Supplier<MicrometerCollector> collectorSupplier = () ->
                new MicrometerCollector(mccf.get().createClient(executor.get()), processStateNotifier.get(), wildFlyRegistry);

        deploymentConfig.set(new MicrometerDeploymentConfiguration() {
            @Override
            public WildFlyRegistry getRegistry() {
                return wildFlyRegistry;
            }

            @Override
            public Predicate<String> getSubsystemFilter() {
                return subsystem -> exposeAnySubsystem || exposedSubsystems.contains(subsystem);
            }
        });

        AtomicReference<MicrometerCollector> captor = new AtomicReference<>();

        context.addStep((operationContext, modelNode) -> {
            MicrometerCollector collector = captor.get();
            // Given that this step runs in the VERIFY stage, and our collector service was started eagerly, the
            // collector reference _should_ be non-null.
            if (collector != null) {
                ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
                Resource rootResource = context.readResourceFromRoot(EMPTY_ADDRESS);

                collector.collectResourceMetrics(rootResource, rootResourceRegistration,
                        Function.identity(), deploymentConfig.get().getSubsystemFilter());
            }
        }, OperationContext.Stage.VERIFY);

        return CapabilityServiceInstaller.builder(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY, collectorSupplier)
            .requires(List.of(mccf, executor, processStateNotifier))
            .withCaptor(captor::set) // capture the provided value
            .onStop(Functions.closingConsumer())
            .asActive() // Start actively
            .build();
    }

    public interface MicrometerDeploymentConfiguration {
        WildFlyRegistry getRegistry();
        Predicate<String> getSubsystemFilter();
    }
}
