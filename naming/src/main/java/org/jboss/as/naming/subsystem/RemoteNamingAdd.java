/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.subsystem;

import static org.jboss.as.naming.subsystem.RemoteNamingResourceDefinition.REMOTING_ENDPOINT_CAPABILITY_NAME;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.remote.RemoteNamingServerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.remoting3.Endpoint;
import org.wildfly.naming.client.remote.RemoteNamingService;

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
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        installRuntimeServices(context);
    }

    void installRuntimeServices(final OperationContext context) {

        final RemoteNamingServerService remoteNamingServerService = new RemoteNamingServerService();

        final ServiceBuilder<RemoteNamingService> builder = context.getServiceTarget().addService(RemoteNamingServerService.SERVICE_NAME, remoteNamingServerService);
        builder.addDependency(context.getCapabilityServiceName(REMOTING_ENDPOINT_CAPABILITY_NAME, Endpoint.class), Endpoint.class, remoteNamingServerService.getEndpointInjector())
                .addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, remoteNamingServerService.getNamingStoreInjector())
                .install();

    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }
}
