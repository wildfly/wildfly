/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.webservices.service.ClientConfigService;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.management.ServerConfig;

/**
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class ClientConfigAdd extends AbstractAddStepHandler {

    static final ClientConfigAdd INSTANCE = new ClientConfigAdd();

    private ClientConfigAdd() {
        // forbidden instantiation
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        // does nothing
    }

    @Override
    protected void rollbackRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final List<ServiceController<?>> controllers) {
        super.rollbackRuntime(context, operation, model, controllers);
        if (!context.isBooting()) {
            context.revertReloadRequired();
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ClientConfigService clientConfigService = new ClientConfigService(name);
        final ServiceTarget target = context.getServiceTarget();
        final ServiceBuilder<?> clientServiceBuilder = target.addService(WSServices.CLIENT_CONFIG_SERVICE.append(name), clientConfigService);

        clientServiceBuilder.addDependency(WSServices.CONFIG_SERVICE, ServerConfig.class, clientConfigService.getServerConfig());

        clientServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();

        if (!context.isBooting()) {
            context.reloadRequired();
        }
    }

}
