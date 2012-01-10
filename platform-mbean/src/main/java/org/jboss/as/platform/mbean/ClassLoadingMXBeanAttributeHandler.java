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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handles read-attribute and write-attribute for the resource representing {@link java.lang.management.ClassLoadingMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ClassLoadingMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static ClassLoadingMXBeanAttributeHandler INSTANCE = new ClassLoadingMXBeanAttributeHandler();

    private ClassLoadingMXBeanAttributeHandler() {
        writeAttributeValidator.registerValidator(ModelDescriptionConstants.VALUE, new ModelTypeValidator(ModelType.BOOLEAN));
    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6 && PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            context.getResult().set(ManagementFactory.CLASS_LOADING_MXBEAN_NAME);
        } else if (PlatformMBeanConstants.TOTAL_LOADED_CLASS_COUNT.equals(name)) {
            context.getResult().set(ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
        } else if (PlatformMBeanConstants.LOADED_CLASS_COUNT.equals(name)) {
            context.getResult().set(ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
        } else if (PlatformMBeanConstants.UNLOADED_CLASS_COUNT.equals(name)) {
            context.getResult().set(ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount());
        } else if (PlatformMBeanConstants.VERBOSE.equals(name)) {
            context.getResult().set(ManagementFactory.getClassLoadingMXBean().isVerbose());
        } else if (PlatformMBeanConstants.CLASSLOADING_METRICS.contains(name)
                || PlatformMBeanConstants.CLASSLOADING_READ_WRITE_ATTRIBUTES.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl2(name);
        } else {
            // Shouldn't happen; the global handler should reject
            throw unknownAttribute(operation);
        }

    }

    @Override
    protected void executeWriteAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();
        if (PlatformMBeanConstants.VERBOSE.equals(name)) {
            ManagementFactory.getClassLoadingMXBean().setVerbose(operation.require(ModelDescriptionConstants.VALUE).asBoolean());
        } else if (PlatformMBeanConstants.CLASSLOADING_READ_WRITE_ATTRIBUTES.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badWriteAttributeImpl1(name);
        } else {
            // Shouldn't happen; the global handler should reject
            throw unknownAttribute(operation);
        }

    }

    @Override
    protected void register(ManagementResourceRegistration registration) {

        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, this, AttributeAccess.Storage.RUNTIME);
        }

        for (String attribute : PlatformMBeanConstants.CLASSLOADING_METRICS) {
            registration.registerMetric(attribute, this);
        }

        for (String attribute : PlatformMBeanConstants.CLASSLOADING_READ_WRITE_ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, this, this, AttributeAccess.Storage.RUNTIME);
        }
    }
}
