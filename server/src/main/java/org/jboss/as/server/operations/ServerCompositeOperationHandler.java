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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.BaseCompositeOperationHandler;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.ServerOperationContext;
import org.jboss.as.server.controller.descriptions.ServerRootDescription;
import org.jboss.dmr.ModelNode;

/**
 * Handler for multi-step operations that have to be performed atomically.
 *
 * @author Brian Stansberry
 */
public class ServerCompositeOperationHandler
    extends BaseCompositeOperationHandler
    implements DescriptionProvider {

    public static final ServerCompositeOperationHandler INSTANCE = new ServerCompositeOperationHandler();

    private ServerCompositeOperationHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ServerRootDescription.getCompositeOperationDescription(locale);
    }

    @Override
    protected CompositeOperationContext getCompositeOperationContext(OperationContext context, ModelNode operation,
            ResultHandler resultHandler, final List<ModelNode> steps) {
        if (context instanceof ServerOperationContext) {
            return new RuntimeCompositeOperationContext((ServerOperationContext)context, resultHandler, steps.size());
        }
        return super.getCompositeOperationContext(context, operation, resultHandler, steps);
    }

    @Override
    protected RuntimeTask getRuntimeTasks(final CompositeOperationContext context) {
        if(context instanceof RuntimeCompositeOperationContext) {
            final RuntimeCompositeOperationContext runtimeCompositeContext = RuntimeCompositeOperationContext.class.cast(context);
            if(!runtimeCompositeContext.runtimeTasks.isEmpty()) {
                return new RuntimeTask() {
                    @Override
                    public void execute(final RuntimeTaskContext context) throws OperationFailedException {
                        for(RuntimeTask runtimeTask : runtimeCompositeContext.runtimeTasks) {
                            runtimeTask.execute(context);
                        }
                    }
                };
            }
        }
        return null;
    }

    private static class RuntimeCompositeOperationContext extends CompositeOperationContext {

        private final ServerOperationContext overallRuntimeContext;
        private boolean modelOnly = false;
        private final Map<Integer, Boolean> modelOnlyStates = new HashMap<Integer, Boolean>();
        private Deque<RuntimeTask> runtimeTasks = new ArrayDeque<RuntimeTask>();

        private RuntimeCompositeOperationContext(final ServerOperationContext overallContext, final ResultHandler resultHandler,
                final int count) {
            super(overallContext, resultHandler, count);
            this.overallRuntimeContext = overallContext;
        }

        @Override
        public OperationContext getStepOperationContext(final Integer index, final PathAddress address, final OperationHandler stepHandler) {
            modelOnlyStates.put(index, Boolean.valueOf(modelOnly));
            OperationContext stepOperationContext;
            if (modelOnly) {
                stepOperationContext = super.getStepOperationContext(index, address, stepHandler);
            }
            else if(stepHandler instanceof BootOperationHandler) {
                // The ModelController needs to be informed that it now shouldn't execute runtime ops
                // FIXME -- by putting rollback in the controller, we've lost the ability to revert this
                overallRuntimeContext.restartRequired();
                modelOnly = true;
                modelOnlyStates.put(index, Boolean.TRUE);
                stepOperationContext = super.getStepOperationContext(index, address, stepHandler);
            }
            else {
                final ModelNode stepModel = getStepSubModel(address, stepHandler);
                stepOperationContext = new StepRuntimeOperationContext(stepModel);
            }
            return stepOperationContext;
        }

        private class StepRuntimeOperationContext implements ServerOperationContext, RuntimeOperationContext {
            private ModelNode stepModel;

            private StepRuntimeOperationContext(ModelNode stepModel) {
                this.stepModel = stepModel;
            }

            @Override
            public ModelNode getSubModel() throws IllegalArgumentException {
                return stepModel;
            }

            @Override
            public ModelNodeRegistration getRegistry() {
                return overallRuntimeContext.getRegistry();
            }

            @Override
            public ServerController getController() {
                return overallRuntimeContext.getController();
            }

            @Override
            public void restartRequired() {
                overallRuntimeContext.restartRequired();
            }

            @Override
            public void revertRestartRequired() {
                overallRuntimeContext.revertRestartRequired();
            }

            @Override
            public RuntimeOperationContext getRuntimeContext() {
                return this;
            }

            @Override
            public void setRuntimeTask(RuntimeTask runtimeTask) {
                runtimeTasks.push(runtimeTask);
            }
        }

    }
}
