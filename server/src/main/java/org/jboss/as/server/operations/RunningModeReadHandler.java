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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Reports the current server {@link RunningMode}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RunningModeReadHandler implements OperationStepHandler {


    private final RunningModeControl runningModeControl;

    public RunningModeReadHandler(RunningModeControl runningModeControl) {
        this.runningModeControl = runningModeControl;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.getResult().set(runningModeControl.getRunningMode().name());
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    public static void createAndRegister(final RunningModeControl runningModeControl, final ManagementResourceRegistration resourceRegistration) {
        AttributeDefinition def = SimpleAttributeDefinitionBuilder.create("running-mode", ModelType.STRING)
            .setValidator(new EnumValidator(RunningMode.class, false, false))
                .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();

        resourceRegistration.registerReadOnlyAttribute(def, new RunningModeReadHandler(runningModeControl));
    }
}
