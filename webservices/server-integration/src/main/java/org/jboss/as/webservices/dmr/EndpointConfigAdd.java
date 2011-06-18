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
import org.jboss.wsf.spi.metadata.config.Feature;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

/**
 * OperationHandler to add an endpoint configuration to {@link org.jboss.as.webservices.service.ServerConfigService ServerConfigService}
 * @author <a href="ema@redhat.com">Jim Ma</a>
 */
public class EndpointConfigAdd implements ModelAddOperationHandler {

    static final EndpointConfigAdd INSTANCE = new EndpointConfigAdd();

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set(REMOVE);

        final ModelNode subModel = context.getSubModel();
        if (operation.hasDefined(Constants.PRE_HANDLER_CHAINS)) {
            ModelNode preHandlers = operation.get(Constants.PRE_HANDLER_CHAINS);
            subModel.get(Constants.PRE_HANDLER_CHAINS).set(preHandlers);
        }

        if (operation.hasDefined(Constants.POST_HANDLER_CHAINS)) {
            ModelNode postHandlers = operation.get(Constants.POST_HANDLER_CHAINS);
            subModel.get(Constants.POST_HANDLER_CHAINS).set(postHandlers);
        }

        if (operation.hasDefined(Constants.PROPERTY)) {
            ModelNode property = operation.get(Constants.PROPERTY);
            subModel.get(Constants.PROPERTY).set(property);
        }
        if (operation.hasDefined(Constants.FEATURE)) {
            ModelNode feature = operation.get(Constants.FEATURE);
            subModel.get(Constants.FEATURE).set(feature);
        }

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ServiceController<?> configService = context.getServiceRegistry().getService(WSServices.CONFIG_SERVICE);
                    if (configService != null) {
                        ServerConfig config = (ServerConfig) configService.getValue();
                        EndpointConfig endpointConfig = new EndpointConfig();
                        endpointConfig.setConfigName(name);
                        if (subModel.hasDefined(Constants.PRE_HANDLER_CHAINS)) {
                            ModelNode preHandlers =subModel.get(Constants.PRE_HANDLER_CHAINS);
                            endpointConfig.setPreHandlerChains(buildChainMD(preHandlers));
                        }
                        if (subModel.hasDefined(Constants.POST_HANDLER_CHAINS)) {
                            ModelNode postHandlers =subModel.get(Constants.POST_HANDLER_CHAINS);
                            endpointConfig.setPostHandlerChains(buildChainMD(postHandlers));
                        }

                        if (subModel.hasDefined(Constants.PROPERTY)) {
                            for (String name :subModel.get(Constants.PROPERTY).keys()) {
                                endpointConfig.setProperty(name, subModel.get(Constants.PROPERTY).get(name).asString());
                            }
                        }
                        if (subModel.hasDefined(Constants.FEATURE)) {
                            for (String name :subModel.get(Constants.FEATURE).keys()) {
                                endpointConfig.setFeature(new Feature(name), true);
                            }
                        }
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