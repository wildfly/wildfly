package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_MICROMETER;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;
import static org.jboss.as.server.deployment.Phase.POST_MODULE_MICROMETER;
import static org.wildfly.extension.micrometer.MicrometerResourceDefinitionRegistrar.PROCESS_STATE_NOTIFIER;
import static org.wildfly.extension.micrometer.MicrometerResourceDefinitionRegistrar.WILDFLY_REGISTRY_NAME;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;
import org.wildfly.extension.micrometer.service.MicrometerCollectorService;
import org.wildfly.extension.micrometer.service.MicrometerCollectorService.MicrometerCollectorSupplier;
import org.wildfly.extension.micrometer.service.MicrometerDeploymentService;
import org.wildfly.extension.micrometer.service.MicrometerRegistryService;
import org.wildfly.subsystem.resource.AttributeTranslation;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

public class MicrometerSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {
    private static final String MICROMETER_MODULE = "org.wildfly.extension.micrometer";
    static final String CLIENT_FACTORY_CAPABILITY = "org.wildfly.management.model-controller-client-factory";
    static final String MANAGEMENT_EXECUTOR = "org.wildfly.management.executor";

    static final String NAME = "micrometer";
    static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(NAME);
    public static final ParentResourceDescriptionResolver RESOLVER =
            new SubsystemResourceDescriptionResolver(NAME, MicrometerSubsystemRegistrar.class);

    public static final RuntimeCapability<Void> MICROMETER_COLLECTOR_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".wildfly-collector", MicrometerCollector.class)
                    .addRequirements(CLIENT_FACTORY_CAPABILITY, MANAGEMENT_EXECUTOR, PROCESS_STATE_NOTIFIER)
                    .build();
    public static final RuntimeCapability<Void> MICROMETER_REGISTRY_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".registry", WildFlyRegistry.class)
                    .build();
    public static final ServiceName MICROMETER_COLLECTOR = MICROMETER_COLLECTOR_RUNTIME_CAPABILITY.getCapabilityServiceName();

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

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent,
                                                   ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration =
                parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(PATH), RESOLVER).build());
        UnaryOperator<PathAddress> translator = pathElements -> {
            System.out.println("hi");
            return null;
        };
        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .addCapability(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY)
                .addCapability(MICROMETER_REGISTRY_RUNTIME_CAPABILITY)
                .addAttributes(List.of(ENDPOINT, STEP, EXPOSED_SUBSYSTEMS))
                .translateAttribute(ENDPOINT, AttributeTranslation.relocate(ENDPOINT, translator))
                .withAddResourceOperationTransformation(new TranslateOtlpHandler())
                .withRuntimeHandler(new CollectorServiceConfigurator()) // Install services
                .withDeploymentChainContributor(new Contributor()) // Register DUPs
                .build();
        ManagementResourceRegistrar.of(descriptor).register(registration);
        new OtlpRegistryDefinitionRegistrar().register(registration, context);
        return registration;
    }

    private static class Contributor implements Consumer<DeploymentProcessorTarget> {

        @Override
        public void accept(DeploymentProcessorTarget target) {
            target.addDeploymentProcessor(MicrometerSubsystemRegistrar.NAME, DEPENDENCIES, DEPENDENCIES_MICROMETER,
                    new MicrometerDependencyProcessor());
            target.addDeploymentProcessor(MicrometerSubsystemRegistrar.NAME, POST_MODULE, POST_MODULE_MICROMETER,
                    new MicrometerDeploymentProcessor(exposeAnySubsystem, exposedSubsystems, wildFlyRegistry));
        }
    }

    private class CollectorServiceConfigurator implements ResourceOperationRuntimeHandler {

        @Override
        public void addRuntime(OperationContext context, ModelNode model) throws OperationFailedException {
            installMicrometerRegistryService(context, model);
            installCollectorService(context, model);
            installDeploymentService(context, model);
        }

        private void installMicrometerRegistryService(OperationContext context, ModelNode model) {
            MicrometerRegistryService registryService = new MicrometerRegistryService();
            context.attach(MicrometerRegistryService.CONFIGURATION_KEY, registryService);
            context.getCapabilityServiceTarget()
                    .addCapability(MICROMETER_REGISTRY_RUNTIME_CAPABILITY)
                    .setInstance(registryService)
                    .install();
        }

        private void installCollectorService(OperationContext context, ModelNode model) {
            CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget()
                    .addCapability(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY);

            MicrometerCollectorService service = new MicrometerCollectorService(
                    serviceBuilder.requiresCapability(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class),
                    serviceBuilder.requiresCapability(MANAGEMENT_EXECUTOR, Executor.class),
                    serviceBuilder.requiresCapability(PROCESS_STATE_NOTIFIER, ProcessStateNotifier.class),
                    serviceBuilder.requiresCapability(WILDFLY_REGISTRY_NAME, WildFlyCompositeRegistry.class),
                    new MicrometerCollectorSupplier(serviceBuilder.provides(MICROMETER_COLLECTOR)));
            serviceBuilder.setInstance(service).install();
        }

        private void installDeploymentService(OperationContext context, ModelNode model) {

            ServiceBuilder<?> sb = context.getCapabilityServiceTarget()
                    .addService(ServiceName.of("micrometer-metrics-scanner"));

            model.get(EXPOSED_SUBSYSTEMS.getName()).asString();

            List<String> exposedSubsystems = model.get(EXPOSED_SUBSYSTEMS.getName()).asList()
                    .stream().map(ModelNode::asString)
                    .collect(Collectors.toList());
            boolean exposeAnySubsystem = exposedSubsystems.remove("*");

            sb.setInstance(new MicrometerDeploymentService(
                            sb.requires(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY.getCapabilityServiceName()),
                            sb.requires(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName()),
                            exposeAnySubsystem, exposedSubsystems))
                    .install();
        }

        private PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
            if (deploymentUnit.getParent() == null) {
                return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
            } else {
                return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
            }
        }

        @Override
        public void removeRuntime(OperationContext operationContext, ModelNode modelNode) throws OperationFailedException {

        }
    }

    private static class TranslateOtlpHandler implements UnaryOperator<OperationStepHandler> {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return (context, operation) -> {
                if (operation.hasDefined(ENDPOINT.getName()) || operation.hasDefined(STEP.getName())) {
                    PathAddress address = context.getCurrentAddress();
                    ModelNode endpoint = operation.remove(ENDPOINT.getName());
                    ModelNode step = operation.remove(STEP.getName());

                    ModelNode otlpOperation = Util.createAddOperation(address.append(OtlpRegistryDefinitionRegistrar.PATH));
                    OperationEntry addOperationEntry = context.getResourceRegistration().getOperationEntry(
                            PathAddress.pathAddress(PathElement.pathElement(PathElement.WILDCARD_VALUE)),
                            ModelDescriptionConstants.ADD);
                    for (AttributeDefinition attribute : addOperationEntry.getOperationDefinition().getParameters()) {
                        String name = attribute.getName();
                        if (endpoint.hasDefined(name)) {
                            otlpOperation.get(name).set(endpoint.get(name));
                        }
                        if (step.hasDefined(name)) {
                            otlpOperation.get(name).set(step.get(name));
                        }
                    }
                    context.addStep(otlpOperation, addOperationEntry.getOperationHandler(), OperationContext.Stage.MODEL);

                }
                handler.execute(context, operation);
            };
        }
    }
}
