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

import static org.jboss.as.platform.mbean.PlatformMBeanUtil.escapeMBeanName;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handles read-attribute and write-attribute for the resource representing {@link java.lang.management.MemoryPoolMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MemoryPoolMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static MemoryPoolMXBeanAttributeHandler INSTANCE = new MemoryPoolMXBeanAttributeHandler();

    private final ParametersValidator usageValidator = new ParametersValidator();

    private MemoryPoolMXBeanAttributeHandler() {
        usageValidator.registerValidator(ModelDescriptionConstants.VALUE, new LongRangeValidator(0));
    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String memPoolName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        try {
            if ((PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name))
                    || PlatformMBeanConstants.MEMORY_POOL_READ_ATTRIBUTES.contains(name)
                    || PlatformMBeanConstants.MEMORY_POOL_READ_WRITE_ATTRIBUTES.contains(name)
                    || PlatformMBeanConstants.MEMORY_POOL_METRICS.contains(name)) {
                MemoryPoolMXBean memoryPoolMXBean = getMemoryPoolMXBean(memPoolName);
                storeResult(name, context.getResult(), memoryPoolMXBean, memPoolName);
            } else {
                // Shouldn't happen; the global handler should reject
                throw unknownAttribute(operation);
            }
        } catch (UnsupportedOperationException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }

    }

    @Override
    protected void executeWriteAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String memPoolName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        MemoryPoolMXBean memoryPoolMXBean = getMemoryPoolMXBean(memPoolName);

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        try {
            if (PlatformMBeanConstants.USAGE_THRESHOLD.equals(name)) {
                usageValidator.validate(operation);
                memoryPoolMXBean.setUsageThreshold(operation.require(ModelDescriptionConstants.VALUE).asLong());
            } else if (PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD.equals(name)) {
                usageValidator.validate(operation);
                memoryPoolMXBean.setCollectionUsageThreshold(operation.require(ModelDescriptionConstants.VALUE).asLong());
            } else if (PlatformMBeanConstants.MEMORY_POOL_READ_WRITE_ATTRIBUTES.contains(name)) {
                // Bug
                throw PlatformMBeanMessages.MESSAGES.badWriteAttributeImpl3(name);
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

        for (String attribute : PlatformMBeanConstants.MEMORY_POOL_READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.MEMORY_POOL_READ_WRITE_ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, this, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.MEMORY_POOL_METRICS) {
            registration.registerMetric(attribute, this);
        }
    }

    static void storeResult(final String name, final ModelNode store, final MemoryPoolMXBean memoryPoolMXBean, final String memPoolName) {

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            final String objName = PlatformMBeanUtil.getObjectNameStringWithNameKey(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE, memPoolName);
            store.set(objName);
        } else if (ModelDescriptionConstants.NAME.equals(name)) {
            store.set(escapeMBeanName(memoryPoolMXBean.getName()));
        } else if (PlatformMBeanConstants.TYPE.equals(name)) {
            store.set(memoryPoolMXBean.getType().name());
        } else if (PlatformMBeanConstants.USAGE.equals(name)) {
            final ModelNode usage = PlatformMBeanUtil.getDetypedMemoryUsage(memoryPoolMXBean.getUsage());
            store.set(usage);
        } else if (PlatformMBeanConstants.PEAK_USAGE.equals(name)) {
            final ModelNode usage = PlatformMBeanUtil.getDetypedMemoryUsage(memoryPoolMXBean.getPeakUsage());
            store.set(usage);
        } else if (PlatformMBeanConstants.VALID.equals(name)) {
            store.set(memoryPoolMXBean.isValid());
        } else if (PlatformMBeanConstants.MEMORY_MANAGER_NAMES.equals(name)) {
            store.setEmptyList();
            for (String mgr : memoryPoolMXBean.getMemoryManagerNames()) {
                store.add(escapeMBeanName(mgr));
            }
        } else if (PlatformMBeanConstants.USAGE_THRESHOLD.equals(name)) {
            store.set(memoryPoolMXBean.getUsageThreshold());
        } else if (PlatformMBeanConstants.USAGE_THRESHOLD_EXCEEDED.equals(name)) {
            store.set(memoryPoolMXBean.isUsageThresholdExceeded());
        } else if (PlatformMBeanConstants.USAGE_THRESHOLD_COUNT.equals(name)) {
            store.set(memoryPoolMXBean.getUsageThresholdCount());
        } else if (PlatformMBeanConstants.USAGE_THRESHOLD_SUPPORTED.equals(name)) {
            store.set(memoryPoolMXBean.isUsageThresholdSupported());
        } else if (PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD.equals(name)) {
            store.set(memoryPoolMXBean.getCollectionUsageThreshold());
        } else if (PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD_EXCEEDED.equals(name)) {
            store.set(memoryPoolMXBean.isCollectionUsageThresholdExceeded());
        } else if (PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD_COUNT.equals(name)) {
            store.set(memoryPoolMXBean.getCollectionUsageThresholdCount());
        } else if (PlatformMBeanConstants.COLLECTION_USAGE_THRESHOLD_SUPPORTED.equals(name)) {
            store.set(memoryPoolMXBean.isCollectionUsageThresholdSupported());
        } else if (PlatformMBeanConstants.COLLECTION_USAGE.equals(name)) {
            final ModelNode usage = PlatformMBeanUtil.getDetypedMemoryUsage(memoryPoolMXBean.getCollectionUsage());
            store.set(usage);
        } else if (PlatformMBeanConstants.MEMORY_POOL_READ_ATTRIBUTES.contains(name)
                || PlatformMBeanConstants.MEMORY_POOL_READ_WRITE_ATTRIBUTES.contains(name)
                || PlatformMBeanConstants.MEMORY_POOL_METRICS.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl7(name);
        }

    }

    static MemoryPoolMXBean getMemoryPoolMXBean(String memPoolName) throws OperationFailedException {

        MemoryPoolMXBean memoryPoolMXBean = null;

        for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memPoolName.equals(escapeMBeanName(mbean.getName()))) {
                memoryPoolMXBean = mbean;
            }
        }

        if (memoryPoolMXBean == null) {
            throw PlatformMBeanMessages.MESSAGES.unknownMemoryPool1(memPoolName);
        }
        return memoryPoolMXBean;
    }
}
