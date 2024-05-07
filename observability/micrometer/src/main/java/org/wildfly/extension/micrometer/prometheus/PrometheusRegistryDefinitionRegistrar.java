package org.wildfly.extension.micrometer.prometheus;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.micrometer.MicrometerConfigurationConstants;
import org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;

public class PrometheusRegistryDefinitionRegistrar implements ChildResourceDefinitionRegistrar {
    static final String NAME = "prometheus";
    public static final PathElement PATH = PathElement.pathElement("registry", NAME);

    static final RuntimeCapability<Void> MICROMETER_PROMETHEUS_CONTEXT_CAPABILITY =
            RuntimeCapability.Builder.of("org.wildfly.extension.micrometer.prometheus.http-context", Void.class).build();

    static final SimpleAttributeDefinition CONTEXT =
            SimpleAttributeDefinitionBuilder
                    .create(MicrometerConfigurationConstants.CONTEXT, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setRestartAllServices()
                    .build();
    static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder.create("security-enabled", ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(CONTEXT, SECURITY_ENABLED);
    private final ResourceRegistration registration;
    private final ResourceDescriptor descriptor;

    public PrometheusRegistryDefinitionRegistrar() {
        registration = ResourceRegistration.of(PATH);
        descriptor = ResourceDescriptor.builder(MicrometerSubsystemRegistrar.RESOLVER.createChildResolver(PATH))
                .addAttributes(ATTRIBUTES)
                .build();
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDefinition definition = ResourceDefinition.builder(this.registration, this.descriptor.getResourceDescriptionResolver()).build();
        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(this.descriptor).register(registration);
        return registration;
    }

    /*
    public PrometheusRegistryDefinitionRegistrar(WildFlyCompositeRegistry wildFlyRegistry) {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, MicrometerExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new PrometheusRegistryAddHandler(wildFlyRegistry))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
        );
    }
    */

    public Stability getStability() {
        return Stability.COMMUNITY;
    }

    /*
    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null,
                    ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }
    */

    private static class TempWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandback) throws OperationFailedException {
            AttributeDefinition attributeDefinition = context.getResourceRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName).getAttributeDefinition();

            return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, voidHandback);
        }
    }
}
