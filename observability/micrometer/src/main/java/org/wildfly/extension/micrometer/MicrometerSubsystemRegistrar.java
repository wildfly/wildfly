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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.Functions;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

class MicrometerSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar, ResourceServiceConfigurator {
    private static final String MICROMETER_MODULE = "org.wildfly.extension.micrometer";
    private static final String MICROMETER_API_MODULE = "org.wildfly.micrometer.deployment";
    private static final String CAPABILITY_NAME_METRICS = "org.wildfly.extension.metrics.scan";
    private static final String CAPABILITY_NAME_OPENTELEMETRY = "org.wildfly.extension.opentelemetry";

    static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(MicrometerConfigurationConstants.NAME);
    static final ParentResourceDescriptionResolver RESOLVER =
            new SubsystemResourceDescriptionResolver(MicrometerConfigurationConstants.NAME, MicrometerSubsystemRegistrar.class);
    static final ServiceName MICROMETER_SERVICE_SERVICE_NAME =
        ServiceNameFactory.parseServiceName(MICROMETER_MODULE + ".service");
    static final NullaryServiceDescriptor<MicrometerService> SERVICE_DESCRIPTOR =
        NullaryServiceDescriptor.of(MICROMETER_SERVICE_SERVICE_NAME.getCanonicalName(), MicrometerService.class);

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

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent,
                                                   ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration =
            parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(PATH), RESOLVER).build());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
            .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
            .addAttributes(List.of(ENDPOINT, STEP, EXPOSED_SUBSYSTEMS))
            .withDeploymentChainContributor(target -> {
                target.addDeploymentProcessor(MicrometerConfigurationConstants.NAME,
                    DEPENDENCIES,
                    DEPENDENCIES_MICROMETER,
                    new MicrometerDependencyProcessor());
                target.addDeploymentProcessor(MicrometerConfigurationConstants.NAME,
                    POST_MODULE,
                    POST_MODULE_MICROMETER,
                    new MicrometerDeploymentProcessor());
            })
            .withAddOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
            .withRemoveOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
            .build();

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<String> otherMetrics = new ArrayList<>();
        if (context.getCapabilityServiceSupport().hasCapability(CAPABILITY_NAME_METRICS)) {
            otherMetrics.add("WildFly Metrics");
        }
        if (context.getCapabilityServiceSupport().hasCapability(CAPABILITY_NAME_OPENTELEMETRY)) {
            otherMetrics.add("OpenTelemetry Metrics");
        }
        if (!otherMetrics.isEmpty()) {
            if (Boolean.parseBoolean(System.getProperty("wildfly.multiple.metrics.warn", "true"))) {
                MICROMETER_LOGGER.multipleMetricsSystemsEnabled(String.join(",", otherMetrics));
            }
        }

        WildFlyMicrometerConfig micrometerConfig = new WildFlyMicrometerConfig.Builder()
            .endpoint(MicrometerSubsystemRegistrar.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull())
            .step(MicrometerSubsystemRegistrar.STEP.resolveModelAttribute(context, model).asLong())
            .exposedSubsystems(MicrometerSubsystemRegistrar.EXPOSED_SUBSYSTEMS.unwrap(context, model))
            .build();

        ServiceDependency<ModelControllerClientFactory> mccf = ServiceDependency.on(ModelControllerClientFactory.SERVICE_DESCRIPTOR);
        ServiceDependency<Executor> executor = ServiceDependency.on(Capabilities.MANAGEMENT_EXECUTOR);
        ServiceDependency<ProcessStateNotifier> processStateNotifier = ServiceDependency.on(ProcessStateNotifier.SERVICE_DESCRIPTOR);

        Supplier<MicrometerService> serviceSupplier = () ->
            new MicrometerService(micrometerConfig, mccf.get().createClient(executor.get()), processStateNotifier.get());

        AtomicReference<MicrometerService> captor = new AtomicReference<>();

        context.addStep((operationContext, modelNode) -> {
            MicrometerService service = captor.get();
            // Given that this step runs in the VERIFY stage, and our service was started eagerly, the
            // service reference _should_ be non-null.
            if (service != null) {
                service.collectResourceMetrics(context.readResourceFromRoot(EMPTY_ADDRESS),
                    context.getRootResourceRegistration(), Function.identity());
            }
        }, OperationContext.Stage.VERIFY);

        return ServiceInstaller.builder(serviceSupplier)
            .provides(MICROMETER_SERVICE_SERVICE_NAME)
            .requires(List.of(mccf, executor, processStateNotifier))
            .onStart(MicrometerService::start)
            .onStop(Functions.closingConsumer())
            .withCaptor(captor::set) // capture the provided value
            .asActive() // Start actively
            .build();
    }
}
