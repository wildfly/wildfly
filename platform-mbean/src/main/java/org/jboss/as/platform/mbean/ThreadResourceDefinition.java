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

import static org.jboss.as.platform.mbean.PlatformMBeanConstants.THREADING_PATH;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class ThreadResourceDefinition extends SimpleResourceDefinition {


    static AttributeDefinition CURRENT_THREAD_CPU_TIME = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.CURRENT_THREAD_CPU_TIME, ModelType.LONG, false)
            .setMeasurementUnit(MeasurementUnit.NANOSECONDS)
            .setStorageRuntime()
            .build();

    static AttributeDefinition CURRENT_THREAD_USER_TIME = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.CURRENT_THREAD_USER_TIME, ModelType.LONG, false)
            .setMeasurementUnit(MeasurementUnit.NANOSECONDS)
            .setStorageRuntime()
            .build();


    static AttributeDefinition ALL_THREAD_IDS = new PrimitiveListAttributeDefinition.Builder(PlatformMBeanConstants.ALL_THREAD_IDS, ModelType.LONG)
            .setStorageRuntime()
            .build();

    static AttributeDefinition THREAD_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.THREAD_COUNT, ModelType.INT, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static AttributeDefinition PEAK_THREAD_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.PEAK_THREAD_COUNT, ModelType.INT, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static AttributeDefinition TOTAL_STARTED_THREAD_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.TOTAL_STARTED_THREAD_COUNT, ModelType.LONG, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static AttributeDefinition DAEMON_THREAD_COUNT = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.DAEMON_THREAD_COUNT, ModelType.INT, false)
            .setStorageRuntime()
            .setMeasurementUnit(MeasurementUnit.NONE)
            .build();

    static AttributeDefinition THREAD_CONTENTION_MONITORING_SUPPORTED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.THREAD_CONTENTION_MONITORING_SUPPORTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    static AttributeDefinition THREAD_CONTENTION_MONITORING_ENABLED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.THREAD_CONTENTION_MONITORING_ENABLED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    static AttributeDefinition THREAD_CPU_TIME_SUPPORTED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.THREAD_CPU_TIME_SUPPORTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    static AttributeDefinition CURRENT_THREAD_CPU_TIME_SUPPORTED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.CURRENT_THREAD_CPU_TIME_SUPPORTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    static AttributeDefinition THREAD_CPU_TIME_ENABLED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.THREAD_CPU_TIME_ENABLED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();
    static AttributeDefinition OBJECT_MONITOR_USAGE_SUPPORTED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.OBJECT_MONITOR_USAGE_SUPPORTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();
    static AttributeDefinition SYNCHRONIZER_USAGE_SUPPORTED = SimpleAttributeDefinitionBuilder.create(PlatformMBeanConstants.SYNCHRONIZER_USAGE_SUPPORTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();


    static final List<AttributeDefinition> METRICS = Arrays.asList(
            THREAD_COUNT,
            PEAK_THREAD_COUNT,
            TOTAL_STARTED_THREAD_COUNT,
            DAEMON_THREAD_COUNT,
            CURRENT_THREAD_CPU_TIME,
            CURRENT_THREAD_USER_TIME
    );
    static final List<AttributeDefinition> READ_ATTRIBUTES = Arrays.asList(
            ALL_THREAD_IDS,
            THREAD_CONTENTION_MONITORING_SUPPORTED,
            THREAD_CPU_TIME_SUPPORTED,
            CURRENT_THREAD_CPU_TIME_SUPPORTED,
            OBJECT_MONITOR_USAGE_SUPPORTED,
            SYNCHRONIZER_USAGE_SUPPORTED
    );
    static final List<AttributeDefinition> READ_WRITE_ATTRIBUTES = Arrays.asList(
            THREAD_CONTENTION_MONITORING_ENABLED,
            THREAD_CPU_TIME_ENABLED
    );

    static final List<String> THREADING_READ_ATTRIBUTES = Arrays.asList(
            ALL_THREAD_IDS.getName(),
            THREAD_CONTENTION_MONITORING_SUPPORTED.getName(),
            THREAD_CPU_TIME_SUPPORTED.getName(),
            CURRENT_THREAD_CPU_TIME_SUPPORTED.getName(),
            OBJECT_MONITOR_USAGE_SUPPORTED.getName(),
            SYNCHRONIZER_USAGE_SUPPORTED.getName()
    );
    static final List<String> THREADING_METRICS = Arrays.asList(
            THREAD_COUNT.getName(),
            PEAK_THREAD_COUNT.getName(),
            TOTAL_STARTED_THREAD_COUNT.getName(),
            DAEMON_THREAD_COUNT.getName(),
            CURRENT_THREAD_CPU_TIME.getName(),
            CURRENT_THREAD_USER_TIME.getName()
    );
    static final List<String> THREADING_READ_WRITE_ATTRIBUTES = Arrays.asList(
            THREAD_CONTENTION_MONITORING_ENABLED.getName(),
            THREAD_CPU_TIME_ENABLED.getName()
    );

    static final ThreadResourceDefinition INSTANCE = new ThreadResourceDefinition();

    private ThreadResourceDefinition() {
        super(THREADING_PATH,
                PlatformMBeanUtil.getResolver(PlatformMBeanConstants.THREADING));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, ThreadMXBeanAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition attribute : READ_WRITE_ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, ThreadMXBeanAttributeHandler.INSTANCE, ThreadMXBeanAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition attribute : READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, ThreadMXBeanAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition attribute : METRICS) {
            registration.registerMetric(attribute, ThreadMXBeanAttributeHandler.INSTANCE);
        }
    }

    static EnumSet<OperationEntry.Flag> READ_ONLY_RUNTIME_ONLY_FLAG = EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY, OperationEntry.Flag.READ_ONLY);


    @Override
    public void registerOperations(ManagementResourceRegistration threads) {
        super.registerOperations(threads);
        threads.registerOperationHandler(ReadResourceHandler.DEFINITION, ThreadMXBeanReadResourceHandler.INSTANCE);
        threads.registerOperationHandler(ThreadMXBeanResetPeakThreadCountHandler.DEFINITION, ThreadMXBeanResetPeakThreadCountHandler.INSTANCE);
        threads.registerOperationHandler(ThreadMXBeanFindDeadlockedThreadsHandler.DEFINITION, ThreadMXBeanFindDeadlockedThreadsHandler.INSTANCE);
        threads.registerOperationHandler(ThreadMXBeanFindMonitorDeadlockedThreadsHandler.DEFINITION, ThreadMXBeanFindMonitorDeadlockedThreadsHandler.INSTANCE);
        threads.registerOperationHandler(ThreadMXBeanThreadInfoHandler.DEFINITION, ThreadMXBeanThreadInfoHandler.INSTANCE);
        threads.registerOperationHandler(ThreadMXBeanThreadInfosHandler.DEFINITION, ThreadMXBeanThreadInfosHandler.INSTANCE);
        threads.registerOperationHandler(ThreadMXBeanCpuTimeHandler.DEFINITION, ThreadMXBeanCpuTimeHandler.INSTANCE);
        threads.registerOperationHandler(ThreadMXBeanUserTimeHandler.DEFINITION, ThreadMXBeanUserTimeHandler.INSTANCE);
        threads.registerOperationHandler(ThreadMXBeanDumpAllThreadsHandler.DEFINITION, ThreadMXBeanDumpAllThreadsHandler.INSTANCE);
    }
}

