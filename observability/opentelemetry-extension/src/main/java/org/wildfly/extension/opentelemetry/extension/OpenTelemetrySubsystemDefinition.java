package org.wildfly.extension.opentelemetry.extension;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants;

/*
 * For future reference: https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#jaeger-exporter
 */

public class OpenTelemetrySubsystemDefinition extends PersistentResourceDefinition {

    private static final String[] ALLOWED_EXPORTERS = {"jaeger", "otlp"};
    private static final String[] ALLOWED_SPAN_PROCESSORS = {"batch", "simple"};

    public static final String[] MODULES = {
            "org.wildfly.extension.opentelemetry-extension"
    };

    public static final String[] EXPORTED_MODULES = {
            "io.opentelemetry.api",
            "io.opentelemetry.context",
            "org.wildfly.extension.opentelemetry-api"
    };

    public static final SimpleAttributeDefinition SERVICE_NAME = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SERVICE_NAME, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition EXPORTER = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.EXPORTER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAttributeGroup("exporter")
            .setDefaultValue(new ModelNode("jaeger"))
            .setAllowedValues(ALLOWED_EXPORTERS)
            .setValidator(new StringAllowedValuesValidator(ALLOWED_EXPORTERS))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition ENDPOINT = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.ENDPOINT, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAttributeGroup("exporter")
            .setRestartAllServices()
            .setDefaultValue(new ModelNode("http://localhost:14250"))
            .build();

    public static final SimpleAttributeDefinition SPAN_PROCESSOR = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SPAN_PROCESSOR, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAttributeGroup("span-processor")
            .setRestartAllServices()
            .setDefaultValue(new ModelNode("batch"))
            .setAllowedValues(ALLOWED_SPAN_PROCESSORS)
            .setValidator(new StringAllowedValuesValidator(ALLOWED_SPAN_PROCESSORS))
            .build();

    public static final SimpleAttributeDefinition BATCH_DELAY = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.BATCH_DELAY, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup("span-processor")
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(5000))
            .build();

    public static final SimpleAttributeDefinition MAX_QUEUE_SIZE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.MAX_QUEUE_SIZE, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup("span-processor")
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(2048))
            .build();

    public static final SimpleAttributeDefinition MAX_EXPORT_BATCH_SIZE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.MAX_EXPORT_BATCH_SIZE, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup("span-processor")
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(512))
            .build();

    public static final SimpleAttributeDefinition EXPORT_TIMEOUT = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.EXPORT_TIMEOUT, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup("span-processor")
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(30000))
            .build();

    public static final SimpleAttributeDefinition SAMPLER = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SAMPLER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAttributeGroup("sampler")
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition SAMPLER_ARG = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SAMPLER_ARG, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAttributeGroup("sampler")
            .setRestartAllServices()
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {
            SERVICE_NAME, EXPORTER, ENDPOINT, SPAN_PROCESSOR, BATCH_DELAY, MAX_QUEUE_SIZE, MAX_EXPORT_BATCH_SIZE,
            EXPORT_TIMEOUT, SAMPLER, SAMPLER_ARG
    };
    public static final OpenTelemetrySubsystemDefinition INSTANCE = new OpenTelemetrySubsystemDefinition();

    protected OpenTelemetrySubsystemDefinition() {

        super(new SimpleResourceDefinition.Parameters(OpenTelemetrySubsystemExtension.SUBSYSTEM_PATH,
                OpenTelemetrySubsystemExtension.getResourceDescriptionResolver(OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME))
                .setAddHandler(OpenTelemetrySubsystemAdd.INSTANCE)
                .setRemoveHandler(OpenTelemetrySubsystemRemove.INSTANCE));

    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }
}
