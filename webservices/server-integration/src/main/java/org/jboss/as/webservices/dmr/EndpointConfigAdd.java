/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

import java.util.ArrayList;
import java.util.List;

public class EndpointConfigAdd implements ModelAddOperationHandler {

    static final EndpointConfigAdd INSTANCE = new EndpointConfigAdd();

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set(REMOVE);

        final ModelNode postHandlers = operation.hasDefined(Constants.POST_HANDLER_CHAINS) ? operation
                .get(Constants.POST_HANDLER_CHAINS) : new ModelNode();
        final ModelNode preHandlers = operation.hasDefined(Constants.PRE_HANDLER_CHAINS) ? operation
                .get(Constants.PRE_HANDLER_CHAINS) : new ModelNode();

        final ModelNode subModel = context.getSubModel();
        subModel.get(Constants.PRE_HANDLER_CHAINS).set(preHandlers);
        subModel.get(Constants.POST_HANDLER_CHAINS).set(postHandlers);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ServiceController<?> configService = context.getServiceRegistry().getService(WSServices.CONFIG_SERVICE);
                    if (configService == null) {

                    } else {
                        ServerConfig config = (ServerConfig) configService.getValue();
                        EndpointConfig endpointConfig = new EndpointConfig();
                        endpointConfig.setConfigName(name);
                        endpointConfig.setPreHandlerChains(buildChainMD(preHandlers));
                        endpointConfig.setPostHandlerChains(buildChainMD(postHandlers));
                        config.addEndpointConfig(endpointConfig);
                    }

                    resultHandler.handleResultComplete();
                }
            });

        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }


    private List<UnifiedHandlerChainMetaData> buildChainMD(ModelNode handlerChainsNode) {
        List<UnifiedHandlerChainMetaData> handlerChains = new ArrayList<UnifiedHandlerChainMetaData>();
        if (handlerChainsNode.getType() == ModelType.LIST) {
            for (ModelNode chainNode : handlerChainsNode.asList()) {
                UnifiedHandlerChainMetaData chainMetaData = new UnifiedHandlerChainMetaData();
                if (chainNode.hasDefined(Constants.PROTOCOL_BINDING)) {
                    chainMetaData.setProtocolBindings(chainNode.get(Constants.PROTOCOL_BINDING).asString());
                }
                //TODO:handler others
                if (chainNode.hasDefined(Constants.HANDLER)) {
                    for (String key : chainNode.get(Constants.HANDLER).keys()) {
                        UnifiedHandlerMetaData handlerMD = new UnifiedHandlerMetaData();
                        handlerMD.setHandlerName(key);
                        handlerMD.setHandlerName(chainNode.get(Constants.HANDLER).get(key).asString());
                        chainMetaData.addHandler(handlerMD);
                    }

                }
                handlerChains.add(chainMetaData);

            }
        }
        return handlerChains;

    }
}