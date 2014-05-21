/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.platform.mbean;


import static org.jboss.as.platform.mbean.PlatformMBeanConstants.NAME;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.VALID;

import java.lang.management.MemoryType;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class MemoryPoolResourceDefinition extends SimpleResourceDefinition {


    private static AttributeDefinition MEMORY_MANAGER_NAMES = new StringListAttributeDefinition.Builder(PlatformMBeanConstants.MEMORY_MANAGER_NAMES)
            .setStorageRuntime()
            .build();

    private static AttributeDefinition TYPE = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.TYPE, ModelType.STRING, false)
            .setValidator(new EnumValidator<MemoryType>(MemoryType.class, false))
            .setStorageRuntime()
            .build();

    private static AttributeDefinition USAGE_THRESHOLD = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.USAGE_THRESHOLD, ModelType.LONG, true)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setValidator(new IntRangeValidator(0))
            .build();

    private static AttributeDefinition USAGE_THRESHOLD_EXCEEDED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.USAGE_THRESHOLD_EXCEEDED, ModelType.BOOLEAN, true)
            .setStorageRuntime()
            .build();

    private static AttributeDefinition USAGE_THRESHOLD_SUPPORTED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.USAGE_THRESHOLD_SUPPORTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    private static AttributeDefinition USAGE_THRESHOLD_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.USAGE_THRESHOLD_COUNT, ModelType.LONG, true)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    private static AttributeDefinition COLLECTION_USAGE_THRESHOLD_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD_COUNT, ModelType.LONG, true)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    private static AttributeDefinition COLLECTION_USAGE_THRESHOLD = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD, ModelType.LONG, true)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setValidator(new IntRangeValidator(0))
            .build();

    private static AttributeDefinition COLLECTION_USAGE_THRESHOLD_SUPPORTED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD_SUPPORTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();
    private static AttributeDefinition COLLECTION_USAGE_THRESHOLD_EXCEEDED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD_EXCEEDED, ModelType.BOOLEAN, true)
            .setStorageRuntime()
            .build();


    private static AttributeDefinition USAGE = new ObjectTypeAttributeDefinition.Builder(
            PlatformMBeanConstants.USAGE,
            PlatformMBeanConstants.MEMORY_INIT,
            PlatformMBeanConstants.MEMORY_USED,
            PlatformMBeanConstants.MEMORY_COMMITTED,
            PlatformMBeanConstants.MEMORY_MAX
    )
            .setStorageRuntime()
            .setAllowNull(false)
            .build();

    private static AttributeDefinition PEAK_USAGE = new ObjectTypeAttributeDefinition.Builder(
            PlatformMBeanConstants.PEAK_USAGE,
            PlatformMBeanConstants.MEMORY_INIT,
            PlatformMBeanConstants.MEMORY_USED,
            PlatformMBeanConstants.MEMORY_COMMITTED,
            PlatformMBeanConstants.MEMORY_MAX)
            .setStorageRuntime()
            .setAllowNull(false)
            .build();

    private static AttributeDefinition COLLECTION_USAGE = new ObjectTypeAttributeDefinition.Builder(
            PlatformMBeanConstants.COLLECTION_USAGE,
            PlatformMBeanConstants.MEMORY_INIT,
            PlatformMBeanConstants.MEMORY_USED,
            PlatformMBeanConstants.MEMORY_COMMITTED,
            PlatformMBeanConstants.MEMORY_MAX)
            .setStorageRuntime()
            .build();


    private static final List<AttributeDefinition> METRICS = Arrays.asList(
            USAGE,
            PEAK_USAGE,
            USAGE_THRESHOLD_EXCEEDED,
            USAGE_THRESHOLD_COUNT,
            COLLECTION_USAGE_THRESHOLD_EXCEEDED,
            COLLECTION_USAGE_THRESHOLD_COUNT,
            COLLECTION_USAGE
    );
    private static final List<AttributeDefinition> READ_WRITE_ATTRIBUTES = Arrays.asList(
            USAGE_THRESHOLD,
            COLLECTION_USAGE_THRESHOLD
    );
    private static final List<AttributeDefinition> READ_ATTRIBUTES = Arrays.asList(
            NAME,
            TYPE,
            VALID,
            MEMORY_MANAGER_NAMES,
            USAGE_THRESHOLD_SUPPORTED,
            COLLECTION_USAGE_THRESHOLD_SUPPORTED
    );


    static final List<String> MEMORY_POOL_METRICS = Arrays.asList(
            USAGE.getName(),
            PEAK_USAGE.getName(),
            USAGE_THRESHOLD_EXCEEDED.getName(),
            USAGE_THRESHOLD_COUNT.getName(),
            COLLECTION_USAGE_THRESHOLD_EXCEEDED.getName(),
            COLLECTION_USAGE_THRESHOLD_COUNT.getName(),
            COLLECTION_USAGE.getName()
    );
    static final List<String> MEMORY_POOL_READ_WRITE_ATTRIBUTES = Arrays.asList(
            USAGE_THRESHOLD.getName(),
            COLLECTION_USAGE_THRESHOLD.getName()
    );

    static final List<String> MEMORY_POOL_READ_ATTRIBUTES = Arrays.asList(
            NAME.getName(),
            TYPE.getName(),
            PlatformMBeanConstants.VALID.getName(),
            MEMORY_MANAGER_NAMES.getName(),
            USAGE_THRESHOLD_SUPPORTED.getName(),
            COLLECTION_USAGE_THRESHOLD_SUPPORTED.getName()
    );


    static final MemoryPoolResourceDefinition INSTANCE = new MemoryPoolResourceDefinition();


    private MemoryPoolResourceDefinition() {
        super(PathElement.pathElement(NAME.getName()),
                PlatformMBeanUtil.getResolver(PlatformMBeanConstants.MEMORY_POOL));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, MemoryMXBeanAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition attribute : READ_WRITE_ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, MemoryPoolMXBeanAttributeHandler.INSTANCE, MemoryPoolMXBeanAttributeHandler.INSTANCE);
        }
        for (AttributeDefinition attribute : READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, MemoryPoolMXBeanAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition attribute : METRICS) {
            registration.registerMetric(attribute, MemoryPoolMXBeanAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(ReadResourceHandler.DEFINITION, MemoryPoolMXBeanReadResourceHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(MemoryPoolMXBeanResetPeakUsageHandler.DEFINITION, MemoryPoolMXBeanResetPeakUsageHandler.INSTANCE);
    }
}

