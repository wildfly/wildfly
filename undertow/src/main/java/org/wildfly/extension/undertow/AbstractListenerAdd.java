/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.wildfly.extension.io.IOServices;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.xnio.Pool;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
abstract class AbstractListenerAdd extends AbstractAddStepHandler {
    private final AbstractListenerResourceDefinition listenerDefinition;


    AbstractListenerAdd(AbstractListenerResourceDefinition definition) {
        this.listenerDefinition = definition;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : listenerDefinition.getAttributes()) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress parent = address.subAddress(0, address.size() - 1);
        String name = address.getLastElement().getValue();
        String bindingRef = AbstractListenerResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        String workerName = AbstractListenerResourceDefinition.WORKER.resolveModelAttribute(context, model).asString();
        String bufferPoolName = AbstractListenerResourceDefinition.BUFFER_POOL.resolveModelAttribute(context, model).asString();
        boolean enabled = AbstractListenerResourceDefinition.ENABLED.resolveModelAttribute(context, model).asBoolean();
        String serverName = parent.getLastElement().getValue();
        if (enabled) {
            final AbstractListenerService<? extends AbstractListenerService> service = createService(name, context, model);
            final ServiceBuilder<? extends AbstractListenerService> serviceBuilder = context.getServiceTarget().addService(constructServiceName(name), service);
            serviceBuilder.addDependency(IOServices.WORKER.append(workerName), XnioWorker.class, service.getWorker())
                    .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding())
                    .addDependency(IOServices.BUFFER_POOL.append(bufferPoolName), Pool.class, service.getBufferPool())
                    .addDependency(UndertowService.SERVER.append(serverName), Server.class, service.getServerService());

            configureAdditionalDependencies(context, serviceBuilder, model, service);

            final ServiceController<? extends AbstractListenerService> serviceController = serviceBuilder.install();
            if (newControllers != null) {
                newControllers.add(serviceController);
            }
        }
    }

    abstract ServiceName constructServiceName(final String name);

    abstract AbstractListenerService<? extends AbstractListenerService> createService(final String name, final OperationContext context, ModelNode model) throws OperationFailedException;

    abstract void configureAdditionalDependencies(OperationContext context, ServiceBuilder<? extends AbstractListenerService> serviceBuilder, ModelNode model, AbstractListenerService service) throws OperationFailedException;


}
