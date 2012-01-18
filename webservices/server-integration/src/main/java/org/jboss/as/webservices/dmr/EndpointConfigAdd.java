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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * @author <a href="ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class EndpointConfigAdd extends AbstractAddStepHandler {

    static final EndpointConfigAdd INSTANCE = new EndpointConfigAdd();

    private EndpointConfigAdd() {}

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // does nothing
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        // TODO: providy utility method to do this lookup
        final ServiceController<?> configService = context.getServiceRegistry(true).getService(WSServices.CONFIG_SERVICE);
        if (configService != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String name = address.getLastElement().getValue();

            ServerConfig config = (ServerConfig) configService.getValue();
            EndpointConfig endpointConfig = new EndpointConfig();
            endpointConfig.setConfigName(name);
            // TODO: remove this code
            /*
            if (model.hasDefined(Constants.PRE_HANDLER_CHAINS)) {
                ModelNode preHandlers = model.get(Constants.PRE_HANDLER_CHAINS);
                endpointConfig.setPreHandlerChains(buildChainMD(preHandlers));
            }
            if (model.hasDefined(Constants.POST_HANDLER_CHAINS)) {
                ModelNode postHandlers = model.get(Constants.POST_HANDLER_CHAINS);
                endpointConfig.setPostHandlerChains(buildChainMD(postHandlers));
            }

            if (model.hasDefined(Constants.PROPERTY)) {
                for (String key : model.get(Constants.PROPERTY).keys()) {
                    endpointConfig.setProperty(key, model.get(Constants.PROPERTY).get(key).asString());
                }
            }
            if (model.hasDefined(Constants.FEATURE)) {
                for (String key : model.get(Constants.FEATURE).keys()) {
                    endpointConfig.setFeature(new Feature(key), true);
                }
            }
            */
            config.addEndpointConfig(endpointConfig);
        }
    }


    private List<UnifiedHandlerChainMetaData> buildChainMD(ModelNode handlerChainsNode) {
        List<UnifiedHandlerChainMetaData> handlerChains = new ArrayList<UnifiedHandlerChainMetaData>();
        if (handlerChainsNode.getType() == ModelType.LIST) {
            for (ModelNode chainNode : handlerChainsNode.asList()) {
                UnifiedHandlerChainMetaData chainMetaData = new UnifiedHandlerChainMetaData();
                if (chainNode.hasDefined(Constants.PROTOCOL_BINDING)) {
                    chainMetaData.setProtocolBindings(chainNode.get(Constants.PROTOCOL_BINDING).asString());
                }
                if (chainNode.hasDefined(Constants.SERVICE_NAME_PATTERN)) {
                    chainMetaData.setServiceNamePattern(new QName(chainNode.get(Constants.SERVICE_NAME_PATTERN).asString()));
                }
                if (chainNode.hasDefined(Constants.PORT_NAME_PATTERN)) {
                    chainMetaData.setPortNamePattern(new QName(chainNode.get(Constants.PORT_NAME_PATTERN).asString()));
                }
                if (chainNode.hasDefined(Constants.HANDLER)) {
                    for (String key : chainNode.get(Constants.HANDLER).keys()) {
                        UnifiedHandlerMetaData handlerMD = new UnifiedHandlerMetaData();
                        handlerMD.setHandlerName(key);
                        handlerMD.setHandlerClass(chainNode.get(Constants.HANDLER).get(key).asString());
                        chainMetaData.addHandler(handlerMD);
                    }

                }
                handlerChains.add(chainMetaData);
            }
        }
        return handlerChains;

    }
}
