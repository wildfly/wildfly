package org.wildfly.extension.micrometer.prometheus;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.micrometer.MicrometerConfigurationConstants;
import org.wildfly.extension.micrometer.MicrometerExtension;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

public class PrometheusRegistryDefinition extends SimpleResourceDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement("registry", "prometheus");

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

    public static final AttributeDefinition[] ATTRIBUTES = {CONTEXT, SECURITY_ENABLED};

    public PrometheusRegistryDefinition(WildFlyCompositeRegistry wildFlyRegistry) {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, MicrometerExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new PrometheusRegistryAddHandler(wildFlyRegistry))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null,
                    ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }

    private static class TempWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandback) throws OperationFailedException {
            AttributeDefinition attributeDefinition = context.getResourceRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName).getAttributeDefinition();

            return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, voidHandback);
        }
    }
}
