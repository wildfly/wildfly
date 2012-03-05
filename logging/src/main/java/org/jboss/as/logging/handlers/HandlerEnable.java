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

package org.jboss.as.logging.handlers;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.logging.util.LogServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Operation responsible for enabling a logging handler.
 *
 * @author John Bailey
 */
public class HandlerEnable implements OperationStepHandler {
    public static final HandlerEnable INSTANCE = new HandlerEnable();

    public void execute(final OperationContext context, ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, ModelNode operation) {
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
                    final ServiceController<?> controller = serviceRegistry.getService(LogServices.handlerName(name));
                    if (controller != null) {
                        controller.addListener(new AbstractServiceListener<Object>() {
                            public void listenerAdded(ServiceController<?> serviceController) {
                                serviceController.setMode(ServiceController.Mode.ACTIVE);
                            }

                            @Override
                            public void transition(ServiceController<?> serviceController, ServiceController.Transition transition) {
                                if (transition == ServiceController.Transition.STARTING_to_UP) {
                                    context.completeStep();
                                }
                            }
                        });
                    } else {
                        context.completeStep();
                    }

                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();

    }
}
