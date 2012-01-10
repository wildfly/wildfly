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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handles read-attribute and write-attribute for the resource representing {@link java.lang.management.GarbageCollectorMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class GarbageCollectorMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static GarbageCollectorMXBeanAttributeHandler INSTANCE = new GarbageCollectorMXBeanAttributeHandler();



    private GarbageCollectorMXBeanAttributeHandler() {

    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String gcName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        GarbageCollectorMXBean gcMBean = null;

        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gcName.equals(escapeMBeanName(mbean.getName()))) {
                gcMBean = mbean;
            }
        }

        if (gcMBean == null) {
            throw PlatformMBeanMessages.MESSAGES.unknownGarbageCollector(gcName);
        }

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            final String objName = PlatformMBeanUtil.getObjectNameStringWithNameKey(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE, gcName);
            context.getResult().set(objName);
        } else if (ModelDescriptionConstants.NAME.equals(name)) {
            context.getResult().set(escapeMBeanName(gcMBean.getName()));
        } else if (PlatformMBeanConstants.VALID.equals(name)) {
            context.getResult().set(gcMBean.isValid());
        } else if (PlatformMBeanConstants.MEMORY_POOL_NAMES.equals(name)) {
            final ModelNode result = context.getResult();
            result.setEmptyList();
            for (String pool : gcMBean.getMemoryPoolNames()) {
                result.add(escapeMBeanName(pool));
            }
        } else if (PlatformMBeanConstants.COLLECTION_COUNT.equals(name)) {
            context.getResult().set(gcMBean.getCollectionCount());
        } else if (PlatformMBeanConstants.COLLECTION_TIME.equals(name)) {
            context.getResult().set(gcMBean.getCollectionTime());
        } else if (PlatformMBeanConstants.GARBAGE_COLLECTOR_READ_ATTRIBUTES.contains(name)
                || PlatformMBeanConstants.GARBAGE_COLLECTOR_METRICS.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl4(name);
        } else {
            // Shouldn't happen; the global handler should reject
            throw unknownAttribute(operation);
        }

    }

    @Override
    protected void executeWriteAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Shouldn't happen; the global handler should reject
        throw unknownAttribute(operation);

    }

    @Override
    protected void register(ManagementResourceRegistration registration) {

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.GARBAGE_COLLECTOR_READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.GARBAGE_COLLECTOR_METRICS) {
            registration.registerMetric(attribute, this);
        }
    }
}
