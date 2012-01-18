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
import static org.jboss.as.webservices.WSMessages.MESSAGES;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class EndpointConfigPropertyRemove extends AbstractRemoveStepHandler {

    static final EndpointConfigPropertyRemove INSTANCE = new EndpointConfigPropertyRemove();

    private EndpointConfigPropertyRemove() {}

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final ServiceController<?> configService = context.getServiceRegistry(true).getService(WSServices.CONFIG_SERVICE);
        if (configService != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String propertyName = address.getElement(address.size() - 1).getValue();
            final String configName = address.getElement(address.size() - 2).getValue();
            final ServerConfig config = (ServerConfig) configService.getValue();
            for (final EndpointConfig endpointConfig : config.getEndpointConfigs()) {
                if (configName.equals(endpointConfig.getConfigName())) {
                    endpointConfig.getProperties().remove(propertyName);
                    context.restartRequired();
                    return;
                }
            }
            throw MESSAGES.missingEndpointConfig(configName);
        }
    }

    @Override
    protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        EndpointConfigFeatureAdd.INSTANCE.performRuntime(context, operation, model, null, null);
    }

}
