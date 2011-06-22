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

import java.util.logging.Handler;
import java.util.logging.Level;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation responsible for changing a log handler logging level.
 *
 * @author John Bailey
 */
public class HandlerLevelChange implements OperationStepHandler {
    static final String OPERATION_NAME = "change-log-level";
    static final HandlerLevelChange INSTANCE = new HandlerLevelChange();

    public void execute(OperationContext context, ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String level = operation.get(CommonAttributes.LEVEL).asString();

        context.readModelForUpdate(PathAddress.EMPTY_ADDRESS).get(CommonAttributes.LEVEL).set(level);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) {
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
                    final ServiceController<Handler> controller = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(name));
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
