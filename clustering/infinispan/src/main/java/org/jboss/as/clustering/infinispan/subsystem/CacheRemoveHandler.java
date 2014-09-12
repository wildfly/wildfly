/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheRemoveHandler extends AbstractRemoveStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        // we also need the containerModel to re-install cache services
        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        CacheType type = CacheType.forName(cacheAddress.getLastElement().getKey());

        PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size() - 1);
        ModelNode containerModel = context.readResourceFromRoot(containerAddress).getModel();

        type.getAddHandler().removeRuntimeServices(context, operation, containerModel, model);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode cacheModel) throws OperationFailedException {

        // we also need the containerModel to re-install cache services
        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        CacheType type = CacheType.forName(cacheAddress.getLastElement().getKey());

        PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size() - 1);
        ModelNode containerModel = context.readResourceFromRoot(containerAddress).getModel();

        ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
        type.getAddHandler().installRuntimeServices(context, operation, containerModel, cacheModel, verificationHandler);
    }
}
