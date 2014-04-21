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

package org.jboss.as.server.operations;


import java.util.EnumSet;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.process.ExitCodes;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler that shuts down the standalone server.
 *
 * @author Jason T. Greene
 */
public class ServerShutdownHandler implements OperationStepHandler {

    private static final SimpleAttributeDefinition RESTART = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RESTART, ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .build();
    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder("shutdown", ServerDescriptions.getResourceDescriptionResolver())
            .setParameters(RESTART)
            .setRuntimeOnly()
            .build();


    private final ControlledProcessState processState;

    public ServerShutdownHandler(ControlledProcessState processState) {
        this.processState = processState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final boolean restart = RESTART.resolveModelAttribute(context, operation).asBoolean();
        // Acquire the controller lock to prevent new write ops and wait until current ones are done
        context.acquireControllerLock();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // WFLY-2741 -- DO NOT call context.getServiceRegistry(true) as that will trigger blocking for
                // service container stability and one use case for this op is to recover from a
                // messed up service container from a previous op. Instead just ask for authorization.
                // Note that we already have the exclusive lock, so we are just skipping waiting for stability.
                // If another op that is a step in a composite step with this op needs to modify the container
                // it will have to wait for container stability, so skipping this only matters for the case
                // where this step is the only runtime change.
//                context.getServiceRegistry(true);
                context.authorize(operation, EnumSet.of(Action.ActionEffect.WRITE_RUNTIME));
                context.completeStep(new OperationContext.ResultHandler() {
                    @Override
                    public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                        if(resultAction == OperationContext.ResultAction.KEEP) {
                            processState.setStopping();
                            final Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    System.exit(restart ? ExitCodes.RESTART_PROCESS_FROM_STARTUP_SCRIPT : ExitCodes.NORMAL);
                                }
                            });
                            // The intention is that this shutdown is graceful, and so the client gets a reply.
                            // At the time of writing we did not yet have graceful shutdown.
                            thread.setName("Management Triggered Shutdown");
                            thread.start();
                        }
                    }
                });
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
