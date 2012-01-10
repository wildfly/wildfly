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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handles read-attribute and write-attribute for the resource representing {@link java.lang.management.OperatingSystemMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperatingSystemMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static OperatingSystemMXBeanAttributeHandler INSTANCE = new OperatingSystemMXBeanAttributeHandler();

    private OperatingSystemMXBeanAttributeHandler() {

    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        try {
            if ((PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name))
                    || PlatformMBeanConstants.OPERATING_SYSTEM_READ_ATTRIBUTES.contains(name)
                    || PlatformMBeanConstants.OPERATING_SYSTEM_METRICS.contains(name)) {
                storeResult(name, context.getResult());
            } else {
                // Shouldn't happen; the global handler should reject
                throw unknownAttribute(operation);
            }
        } catch (SecurityException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
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

        for (String attribute : PlatformMBeanConstants.OPERATING_SYSTEM_READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.OPERATING_SYSTEM_METRICS) {
            registration.registerMetric(attribute, this);
        }
    }

    static void storeResult(final String name, final ModelNode store) {

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            store.set(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        } else if (ModelDescriptionConstants.NAME.equals(name)) {
            store.set(ManagementFactory.getOperatingSystemMXBean().getName());
        } else if (PlatformMBeanConstants.ARCH.equals(name)) {
            store.set(ManagementFactory.getOperatingSystemMXBean().getArch());
        } else if (PlatformMBeanConstants.VERSION.equals(name)) {
            store.set(ManagementFactory.getOperatingSystemMXBean().getVersion());
        } else if (PlatformMBeanConstants.AVAILABLE_PROCESSORS.equals(name)) {
            store.set(ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
        } else if (PlatformMBeanConstants.SYSTEM_LOAD_AVERAGE.equals(name)) {
            store.set(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        } else if (PlatformMBeanConstants.OPERATING_SYSTEM_READ_ATTRIBUTES.contains(name)
                    || PlatformMBeanConstants.OPERATING_SYSTEM_METRICS.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl8(name);
        }
    }
}
