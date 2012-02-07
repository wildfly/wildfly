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
import static org.jboss.as.webservices.dmr.Constants.CLASS;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
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
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class HandlerAdd extends AbstractAddStepHandler {

    static final HandlerAdd INSTANCE = new HandlerAdd();

    private HandlerAdd() {
        // forbidden instantiation
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final ServerConfig config = getServerConfig(context);
        if (config != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String configName = address.getElement(address.size() - 3).getValue();
            final String handlerChainType = address.getElement(address.size() - 2).getKey();
            final String handlerChainId = address.getElement(address.size() - 2).getValue();
            final String handlerName = address.getElement(address.size() - 1).getValue();
            final String handlerClass = operation.require(CLASS).asString();
            for (final EndpointConfig endpointConfig : config.getEndpointConfigs()) {
                if (configName.equals(endpointConfig.getConfigName())) {
                    final List<UnifiedHandlerChainMetaData> handlerChains;
                    if (PRE_HANDLER_CHAIN.equals(handlerChainType)) {
                        handlerChains = endpointConfig.getPreHandlerChains();
                    } else if (POST_HANDLER_CHAIN.equals(handlerChainType)) {
                        handlerChains = endpointConfig.getPostHandlerChains();
                    } else {
                        throw MESSAGES.wrongHandlerChainType(handlerChainType, PRE_HANDLER_CHAIN, POST_HANDLER_CHAIN);
                    }
                    final UnifiedHandlerChainMetaData handlerChain = getChain(handlerChains, handlerChainId);
                    if (handlerChain == null) {
                        throw MESSAGES.multipleHandlerChainsWithSameId(handlerChainType, handlerChainId, configName);
                    }
                    final UnifiedHandlerMetaData handler = new UnifiedHandlerMetaData();
                    handler.setHandlerName(handlerName);
                    handler.setHandlerClass(handlerClass);
                    handlerChain.addHandler(handler);
                    if (!context.isBooting()) {
                        context.restartRequired();
                    }
                    return;
                }
            }
            throw MESSAGES.missingEndpointConfig(configName);
        }
    }

    private static UnifiedHandlerChainMetaData getChain(final List<UnifiedHandlerChainMetaData> handlerChains, final String handlerChainId) {
        if (handlerChains != null) {
            for (final UnifiedHandlerChainMetaData handlerChain : handlerChains) {
                if (handlerChainId.equals(handlerChain.getId())) return handlerChain;
            }
        }
        return null;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        if (operation.hasDefined(CLASS)) {
            model.get(CLASS).set(operation.get(CLASS));
        }
    }

}
