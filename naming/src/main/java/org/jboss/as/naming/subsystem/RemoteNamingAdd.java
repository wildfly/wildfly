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
package org.jboss.as.naming.subsystem;

import java.util.List;
import java.util.concurrent.Executors;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.remote.RemoteNamingServerService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.naming.remote.server.RemoteNamingService;
import org.jboss.remoting3.Endpoint;

/**
 * A {@link org.jboss.as.controller.AbstractAddStepHandler} to handle the add operation for simple JNDI bindings
 *
 * @author Stuart Douglas
 */
public class RemoteNamingAdd extends AbstractAddStepHandler {

    static final RemoteNamingAdd INSTANCE = new RemoteNamingAdd();

    private RemoteNamingAdd() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        installRuntimeServices(context, verificationHandler, newControllers);
    }

    void installRuntimeServices(final OperationContext context, ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final RemoteNamingServerService remoteNamingServerService = new RemoteNamingServerService();

        final ServiceBuilder<RemoteNamingService> builder = context.getServiceTarget().addService(RemoteNamingServerService.SERVICE_NAME, remoteNamingServerService);
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        final ServiceController<RemoteNamingService> controller = builder
                .addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, remoteNamingServerService.getEndpointInjector())
                .addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, remoteNamingServerService.getNamingStoreInjector())
                .addInjection(remoteNamingServerService.getExecutorServiceInjector(), Executors.newFixedThreadPool(10))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        if (newControllers != null) {
            newControllers.add(controller);
        }

    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }
}
