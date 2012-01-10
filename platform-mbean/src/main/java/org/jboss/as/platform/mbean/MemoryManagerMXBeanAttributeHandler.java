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
import java.lang.management.MemoryManagerMXBean;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handles read-attribute and write-attribute for the resource representing {@link java.lang.management.MemoryManagerMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MemoryManagerMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static MemoryManagerMXBeanAttributeHandler INSTANCE = new MemoryManagerMXBeanAttributeHandler();

    private MemoryManagerMXBeanAttributeHandler() {

    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String mmName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        MemoryManagerMXBean memoryManagerMXBean = null;

        for (MemoryManagerMXBean mbean : ManagementFactory.getMemoryManagerMXBeans()) {
            if (mmName.equals(escapeMBeanName(mbean.getName()))) {
                memoryManagerMXBean = mbean;
            }
        }

        if (memoryManagerMXBean == null) {
            throw PlatformMBeanMessages.MESSAGES.unknownMemoryManager(mmName);
        }

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            final String objName = PlatformMBeanUtil.getObjectNameStringWithNameKey(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE, mmName);
            context.getResult().set(objName);
        } else if (ModelDescriptionConstants.NAME.equals(name)) {
            context.getResult().set(escapeMBeanName(memoryManagerMXBean.getName()));
        } else if (PlatformMBeanConstants.VALID.equals(name)) {
            context.getResult().set(memoryManagerMXBean.isValid());
        } else if (PlatformMBeanConstants.MEMORY_POOL_NAMES.equals(name)) {
            final ModelNode result = context.getResult();
            result.setEmptyList();
            for (String pool : memoryManagerMXBean.getMemoryPoolNames()) {
                result.add(escapeMBeanName(pool));
            }
        } else if (PlatformMBeanConstants.MEMORY_MANAGER_READ_ATTRIBUTES.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl5(name);
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

        for (String attribute : PlatformMBeanConstants.MEMORY_MANAGER_READ_ATTRIBUTES) {
            registration.registerMetric(attribute, this);
        }
    }
}
