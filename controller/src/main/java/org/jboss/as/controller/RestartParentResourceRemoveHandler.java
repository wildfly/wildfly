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
package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.NoSuchElementException;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Simple remove handler that, if allowed, restarts a parent resource when a child is removed.
 * Otherwise the server is put into a forced reload.
 *
 * @author Jason T. Greene
 */
public abstract class RestartParentResourceRemoveHandler extends AbstractRemoveStepHandler {
    private final String parentKeyName;

    protected RestartParentResourceRemoveHandler(String parentKeyName) {
        this.parentKeyName = parentKeyName;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        if (context.isBooting()) {
            return;
        }

        PathAddress address = getParentAddress(PathAddress.pathAddress(operation.require(OP_ADDR)));
        ServiceName serviceName = getParentServiceName(address);
        ServiceController<?> service = serviceName != null ?
                context.getServiceRegistry(false).getService(serviceName) : null;

        // No parent service, nothing to do
        if (service == null) {
            return;
        }

        if (context.isResourceServiceRestartAllowed()) {
            ModelNode parentModel = getModel(context, address);
            if (parentModel != null && context.markResourceRestarted(address, this)) {
                context.removeService(serviceName);
                final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                recreateParentService(context, address, parentModel, verificationHandler);
                context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
            }
        }  else {
            context.reloadRequired();
        }

    }

    protected abstract void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel, ServiceVerificationHandler verificationHandler);

    protected abstract ServiceName getParentServiceName(PathAddress parentAddress);


    protected PathAddress getParentAddress(PathAddress address) {
        return Util.getParentAddressByKey(address, parentKeyName);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        PathAddress address = getParentAddress(PathAddress.pathAddress(operation.require(OP_ADDR)));
        ServiceName serviceName = getParentServiceName(address);
        ServiceController<?> service = serviceName != null ?
                             context.getServiceRegistry(false).getService(serviceName) : null;

        // No parent service indicates boot
        if (service == null) {
            return;
        }

        if (context.isResourceServiceRestartAllowed()) {
            ModelNode parentModel = getOriginalModel(context, address);
            if (parentModel != null && context.revertResourceRestarted(address, this)) {
                context.removeService(serviceName);
                recreateParentService(context, address, parentModel, null);
            }
        }  else {
            context.revertReloadRequired();
        }
    }

     private ModelNode getModel(OperationContext ctx, PathAddress address) {
        try {
            Resource resource = ctx.getRootResource().navigate(address);
            return Resource.Tools.readModel(resource);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private ModelNode getOriginalModel(OperationContext ctx, PathAddress address) {
        try {
            Resource resource = ctx.getOriginalRootResource().navigate(address);
            return Resource.Tools.readModel(resource);
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
