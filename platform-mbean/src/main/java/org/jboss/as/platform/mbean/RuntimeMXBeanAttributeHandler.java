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
import java.util.Map;
import java.util.TreeMap;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handles read-attribute and write-attribute for the resource representing {@link java.lang.management.RuntimeMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RuntimeMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static RuntimeMXBeanAttributeHandler INSTANCE = new RuntimeMXBeanAttributeHandler();

    private RuntimeMXBeanAttributeHandler() {

    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        try {
            if ((PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name))
                    || PlatformMBeanConstants.RUNTIME_READ_ATTRIBUTES.contains(name)
                    || PlatformMBeanConstants.RUNTIME_METRICS.contains(name)) {
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

        // Shouldn't happen; the global handler should reject
        throw unknownAttribute(operation);

    }

    @Override
    protected void register(ManagementResourceRegistration registration) {

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.RUNTIME_READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.RUNTIME_METRICS) {
            registration.registerMetric(attribute, this);
        }
    }

    static void storeResult(final String name, final ModelNode store) {

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            store.set(ManagementFactory.RUNTIME_MXBEAN_NAME);
        } else if (ModelDescriptionConstants.NAME.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getName());
        } else if (PlatformMBeanConstants.VM_NAME.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getVmName());
        } else if (PlatformMBeanConstants.VM_VENDOR.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getVmVendor());
        } else if (PlatformMBeanConstants.VM_VERSION.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getVmVersion());
        } else if (PlatformMBeanConstants.SPEC_NAME.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getSpecName());
        } else if (PlatformMBeanConstants.SPEC_VENDOR.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getSpecVendor());
        } else if (PlatformMBeanConstants.SPEC_VERSION.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getSpecVersion());
        } else if (PlatformMBeanConstants.MANAGEMENT_SPEC_VERSION.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getManagementSpecVersion());
        } else if (PlatformMBeanConstants.CLASS_PATH.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getClassPath());
        } else if (PlatformMBeanConstants.LIBRARY_PATH.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getLibraryPath());
        } else if (PlatformMBeanConstants.BOOT_CLASS_PATH_SUPPORTED.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().isBootClassPathSupported());
        } else if (PlatformMBeanConstants.BOOT_CLASS_PATH.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getBootClassPath());
        } else if (PlatformMBeanConstants.INPUT_ARGUMENTS.equals(name)) {
            store.setEmptyList();
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                store.add(arg);
            }
        } else if (PlatformMBeanConstants.UPTIME.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getUptime());
        } else if (PlatformMBeanConstants.START_TIME.equals(name)) {
            store.set(ManagementFactory.getRuntimeMXBean().getStartTime());
        } else if (PlatformMBeanConstants.SYSTEM_PROPERTIES.equals(name)) {
            store.setEmptyObject();
            final TreeMap<String, String> sorted = new TreeMap<String, String>(ManagementFactory.getRuntimeMXBean().getSystemProperties());
            for (Map.Entry<String, String> prop : sorted.entrySet()) {
                final ModelNode propNode = store.get(prop.getKey());
                if (prop.getValue() != null) {
                    propNode.set(prop.getValue());
                }
            }
        } else if (PlatformMBeanConstants.RUNTIME_READ_ATTRIBUTES.contains(name)
                || PlatformMBeanConstants.RUNTIME_METRICS.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl10(name);
        }

    }
}
