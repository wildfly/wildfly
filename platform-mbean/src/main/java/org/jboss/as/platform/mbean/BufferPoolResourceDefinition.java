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


import static org.jboss.as.platform.mbean.PlatformMBeanConstants.BUFFER_POOL;
import static org.jboss.as.platform.mbean.PlatformMBeanConstants.NAME;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class BufferPoolResourceDefinition extends SimpleResourceDefinition {


    private static AttributeDefinition MEMORY_USED_NAME = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.MEMORY_USED_NAME, ModelType.LONG, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .build();
    private static AttributeDefinition TOTAL_CAPACITY = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.TOTAL_CAPACITY, ModelType.LONG, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .build();

    private static AttributeDefinition COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.COUNT, ModelType.LONG, false)
            .setStorageRuntime()
            .build();

    private static final List<AttributeDefinition> METRICS = Arrays.asList(
            COUNT,
            MEMORY_USED_NAME,
            TOTAL_CAPACITY
    );

    private static final List<AttributeDefinition> READ_ATTRIBUTES = Arrays.asList(
            PlatformMBeanConstants.NAME
    );

    static final List<String> BUFFER_POOL_METRICS = Arrays.asList(
            COUNT.getName(),
            MEMORY_USED_NAME.getName(),
            TOTAL_CAPACITY.getName()
    );


    static final BufferPoolResourceDefinition INSTANCE = new BufferPoolResourceDefinition();


    private BufferPoolResourceDefinition() {
        super(PathElement.pathElement(NAME.getName()),
                PlatformMBeanUtil.getResolver(BUFFER_POOL));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, BufferPoolMXBeanAttributeHandler.INSTANCE);
        for (AttributeDefinition attribute : READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, BufferPoolMXBeanAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition attribute : METRICS) {
            registration.registerMetric(attribute, BufferPoolMXBeanAttributeHandler.INSTANCE);
        }
    }

}

