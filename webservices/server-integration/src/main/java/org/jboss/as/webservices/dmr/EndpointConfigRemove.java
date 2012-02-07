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
package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.webservices.dmr.PackageUtils.getServerConfig;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;

/**
 * OperationHandler to remove the endpoint configuration
 *
 * @author <a href="ema@redhat.com">Jim Ma</a>
 */
final class EndpointConfigRemove extends AbstractRemoveStepHandler {

    static final EndpointConfigRemove INSTANCE = new EndpointConfigRemove();

    private EndpointConfigRemove() {
        // forbidden instantiation
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final ServerConfig config = getServerConfig(context);
        if (config != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String name = address.getLastElement().getValue();

            EndpointConfig target = null;
            for (EndpointConfig epConfig : config.getEndpointConfigs()) {
                if (epConfig.getConfigName().equals(name)) {
                    target = epConfig;
                }
            }
            if (target != null) {
                config.getEndpointConfigs().remove(target);
                context.restartRequired();
            }
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        EndpointConfigAdd.INSTANCE.performRuntime(context, operation, model, null, null);
    }
}
