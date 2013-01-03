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

package org.jboss.as.controller.operations.common;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler for process reloads of servers.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class ProcessReloadHandler<T extends RunningModeControl> implements OperationStepHandler {

    /**
     * The operation name.
     */
    protected static final String OPERATION_NAME = "reload";

    protected static final AttributeDefinition ADMIN_ONLY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.ADMIN_ONLY, ModelType.BOOLEAN, true)
                                                                    .setDefaultValue(new ModelNode(false)).build();

    private final T runningModeControl;
    private final ControlledProcessState processState;

    private final ServiceName rootService;

    public ProcessReloadHandler(final ServiceName rootService, final T runningModeControl,
                                final ControlledProcessState processState) {
        this.rootService = rootService;
        this.runningModeControl = runningModeControl;
        this.processState = processState;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final ReloadContext<T> reloadContext = initializeReloadContext(context, operation);
                final ServiceController<?> service = context.getServiceRegistry(true).getRequiredService(rootService);
                context.completeStep(new OperationContext.ResultHandler() {
                    @Override
                    public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                        if(resultAction == OperationContext.ResultAction.KEEP) {
                            service.addListener(new AbstractServiceListener<Object>() {
                                public void listenerAdded(final ServiceController<?> controller) {
                                    reloadContext.reloadInitiated(runningModeControl);
                                    processState.setStopping();
                                    controller.setMode(ServiceController.Mode.NEVER);
                                }

                                public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                                    if (transition == ServiceController.Transition.STOPPING_to_DOWN) {
                                        controller.removeListener(this);
                                        reloadContext.doReload(runningModeControl);
                                        controller.setMode(ServiceController.Mode.ACTIVE);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }

    protected abstract ReloadContext<T> initializeReloadContext(OperationContext context, ModelNode operation) throws OperationFailedException;

    protected interface ReloadContext<T> {
        void reloadInitiated(T runningModeControl);
        void doReload(T runningModeControl);
    }
}
