package org.wildfly.extension.micrometer.otlp;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.micrometer.MicrometerConfigurationConstants;
import org.wildfly.extension.micrometer.MicrometerExtension;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

public class OtlpRegistryDefinition extends SimpleResourceDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement("registry", "otlp");


    public static final SimpleAttributeDefinition ENDPOINT =
            SimpleAttributeDefinitionBuilder
                    .create(MicrometerConfigurationConstants.ENDPOINT, ModelType.STRING)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setRestartAllServices()
                    .build();

    public static final SimpleAttributeDefinition STEP = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.STEP, ModelType.LONG, true)
            .setDefaultValue(new ModelNode(TimeUnit.MINUTES.toSeconds(1)))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { ENDPOINT, STEP };

    public OtlpRegistryDefinition(WildFlyCompositeRegistry wildFlyRegistry) {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, MicrometerExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new OtlpRegistryAddHandler(wildFlyRegistry))
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
}
