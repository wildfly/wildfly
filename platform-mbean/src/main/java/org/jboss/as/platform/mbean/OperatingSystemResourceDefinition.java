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

import static org.jboss.as.platform.mbean.PlatformMBeanConstants.OPERATING_SYSTEM_PATH;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class OperatingSystemResourceDefinition extends SimpleResourceDefinition {


    private static SimpleAttributeDefinition AVAILABLE_PROCESSORS = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.AVAILABLE_PROCESSORS, ModelType.INT, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    private static SimpleAttributeDefinition SYSTEM_LOAD_AVERAGE = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.SYSTEM_LOAD_AVERAGE, ModelType.DOUBLE, false)
            .setMeasurementUnit(MeasurementUnit.PERCENTAGE)
            .setStorageRuntime()
            .build();


    private static AttributeDefinition ARCH = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.ARCH, ModelType.STRING, true)
            .setStorageRuntime()
            .build();
    private static AttributeDefinition VERSION = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.VERSION, ModelType.STRING, true)
            .setStorageRuntime()
            .build();

    private static final List<SimpleAttributeDefinition> METRICS = Arrays.asList(
            AVAILABLE_PROCESSORS,
            SYSTEM_LOAD_AVERAGE
    );
    private static final List<AttributeDefinition> READ_ATTRIBUTES = Arrays.asList(
            PlatformMBeanConstants.NAME,
            ARCH,
            VERSION
    );

    public static final List<String> OPERATING_SYSTEM_READ_ATTRIBUTES = Arrays.asList(
            PlatformMBeanConstants.NAME.getName(),
            ARCH.getName(),
            VERSION.getName()
    );
    public static final List<String> OPERATING_SYSTEM_METRICS = Arrays.asList(
            AVAILABLE_PROCESSORS.getName(),
            SYSTEM_LOAD_AVERAGE.getName()
    );


    static final OperatingSystemResourceDefinition INSTANCE = new OperatingSystemResourceDefinition();

    private OperatingSystemResourceDefinition() {
        super(OPERATING_SYSTEM_PATH,
                PlatformMBeanUtil.getResolver(PlatformMBeanConstants.OPERATING_SYSTEM));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, OperatingSystemMXBeanAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition attribute : READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, OperatingSystemMXBeanAttributeHandler.INSTANCE);
        }

        for (SimpleAttributeDefinition attribute : METRICS) {
            registration.registerMetric(attribute, OperatingSystemMXBeanAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(ReadResourceHandler.DEFINITION, OperatingSystemMXBeanReadResourceHandler.INSTANCE);
    }
}

