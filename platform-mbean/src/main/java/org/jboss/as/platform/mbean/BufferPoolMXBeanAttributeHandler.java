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

import javax.management.ObjectName;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handles read-attribute and write-attribute for the resource representing {@code java.lang.management.BufferPoolMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BufferPoolMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static BufferPoolMXBeanAttributeHandler INSTANCE = new BufferPoolMXBeanAttributeHandler();

    private BufferPoolMXBeanAttributeHandler() {
    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String bpName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

        final ObjectName objectName = PlatformMBeanUtil.getObjectNameWithNameKey(PlatformMBeanConstants.BUFFER_POOL_MXBEAN_DOMAIN_TYPE, bpName);
        if (!ManagementFactory.getPlatformMBeanServer().isRegistered(objectName)) {
            throw PlatformMBeanMessages.MESSAGES.unknownBufferPool(bpName);
        }

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        if (PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            context.getResult().set(objectName.toString());
        } else if (ModelDescriptionConstants.NAME.equals(name)) {
            context.getResult().set(PlatformMBeanUtil.getMBeanAttribute(objectName, "Name").toString());
        } else if (PlatformMBeanConstants.COUNT.equals(name)) {
            context.getResult().set(Long.class.cast(PlatformMBeanUtil.getMBeanAttribute(objectName, "Count")));
        } else if (PlatformMBeanConstants.MEMORY_USED.equals(name)) {
            context.getResult().set(Long.class.cast(PlatformMBeanUtil.getMBeanAttribute(objectName, "MemoryUsed")));
        } else if (PlatformMBeanConstants.TOTAL_CAPACITY.equals(name)) {
            context.getResult().set(Long.class.cast(PlatformMBeanUtil.getMBeanAttribute(objectName, "TotalCapacity")));
        } else if (PlatformMBeanConstants.BUFFER_POOL_METRICS.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl1(name);
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

        registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, this, AttributeAccess.Storage.RUNTIME);

        for (String attribute : PlatformMBeanConstants.BUFFER_POOL_READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.BUFFER_POOL_METRICS) {
            registration.registerMetric(attribute, this);
        }
    }
}
