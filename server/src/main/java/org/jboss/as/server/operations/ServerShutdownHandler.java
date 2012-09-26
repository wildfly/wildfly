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


import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
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
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                processState.setStopping();
                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        System.exit(restart ? ExitCodes.RESTART_PROCESS_FROM_STARTUP_SCRIPT : 0);
                    }
                });
                // The intention is that this shutdown is graceful, and so the client gets a reply.
                // At the time of writing we did not yet have graceful shutdown.
                thread.setName("Management Triggered Shutdown");
                thread.start();
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
