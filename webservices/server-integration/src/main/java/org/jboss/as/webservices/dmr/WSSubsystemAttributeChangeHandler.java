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

import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.management.ServerConfig;

/**
 * @author <a href="mailto:opalka.richard@gmail.com">Richard Opalka</a>
 */
final class WSSubsystemAttributeChangeHandler extends AbstractWriteAttributeHandler<Void> {

    WSSubsystemAttributeChangeHandler(final ParameterValidator valueValidator) {
        super(valueValidator);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> voidHandback) throws OperationFailedException {
        final ServiceController<?> configService = context.getServiceRegistry(true).getService(WSServices.CONFIG_SERVICE);
        if (configService != null) {
            final ServerConfig config = (ServerConfig) configService.getValue();
            final String value = resolvedValue.isDefined() ? resolvedValue.asString() : null;

            if (MODIFY_WSDL_ADDRESS.equals(attributeName)) {
                final boolean modifyWSDLAddress = value != null ? Boolean.parseBoolean(value) : false;
                config.setModifySOAPAddress(modifyWSDLAddress);
            } else if (WSDL_HOST.equals(attributeName)) {
                final String host = value != null ? value : null;
                try {
                    config.setWebServiceHost(host);
                } catch (final Exception e) {
                    throw new OperationFailedException(e.getMessage(), e);
                }
            } else if (WSDL_PORT.equals(attributeName)) {
                final int port = value != null ? Integer.parseInt(value) : -1;
                config.setWebServicePort(port);
            } else if (WSDL_SECURE_PORT.equals(attributeName)) {
                final int securePort = value != null ? Integer.parseInt(value) : -1;
                config.setWebServiceSecurePort(securePort);
            }
        }

        return true;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final Void handback) throws OperationFailedException {
        // does nothing
    }

}
