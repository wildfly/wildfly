package org.jboss.as.platform.mbean;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class CompilationResourceDefinition extends SimpleResourceDefinition {
    //metrics
    static SimpleAttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.NAME, ModelType.STRING, false)
            .setStorageRuntime()
            .build();
    static SimpleAttributeDefinition COMPILATION_TIME_MONITORING_SUPPORTED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.COMPILATION_TIME_MONITORING_SUPPORTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();
    static SimpleAttributeDefinition TOTAL_COMPILATION_TIME = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.TOTAL_COMPILATION_TIME, ModelType.LONG, true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setStorageRuntime()
            .build();
    protected static final List<String> COMPILATION_READ_ATTRIBUTES = Arrays.asList(
            NAME.getName(),
            COMPILATION_TIME_MONITORING_SUPPORTED.getName()
    );
    protected static final List<String> COMPILATION_METRICS = Arrays.asList(
            TOTAL_COMPILATION_TIME.getName()
    );

    private static final List<SimpleAttributeDefinition> READ_ATTRIBUTES = Arrays.asList(
            NAME,
            COMPILATION_TIME_MONITORING_SUPPORTED
    );
    private static final List<SimpleAttributeDefinition> METRICS = Arrays.asList(
            TOTAL_COMPILATION_TIME
    );

    static final CompilationResourceDefinition INSTANCE = new CompilationResourceDefinition();

    private CompilationResourceDefinition() {
        super(PlatformMBeanConstants.COMPILATION_PATH,
                PlatformMBeanUtil.getResolver(PlatformMBeanConstants.COMPILATION));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, CompilationMXBeanAttributeHandler.INSTANCE);
        }

        for (SimpleAttributeDefinition attribute : READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, CompilationMXBeanAttributeHandler.INSTANCE);
        }

        for (SimpleAttributeDefinition attribute : METRICS) {
            registration.registerMetric(attribute, CompilationMXBeanAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(ReadResourceHandler.DEFINITION, CompilationMXBeanReadResourceHandler.INSTANCE);
    }
}

