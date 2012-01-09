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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;

/**
 * Handles read-resource for the resource representing {@link java.lang.management.RuntimeMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RuntimeMXBeanReadResourceHandler implements OperationStepHandler {

    public static final RuntimeMXBeanReadResourceHandler INSTANCE = new RuntimeMXBeanReadResourceHandler();

    private RuntimeMXBeanReadResourceHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode result = context.getResult();

        for (String attribute : PlatformMBeanConstants.RUNTIME_READ_ATTRIBUTES) {
            final ModelNode store = result.get(attribute);
            try {
                RuntimeMXBeanAttributeHandler.storeResult(attribute, store);
            } catch (SecurityException ignored) {
                // just leave it undefined
            } catch (UnsupportedOperationException ignored) {
                // just leave it undefined
            }
        }

        for (String attribute : PlatformMBeanConstants.RUNTIME_METRICS) {
            final ModelNode store = result.get(attribute);
            try {
                RuntimeMXBeanAttributeHandler.storeResult(attribute, store);
            } catch (SecurityException ignored) {
                // just leave it undefined
            } catch (UnsupportedOperationException ignored) {
                // just leave it undefined
            }
        }
        if (PlatformMBeanUtil.JVM_MAJOR_VERSION > 6) {
            final ModelNode store = result.get(PlatformMBeanConstants.OBJECT_NAME);
            RuntimeMXBeanAttributeHandler.storeResult(PlatformMBeanConstants.OBJECT_NAME, store);
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
