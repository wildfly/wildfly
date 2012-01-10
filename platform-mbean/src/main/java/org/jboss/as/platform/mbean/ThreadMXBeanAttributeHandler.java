/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.platform.mbean;

import java.lang.management.ManagementFactory;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handles read-attribute and write-attribute for the resource representing {@link java.lang.management.ThreadMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ThreadMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static ThreadMXBeanAttributeHandler INSTANCE = new ThreadMXBeanAttributeHandler();

    private final ParametersValidator enabledValidator = new ParametersValidator();

    private ThreadMXBeanAttributeHandler() {
        enabledValidator.registerValidator(ModelDescriptionConstants.VALUE, new ModelTypeValidator(ModelType.BOOLEAN));
    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        try {
            if ((PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name))
                    || PlatformMBeanConstants.THREADING_READ_ATTRIBUTES.contains(name)
                    || PlatformMBeanConstants.THREADING_READ_WRITE_ATTRIBUTES.contains(name)
                    || PlatformMBeanConstants.THREADING_METRICS.contains(name)) {
                storeResult(name, context.getResult());
            } else {
                // Shouldn't happen; the global handler should reject
                throw unknownAttribute(operation);
            }
        } catch (SecurityException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        } catch (UnsupportedOperationException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }

    }

    @Override
    protected void executeWriteAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        try {
            if (PlatformMBeanConstants.THREAD_CONTENTION_MONITORING_ENABLED.equals(name)) {
                enabledValidator.validate(operation);
                ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(operation.require(ModelDescriptionConstants.VALUE).asBoolean());
            } else if (PlatformMBeanConstants.THREAD_CPU_TIME_ENABLED.equals(name)) {
                enabledValidator.validate(operation);
                ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(operation.require(ModelDescriptionConstants.VALUE).asBoolean());
            } else if (PlatformMBeanConstants.THREADING_READ_WRITE_ATTRIBUTES.contains(name)) {
                // Bug
                throw PlatformMBeanMessages.MESSAGES.badWriteAttributeImpl4(name);
            } else {
                // Shouldn't happen; the global handler should reject
                throw unknownAttribute(operation);
            }
        } catch (SecurityException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        } catch (UnsupportedOperationException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }

    }

    @Override
    protected void register(ManagementResourceRegistration registration) {

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.THREADING_READ_WRITE_ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, this, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.THREADING_READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.THREADING_METRICS) {
            registration.registerMetric(attribute, this);
        }
    }

    static void storeResult(final String name, final ModelNode store) {

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            store.set(ManagementFactory.THREAD_MXBEAN_NAME);
        } else if (PlatformMBeanConstants.THREAD_COUNT.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().getThreadCount());
        } else if (PlatformMBeanConstants.PEAK_THREAD_COUNT.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().getPeakThreadCount());
        } else if (PlatformMBeanConstants.TOTAL_STARTED_THREAD_COUNT.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().getTotalStartedThreadCount());
        } else if (PlatformMBeanConstants.DAEMON_THREAD_COUNT.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().getDaemonThreadCount());
        } else if (PlatformMBeanConstants.ALL_THREAD_IDS.equals(name)) {
            store.setEmptyList();
            for (Long id : ManagementFactory.getThreadMXBean().getAllThreadIds()) {
                store.add(id);
            }
        } else if (PlatformMBeanConstants.THREAD_CONTENTION_MONITORING_SUPPORTED.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().isThreadContentionMonitoringSupported());
        } else if (PlatformMBeanConstants.THREAD_CONTENTION_MONITORING_ENABLED.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().isThreadContentionMonitoringEnabled());
        } else if (PlatformMBeanConstants.CURRENT_THREAD_CPU_TIME.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime());
        } else if (PlatformMBeanConstants.CURRENT_THREAD_USER_TIME.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().getCurrentThreadUserTime());
        } else if (PlatformMBeanConstants.THREAD_CPU_TIME_SUPPORTED.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported());
        } else if (PlatformMBeanConstants.CURRENT_THREAD_CPU_TIME_SUPPORTED.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().isCurrentThreadCpuTimeSupported());
        } else if (PlatformMBeanConstants.THREAD_CPU_TIME_ENABLED.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled());
        } else if (PlatformMBeanConstants.OBJECT_MONITOR_USAGE_SUPPORTED.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().isObjectMonitorUsageSupported());
        } else if (PlatformMBeanConstants.SYNCHRONIZER_USAGE_SUPPORTED.equals(name)) {
            store.set(ManagementFactory.getThreadMXBean().isSynchronizerUsageSupported());
        } else if (PlatformMBeanConstants.THREADING_READ_ATTRIBUTES.contains(name)
                || PlatformMBeanConstants.THREADING_READ_WRITE_ATTRIBUTES.contains(name)
                || PlatformMBeanConstants.THREADING_METRICS.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl11(name);
        }

    }
}
