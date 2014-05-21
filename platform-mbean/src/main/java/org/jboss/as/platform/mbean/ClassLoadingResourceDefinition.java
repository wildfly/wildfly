package org.jboss.as.platform.mbean;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class ClassLoadingResourceDefinition extends SimpleResourceDefinition {
    //metrics
    private static SimpleAttributeDefinition TOTAL_LOADED_CLASS_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.TOTAL_LOADED_CLASS_COUNT, ModelType.LONG, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();
    private static SimpleAttributeDefinition LOADED_CLASS_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.LOADED_CLASS_COUNT, ModelType.INT, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();
    private static SimpleAttributeDefinition UNLOADED_CLASS_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.UNLOADED_CLASS_COUNT, ModelType.LONG, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();
    //r+w attributes
    private static SimpleAttributeDefinition VERBOSE = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.VERBOSE, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    static final List<String> CLASSLOADING_METRICS = Arrays.asList(
            TOTAL_LOADED_CLASS_COUNT.getName(),
            LOADED_CLASS_COUNT.getName(),
            UNLOADED_CLASS_COUNT.getName()
    );
    static final List<String> CLASSLOADING_READ_WRITE_ATTRIBUTES = Arrays.asList(
            VERBOSE.getName()
    );
    private static final List<SimpleAttributeDefinition> METRICS = Arrays.asList(
            TOTAL_LOADED_CLASS_COUNT,
            LOADED_CLASS_COUNT,
            UNLOADED_CLASS_COUNT
    );
    private static final List<SimpleAttributeDefinition> READ_WRITE_ATTRIBUTES = Arrays.asList(
            VERBOSE
    );
    static final ClassLoadingResourceDefinition INSTANCE = new ClassLoadingResourceDefinition();

    private ClassLoadingResourceDefinition() {
        super(PlatformMBeanConstants.CLASS_LOADING_PATH,
                PlatformMBeanUtil.getResolver(PlatformMBeanConstants.CLASS_LOADING));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, ClassLoadingMXBeanAttributeHandler.INSTANCE);
        }

        for (SimpleAttributeDefinition attribute : METRICS) {
            registration.registerMetric(attribute, ClassLoadingMXBeanAttributeHandler.INSTANCE);
        }

        for (SimpleAttributeDefinition attribute : READ_WRITE_ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, ClassLoadingMXBeanAttributeHandler.INSTANCE, ClassLoadingMXBeanAttributeHandler.INSTANCE);
        }
    }


}

