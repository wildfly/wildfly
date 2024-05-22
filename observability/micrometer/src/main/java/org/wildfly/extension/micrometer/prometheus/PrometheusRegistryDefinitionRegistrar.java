package org.wildfly.extension.micrometer.prometheus;

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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.as.version.Stability;
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
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

public class PrometheusRegistryDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {
    public static final String NAME = "prometheus";
    public static final PathElement PATH = PathElement.pathElement("registry", NAME);
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
        ResourceDescriptor descriptor = ResourceDescriptor.builder(MicrometerSubsystemRegistrar.RESOLVER.createChildResolver(PATH))
                .addAttributes(ATTRIBUTES)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();
        ManagementResourceRegistration mrr = parent.registerSubModel(
                ResourceDefinition.builder(ResourceRegistration.of(PATH, Stability.PREVIEW),
                        descriptor.getResourceDescriptionResolver()).build());
        // Temporary null-check. See https://issues.redhat.com/browse/WFCORE-6940
        if (mrr != null) {
            ManagementResourceRegistrar.of(descriptor).register(mrr);
        }

        return mrr;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String serviceContext = CONTEXT.resolveModelAttribute(context, model).asStringOrNull();
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
                .asActive()
                .build();
    }
}