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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.webservices.WSMessages.MESSAGES;
import static org.jboss.as.webservices.dmr.PackageUtils.getServerConfig;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class PropertyAdd extends AbstractAddStepHandler {

    static final PropertyAdd INSTANCE = new PropertyAdd();

    private PropertyAdd() {
        // forbidden instantiation
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final ServerConfig config = getServerConfig(context);
        if (config != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String propertyName = address.getElement(address.size() - 1).getValue();
            final String configName = address.getElement(address.size() - 2).getValue();
            final String propertyValue = operation.has(VALUE) ? operation.get(VALUE).asString() : null;
            for (final EndpointConfig endpointConfig : config.getEndpointConfigs()) {
                if (configName.equals(endpointConfig.getConfigName())) {
                    endpointConfig.setProperty(propertyName, propertyValue);
                    if (!context.isBooting()) {
                        context.restartRequired();
                    }
                    return;
                }
            }
            throw MESSAGES.missingEndpointConfig(configName);
        }
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        if (operation.hasDefined(VALUE)) {
            final ModelNode propertyValue = operation.get(VALUE);
            model.get(VALUE).set(propertyValue);
        }
    }

}
