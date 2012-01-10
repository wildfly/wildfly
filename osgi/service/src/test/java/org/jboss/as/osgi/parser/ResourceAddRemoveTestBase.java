/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.parser;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author David Bosschaert
 */
class ResourceAddRemoveTestBase {

    private final AtomicReference<ModelNode> operationHolder = new AtomicReference<ModelNode>();

    @SuppressWarnings({ "unchecked" })
    protected OperationContext mockOperationContext(SubsystemState stateService, final List<OperationStepHandler> addedSteps,
                                                    final ResultAction stepResult) {
        ServiceRegistry serviceRegistry = Mockito.mock(ServiceRegistry.class);
        ServiceController serviceController = Mockito.mock(ServiceController.class);
        Mockito.when(serviceController.getValue()).thenReturn(stateService);
        Mockito.when(serviceRegistry.getService(SubsystemState.SERVICE_NAME)).thenReturn(serviceController);
        ModelNode result = new ModelNode();
        final OperationContext context = Mockito.mock(OperationContext.class);
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getModel()).thenReturn(result);
        Mockito.when(context.getServiceRegistry(true)).thenReturn(serviceRegistry);
        Mockito.when(context.completeStep()).thenReturn(stepResult);
        Mockito.when(context.createResource(PathAddress.EMPTY_ADDRESS)).thenReturn(resource);
        Mockito.when(context.readResource(PathAddress.EMPTY_ADDRESS)).thenReturn(resource);
        Mockito.when(context.getType()).thenReturn(OperationContext.Type.SERVER);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                addedSteps.add((OperationStepHandler) invocation.getArguments()[0]);
                return null;
            }
        }).when(context).addStep((OperationStepHandler) Mockito.anyObject(), Mockito.eq(OperationContext.Stage.RUNTIME));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (stepResult == ResultAction.ROLLBACK) {
                    Object[] args = invocation.getArguments();
                    OperationContext.RollbackHandler handler = OperationContext.RollbackHandler.class.cast(args[0]);
                    handler.handleRollback(context, operationHolder.get());
                }
                return null;
            }
        }).when(context).completeStep(Mockito.any(OperationContext.RollbackHandler.class));
        return context;
    }

    protected void execute(final OperationStepHandler handler, final OperationContext context,
                                final ModelNode op) throws OperationFailedException {
        operationHolder.set(op);
        handler.execute(context, op);
    }

    protected void configureForRollback(final OperationContext context, final ModelNode operation) {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                OperationContext.RollbackHandler handler = OperationContext.RollbackHandler.class.cast(args[0]);
                handler.handleRollback(context, operation);
                return null;
            }
        }).when(context).completeStep(Mockito.any(OperationContext.RollbackHandler.class));
        Mockito.when(context.completeStep()).thenReturn(OperationContext.ResultAction.ROLLBACK);
    }

    protected void configureForSuccess(final OperationContext context) {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(context).completeStep(Mockito.any(OperationContext.RollbackHandler.class));
        Mockito.when(context.completeStep()).thenReturn(OperationContext.ResultAction.KEEP);
    }
}
