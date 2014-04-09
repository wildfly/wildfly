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

package org.jboss.as.server.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.dmr.ModelNode;

/**
 * Operation handlers responsible for putting the server in either the reload or restart-required state.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerProcessStateHandler implements OperationStepHandler {

    public static final String REQUIRE_RELOAD_OPERATION = "server-set-reload-required";
    public static final String REQUIRE_RESTART_OPERATION = "server-set-restart-required";

    public static final SimpleOperationDefinition RELOAD_DEFINITION = new SimpleOperationDefinitionBuilder(REQUIRE_RELOAD_OPERATION, ServerDescriptions.getResourceDescriptionResolver())
            .setPrivateEntry()
            .build();

    public static final SimpleOperationDefinition RESTART_DEFINITION = new SimpleOperationDefinitionBuilder(REQUIRE_RESTART_OPERATION, ServerDescriptions.getResourceDescriptionResolver())
            .setPrivateEntry()
            .build();

    public static final OperationStepHandler SET_RELOAD_REQUIRED_HANDLER = new ServerProcessStateHandler(true);
    public static final OperationStepHandler SET_RESTART_REQUIRED_HANDLER = new ServerProcessStateHandler(false);

    private final boolean reload;
    ServerProcessStateHandler(boolean reload) {
        this.reload = reload;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // Acquire the lock and check the write permissions for this operation
        context.getServiceRegistry(true);
        if (reload) {
            context.reloadRequired();
        } else {
            context.restartRequired();
        }
        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (reload) {
                    context.revertReloadRequired();
                } else {
                    context.revertRestartRequired();
                }
            }
        });
    }

}
