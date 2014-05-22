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

import static org.jboss.as.platform.mbean.PlatformMBeanConstants.MEMORY;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.MEMORY_PATH;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class MemoryResourceDefinition extends SimpleResourceDefinition {

    //metrics
    private static SimpleAttributeDefinition OBJECT_PENDING_FINALIZATION_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.OBJECT_PENDING_FINALIZATION_COUNT, ModelType.INT, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();


    private static SimpleAttributeDefinition HEAP_MEMORY_USAGE = new ObjectTypeAttributeDefinition.Builder(
            PlatformMBeanConstants.HEAP_MEMORY_USAGE,
            PlatformMBeanConstants.MEMORY_INIT,
            PlatformMBeanConstants.MEMORY_USED,
            PlatformMBeanConstants.MEMORY_COMMITTED,
            PlatformMBeanConstants.MEMORY_MAX
    )
            .setStorageRuntime()
            .setAllowNull(false)
            .build();

    private static SimpleAttributeDefinition NON_HEAP_MEMORY_USAGE = new ObjectTypeAttributeDefinition.Builder(
            PlatformMBeanConstants.NON_HEAP_MEMORY_USAGE,
            PlatformMBeanConstants.MEMORY_INIT,
            PlatformMBeanConstants.MEMORY_USED,
            PlatformMBeanConstants.MEMORY_COMMITTED,
            PlatformMBeanConstants.MEMORY_MAX)
            .setStorageRuntime()
            .setAllowNull(false)
            .build();

    private static AttributeDefinition VERBOSE = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.VERBOSE, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    private static final List<SimpleAttributeDefinition> METRICS = Arrays.asList(
            OBJECT_PENDING_FINALIZATION_COUNT,
            HEAP_MEMORY_USAGE,
            NON_HEAP_MEMORY_USAGE
    );
    private static final List<AttributeDefinition> READ_WRITE_ATTRIBUTES = Arrays.asList(
            VERBOSE
    );

    public static final List<String> MEMORY_METRICS = Arrays.asList(
            PlatformMBeanConstants.OBJECT_PENDING_FINALIZATION_COUNT,
            PlatformMBeanConstants.HEAP_MEMORY_USAGE,
            PlatformMBeanConstants.NON_HEAP_MEMORY_USAGE
    );
    public static final List<String> MEMORY_READ_WRITE_ATTRIBUTES = Arrays.asList(
            VERBOSE.getName()
    );

    static final MemoryResourceDefinition INSTANCE = new MemoryResourceDefinition();


    private MemoryResourceDefinition() {
        super(MEMORY_PATH,
                PlatformMBeanUtil.getResolver(MEMORY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, MemoryMXBeanAttributeHandler.INSTANCE);

        for (AttributeDefinition attribute : READ_WRITE_ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, MemoryMXBeanAttributeHandler.INSTANCE, MemoryMXBeanAttributeHandler.INSTANCE);
        }

        for (SimpleAttributeDefinition attribute : METRICS) {
            registration.registerMetric(attribute, MemoryMXBeanAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(MemoryMXBeanGCHandler.DEFINITION, MemoryMXBeanGCHandler.INSTANCE);
    }
}

