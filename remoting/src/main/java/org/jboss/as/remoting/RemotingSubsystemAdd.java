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

package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Add operation handler for the remoting subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class RemotingSubsystemAdd extends AbstractAddStepHandler {

    static final RemotingSubsystemAdd DOMAIN = new RemotingSubsystemAdd(false);
    static final RemotingSubsystemAdd SERVER = new RemotingSubsystemAdd(true);

    private final boolean server;

    private RemotingSubsystemAdd(final boolean server) {
        this.server = server;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // initialize the connectors
        model.get(CONNECTOR);
        RemotingSubsystemRootResource.WORKER_READ_THREADS.validateAndSet(operation, model);
        RemotingSubsystemRootResource.WORKER_TASK_CORE_THREADS.validateAndSet(operation, model);
        RemotingSubsystemRootResource.WORKER_TASK_KEEPALIVE.validateAndSet(operation, model);
        RemotingSubsystemRootResource.WORKER_TASK_LIMIT.validateAndSet(operation, model);
        RemotingSubsystemRootResource.WORKER_TASK_MAX_THREADS.validateAndSet(operation, model);
        RemotingSubsystemRootResource.WORKER_WRITE_THREADS.validateAndSet(operation, model);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return server;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        launchServices(context, model, verificationHandler, newControllers);
    }

    void launchServices(OperationContext context, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        // create endpoint
        final EndpointService endpointService = new EndpointService(RemotingExtension.NODE_NAME, EndpointService.EndpointType.SUBSYSTEM);
        // todo configure option map
        final OptionMap map = OptionMap.builder()
                .set(Options.WORKER_READ_THREADS, RemotingSubsystemRootResource.WORKER_READ_THREADS.resolveModelAttribute(context, model).asInt())
                .set(Options.WORKER_TASK_CORE_THREADS, RemotingSubsystemRootResource.WORKER_TASK_CORE_THREADS.resolveModelAttribute(context, model).asInt())
                .set(Options.WORKER_TASK_KEEPALIVE, RemotingSubsystemRootResource.WORKER_TASK_KEEPALIVE.resolveModelAttribute(context, model).asInt())
                .set(Options.WORKER_TASK_LIMIT, RemotingSubsystemRootResource.WORKER_TASK_LIMIT.resolveModelAttribute(context, model).asInt())
                .set(Options.WORKER_TASK_MAX_THREADS, RemotingSubsystemRootResource.WORKER_TASK_MAX_THREADS.resolveModelAttribute(context, model).asInt())
                .set(Options.WORKER_WRITE_THREADS, RemotingSubsystemRootResource.WORKER_WRITE_THREADS.resolveModelAttribute(context, model).asInt())
                .set(Options.WORKER_READ_THREADS, RemotingSubsystemRootResource.WORKER_READ_THREADS.resolveModelAttribute(context, model).asInt())
                .getMap();
        endpointService.setOptionMap(map);

        ServiceBuilder<?> builder = context.getServiceTarget()
                .addService(RemotingServices.SUBSYSTEM_ENDPOINT, endpointService);

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        ServiceController<?> controller = builder.install();
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }
}
