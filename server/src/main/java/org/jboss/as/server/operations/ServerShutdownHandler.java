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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.process.ExitCodes;
import org.jboss.as.server.controller.descriptions.ServerRootDescription;
import org.jboss.dmr.ModelNode;

/**
 * Handler that shuts down the standalone server.
 *
 * @author Jason T. Greene
 */
public class ServerShutdownHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "shutdown";
    public static final ServerShutdownHandler INSTANCE = new ServerShutdownHandler();

    private ServerShutdownHandler() {
    }

    /** {@inheritDoc} */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final boolean restart = operation.hasDefined(RESTART) ? operation.get(RESTART).asBoolean() : false;
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        System.exit(restart ? ExitCodes.RESTART_PROCESS_FROM_STARTUP_SCRIPT : 0);
                    }
                });
                // The intention is that this shutdown is graceful, and so the client gets a reply.
                // At the time of writing we did not yet have graceful shutdown.
                thread.setName("Management Triggered Shutdown");
                thread.start();
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep();
    }

    /** {@inheritDoc} */
    public ModelNode getModelDescription(final Locale locale) {
        return ServerRootDescription.getShutdownOperationDescription(locale);
    }
}
