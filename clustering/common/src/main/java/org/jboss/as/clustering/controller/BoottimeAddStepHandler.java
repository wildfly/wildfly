/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Generic boot-time add step handler that delegates service installation/rollback to a {@link ResourceServiceHandler}.
 * Implementation note:
 * This handler inherits the logic from {@link AddStepHandler} and reimplements the logic from {@link org.jboss.as.controller.AbstractBoottimeAddStepHandler}
 * since the latter requires less code duplication.
 * @author Paul Ferraro
 */
public class BoottimeAddStepHandler extends AddStepHandler {

    public BoottimeAddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
        super(descriptor, handler);
    }

    public BoottimeAddStepHandler(AddStepHandlerDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected final void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        if (context.isBooting()) {
            super.performRuntime(context, operation, resource);
        } else {
            context.reloadRequired();
        }
    }

    @Override
    protected final void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        if (context.isBooting()) {
            super.rollbackRuntime(context, operation, resource);
        } else {
            context.revertReloadRequired();
        }
    }
}
