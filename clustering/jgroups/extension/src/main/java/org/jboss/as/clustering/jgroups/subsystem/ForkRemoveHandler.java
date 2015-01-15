/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.dmr.ModelNode;

/**
 * Remove operation handler for fork resources.
 * @author Paul Ferraro
 */
public class ForkRemoveHandler extends AbstractRemoveStepHandler {

    private final boolean allowRuntimeOnlyRegistration;

    ForkRemoveHandler(boolean allowRuntimeOnlyRegistration) {
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        String name = context.getCurrentAddressValue();

        if (this.allowRuntimeOnlyRegistration && (context.getRunningMode() == RunningMode.NORMAL)) {
            Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            for (ResourceEntry entry: resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
                context.removeResource(PathAddress.pathAddress(entry.getPathElement()));
            }
            context.getResourceRegistrationForUpdate().unregisterOverrideModel(name);
        }

        super.performRemove(context, operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        ForkAddHandler.removeRuntimeServices(context, operation, model);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ForkAddHandler.installRuntimeServices(context, operation, model);
    }
}
