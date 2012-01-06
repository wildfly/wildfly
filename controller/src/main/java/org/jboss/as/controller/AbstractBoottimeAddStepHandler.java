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

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;

/**
 * Base class for {@link OperationStepHandler} implementations that add managed resource and also perform runtime
 * processing that <strong>should only occur during server boot</strong>. An example of such processing would be installing a
 * deployment unit processor.
 * <p>
 * <strong>Do not extend this class for operations that can run after server boot.</strong> Typically it should only
 * be extended for operations that add a deployment unit processor.
 * </p>
 * <p>
 * If an operation handled via an extension of this class is executed on a server after boot, the server's persistent
 * configuration model will be updated, but the
 * {@link #performBoottime(OperationContext, ModelNode, ModelNode, ServiceVerificationHandler, List) performBoottime}
 * method will not be invoked. Instead the server will be {@link OperationContext#reloadRequired() put into "reload required" state}.
 * </p>
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractBoottimeAddStepHandler extends AbstractAddStepHandler {

    /**
     * If {@link OperationContext#isBooting()} returns {@code true}, invokes
     * {@link #performBoottime(OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, ServiceVerificationHandler, java.util.List)},
     * else invokes {@link OperationContext#reloadRequired()}.
     */
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        if (context.isBooting()) {
            performBoottime(context, operation, model, verificationHandler, newControllers);
        } else {
            context.reloadRequired();
        }
    }

    /**
     * Make any runtime changes necessary to effect the changes indicated by the given {@code operation}. Will only be
     * invoked if {@link OperationContext#isBooting()} returns {@code true}.
     *
     * @param context             the operation context
     * @param operation           the operation being executed
     * @param model               persistent configuration model node that corresponds to the address of {@code operation}
     * @param verificationHandler step handler that can be added as a listener to any new services installed in order to
     *                            validate the services installed correctly during the
     *                            {@link OperationContext.Stage#VERIFY VERIFY stage}
     * @param newControllers      holder for the {@link ServiceController} for any new services installed by the method. The
     *                            method should add the {@code ServiceController} for any new services to this list. If the
     *                            overall operation needs to be rolled back, the list will be used in
     *                            {@link #rollbackRuntime(OperationContext, ModelNode, ModelNode, java.util.List)}  to automatically removed
     *                            the newly added services
     * @throws OperationFailedException if {@code operation} is invalid or updating the runtime otherwise fails
     */
    protected abstract void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException;

    /**
     * Overrides the superclass to additionally call {@link OperationContext#revertReloadRequired()}
     * if {@link OperationContext#isBooting()} returns {@code false}.
     */
    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        super.rollbackRuntime(context, operation, model, controllers);
        if (!context.isBooting()) {
            context.revertReloadRequired();
        }
    }
}
