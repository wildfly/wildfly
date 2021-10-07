/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.model;

import java.util.function.Function;

import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * <p>This remove handler is used during the removal of all partition-manager resources.</p>
 *
 * @author Pedro Silva
 */
public class DefaultRemoveStepHandler extends ModelOnlyRemoveStepHandler {

    private final Function<PathAddress, PathAddress> partitionAddressProvider;

    DefaultRemoveStepHandler(final Function<PathAddress, PathAddress> partitionAddressProvider) {
        this.partitionAddressProvider = partitionAddressProvider;
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        context.addStep(((context1, operation1) -> PartitionManagerResourceDefinition.validateModel(context1, partitionAddressProvider.apply(context1.getCurrentAddress()))), OperationContext.Stage.MODEL);
        super.performRemove(context, operation, model);
    }

    @Override
    protected boolean removeChildRecursively(PathElement child) {
        // children only represent configuration details of the parent, and are not independent entities
        return false;
    }

}
