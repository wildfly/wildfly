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
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Executes the {@link java.lang.management.MemoryMXBean#gc()} method.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MemoryPoolMXBeanResetPeakUsageHandler implements OperationStepHandler, DescriptionProvider {

    public static final MemoryPoolMXBeanResetPeakUsageHandler INSTANCE = new MemoryPoolMXBeanResetPeakUsageHandler();

    private MemoryPoolMXBeanResetPeakUsageHandler() {

    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        try {
            getMemoryPoolMXBean(operation).resetPeakUsage();
        } catch (SecurityException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }

        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return PlatformMBeanDescriptions.getDescriptionOnlyOperation(locale, "reset-peak-usage", PlatformMBeanConstants.MEMORY_POOL);
    }

    private MemoryPoolMXBean getMemoryPoolMXBean(ModelNode operation) throws OperationFailedException {
        final String memPoolName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        MemoryPoolMXBean memoryPoolMXBean = null;

        for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memPoolName.equals(escapeMBeanName(mbean.getName()))) {
                memoryPoolMXBean = mbean;
            }
        }

        if (memoryPoolMXBean == null) {
            throw PlatformMBeanMessages.MESSAGES.unknownMemoryPool2(memPoolName);
        }
        return memoryPoolMXBean;
    }
}
