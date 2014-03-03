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

package org.jboss.as.domain.management.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACTIVE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_OPERATIONS;

import java.util.EnumSet;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.NoSuchResourceException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.management.logging.DomainManagementLogger;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} to cancel an
 * {@link org.jboss.as.domain.management.controller.ActiveOperationResourceDefinition active operation}.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class CancelActiveOperationHandler implements OperationStepHandler {

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder("cancel",
            DomainManagementResolver.getResolver(CORE, MANAGEMENT_OPERATIONS, ACTIVE_OPERATION))
            .setReplyType(ModelType.BOOLEAN)
            .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
            .build();

    static final OperationStepHandler INSTANCE = new CancelActiveOperationHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        context.authorize(operation, EnumSet.of(Action.ActionEffect.WRITE_RUNTIME));

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                boolean cancelled = false;
                try {
                    Cancellable cancellable = Cancellable.class.cast(context.readResource(PathAddress.EMPTY_ADDRESS));
                    DomainManagementLogger.ROOT_LOGGER.debugf("Cancelling %s", cancellable);
                    cancelled = cancellable.cancel();
                } catch (NoSuchResourceException nsre) {
                    // resource is gone; return 'false'
                }
                context.getResult().set(cancelled);

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
