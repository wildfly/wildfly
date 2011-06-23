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

package org.jboss.as.logging;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation responsible for changing the logging level of the root logger.
 *
 * @author John Bailey
 */
public class RootLoggerLevelChange implements OperationStepHandler {
    static final String OPERATION_NAME = "change-root-log-level";
    static final RootLoggerLevelChange INSTANCE = new RootLoggerLevelChange();

    public void execute(OperationContext context, ModelNode operation) {
        final ModelNode model = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        final String level = operation.get(CommonAttributes.LEVEL).asString();
        model.get(CommonAttributes.ROOT_LOGGER, CommonAttributes.LEVEL).set(level);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) {
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
                    final ServiceController<Logger> controller = (ServiceController<Logger>) serviceRegistry.getService(LogServices.ROOT_LOGGER);
                    if (controller != null) {
                        controller.getValue().setLevel(Level.parse(level));
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }
}
