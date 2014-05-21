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

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class GarbageCollectorResourceDefinition extends SimpleResourceDefinition {
    //metrics
    private static SimpleAttributeDefinition COLLECTION_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.COLLECTION_COUNT, ModelType.LONG, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();
    private static SimpleAttributeDefinition COLLECTION_TIME = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.COLLECTION_TIME, ModelType.LONG, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .build();
    private static AttributeDefinition MEMORY_POOL_NAMES = new StringListAttributeDefinition.Builder(PlatformMBeanConstants.MEMORY_POOL_NAMES)
            .setStorageRuntime()
            .build();

    private static final List<SimpleAttributeDefinition> METRICS = Arrays.asList(
            COLLECTION_COUNT,
            COLLECTION_TIME
    );
    private static final List<AttributeDefinition> READ_ATTRIBUTES = Arrays.asList(
            NAME,
            PlatformMBeanConstants.VALID,
            MEMORY_POOL_NAMES
    );


    static final List<String> GARBAGE_COLLECTOR_READ_ATTRIBUTES = Arrays.asList(
            NAME.getName(),
            PlatformMBeanConstants.VALID.getName(),
            MEMORY_POOL_NAMES.getName()
    );
    static final List<String> GARBAGE_COLLECTOR_METRICS = Arrays.asList(
            COLLECTION_COUNT.getName(),
            COLLECTION_TIME.getName()
    );
    static final GarbageCollectorResourceDefinition INSTANCE = new GarbageCollectorResourceDefinition();

    private GarbageCollectorResourceDefinition() {
        super(PathElement.pathElement(NAME.getName()),
                PlatformMBeanUtil.getResolver(PlatformMBeanConstants.GARBAGE_COLLECTOR));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, GarbageCollectorMXBeanAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition attribute : READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, GarbageCollectorMXBeanAttributeHandler.INSTANCE);
        }

        for (SimpleAttributeDefinition attribute : METRICS) {
            registration.registerMetric(attribute, GarbageCollectorMXBeanAttributeHandler.INSTANCE);
        }
    }


}

