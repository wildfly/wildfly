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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.webservices.dmr.Constants.CLASS;
import static org.jboss.as.webservices.dmr.Constants.CLIENT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDINGS;
import static org.jboss.as.webservices.dmr.Constants.VALUE;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class WSSubsystemDescribe implements OperationStepHandler {

    static final WSSubsystemDescribe INSTANCE = new WSSubsystemDescribe();

    private WSSubsystemDescribe() {
        // forbidden inheritance
    }

    /** {@inheritDoc} */
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode wsSubsystemAddress = rootAddress.toModelNode();
        final ModelNode wsSubsystem = context.readModel(PathAddress.EMPTY_ADDRESS);
        final ModelNode result = context.getResult();

        final ModelNode subsystemAdd = getSubsystemAddOperation(wsSubsystemAddress, wsSubsystem);
        result.add(subsystemAdd);

        if (wsSubsystem.hasDefined(ENDPOINT_CONFIG)) {
            processConfig(wsSubsystem, ENDPOINT_CONFIG, wsSubsystemAddress, result);
        }
        if (wsSubsystem.hasDefined(CLIENT_CONFIG)) {
            processConfig(wsSubsystem, CLIENT_CONFIG, wsSubsystemAddress, result);
        }

        context.completeStep();
    }

    private static void processConfig(final ModelNode wsSubsystem, final String elementName, final ModelNode wsSubsystemAddress, final ModelNode result) {
        final ModelNode configs = wsSubsystem.get(elementName);
        for (final String configName : configs.keys()) {
            final ModelNode config = configs.get(configName);
            final ModelNode configAddress = wsSubsystemAddress.clone().add(elementName, configName);
            final ModelNode configAdd = getConfigAddOperation(configAddress);
            result.add(configAdd);

            if (config.hasDefined(PRE_HANDLER_CHAIN)) {
                final ModelNode preHandlerChains = config.get(PRE_HANDLER_CHAIN);
                for (final String preHandlerChainName : preHandlerChains.keys()) {
                    final ModelNode preHandlerChain = preHandlerChains.get(preHandlerChainName);
                    final ModelNode preHandlerChainAddress = configAddress.clone().add(PRE_HANDLER_CHAIN, preHandlerChainName);
                    final ModelNode preHandlerChainAdd = getHandlerChainAddOperation(preHandlerChainAddress, preHandlerChain);
                    result.add(preHandlerChainAdd);

                    if (preHandlerChain.hasDefined(HANDLER)) {
                        final ModelNode handlers = preHandlerChain.get(HANDLER);
                        for (final String handlerName : handlers.keys()) {
                            final ModelNode handler = handlers.get(handlerName);
                            final ModelNode handlerAddress = preHandlerChainAddress.clone().add(HANDLER, handlerName);
                            final ModelNode handlerAdd = getHandlerAddOperation(handlerAddress, handler);
                            result.add(handlerAdd);
                        }
                    }
                }
            }

            if (config.hasDefined(POST_HANDLER_CHAIN)) {
                final ModelNode postHandlerChains = config.get(POST_HANDLER_CHAIN);
                for (final String postHandlerChainName : postHandlerChains.keys()) {
                    final ModelNode postHandlerChain = postHandlerChains.get(postHandlerChainName);
                    final ModelNode postHandlerChainAddress = configAddress.clone().add(POST_HANDLER_CHAIN, postHandlerChainName);
                    final ModelNode postHandlerChainAdd = getHandlerChainAddOperation(postHandlerChainAddress, postHandlerChain);
                    result.add(postHandlerChainAdd);

                    if (postHandlerChain.hasDefined(HANDLER)) {
                        final ModelNode handlers = postHandlerChain.get(HANDLER);
                        for (final String handlerName : handlers.keys()) {
                            final ModelNode handler = handlers.get(handlerName);
                            final ModelNode handlerAddress = postHandlerChainAddress.clone().add(HANDLER, handlerName);
                            final ModelNode handlerAdd = getHandlerAddOperation(handlerAddress, handler);
                            result.add(handlerAdd);
                        }
                    }
                }
            }

            if (config.hasDefined(PROPERTY)) {
                final ModelNode properties = config.get(PROPERTY);
                for (final String propertyName : properties.keys()) {
                    final ModelNode property = properties.get(propertyName);
                    final ModelNode propertyAddress = configAddress.clone().add(PROPERTY, propertyName);
                    final ModelNode propertyAdd = getPropertyAddOperation(propertyAddress, property);
                    result.add(propertyAdd);
                }
            }
        }
    }

    private static ModelNode getSubsystemAddOperation(final ModelNode wsSubsystemAddress, final ModelNode wsSubsystem) {
        final ModelNode wsSubsystemAdd = new ModelNode();
        wsSubsystemAdd.get(OP).set(ADD);
        wsSubsystemAdd.get(OP_ADDR).set(wsSubsystemAddress);
        if (wsSubsystem.hasDefined(MODIFY_WSDL_ADDRESS)) {
            wsSubsystemAdd.get(MODIFY_WSDL_ADDRESS).set(wsSubsystem.get(MODIFY_WSDL_ADDRESS));
        }
        if (wsSubsystem.hasDefined(WSDL_HOST)) {
            wsSubsystemAdd.get(WSDL_HOST).set(wsSubsystem.get(WSDL_HOST));
        }
        if (wsSubsystem.hasDefined(WSDL_PORT)) {
            wsSubsystemAdd.get(WSDL_PORT).set(wsSubsystem.get(WSDL_PORT));
        }
        if (wsSubsystem.hasDefined(WSDL_SECURE_PORT)) {
            wsSubsystemAdd.get(WSDL_SECURE_PORT).set(wsSubsystem.get(WSDL_SECURE_PORT));
        }
        return wsSubsystemAdd;
    }

    private static ModelNode getConfigAddOperation(final ModelNode configAddress) {
        final ModelNode configAdd = new ModelNode();
        configAdd.get(OP).set(ADD);
        configAdd.get(OP_ADDR).set(configAddress);
        return configAdd;
    }

    private static ModelNode getPropertyAddOperation(final ModelNode propertyAddress, final ModelNode property) {
        final ModelNode propertyAdd = new ModelNode();
        propertyAdd.get(OP).set(ADD);
        propertyAdd.get(OP_ADDR).set(propertyAddress);
        if (property.hasDefined(VALUE)) {
            propertyAdd.get(VALUE).set(property.get(VALUE).asString());
        }
        return propertyAdd;
    }

    private static ModelNode getHandlerChainAddOperation(final ModelNode handlerChainAddress, final ModelNode handlerChain) {
        final ModelNode handlerChainAdd = new ModelNode();
        handlerChainAdd.get(OP).set(ADD);
        handlerChainAdd.get(OP_ADDR).set(handlerChainAddress);
        if (handlerChain.hasDefined(PROTOCOL_BINDINGS)) {
            handlerChainAdd.get(PROTOCOL_BINDINGS).set(handlerChain.get(PROTOCOL_BINDINGS).asString());
        }
        return handlerChainAdd;
    }

    private static ModelNode getHandlerAddOperation(final ModelNode handlerAddress, final ModelNode handler) {
        final ModelNode handlerAdd = new ModelNode();
        handlerAdd.get(OP).set(ADD);
        handlerAdd.get(OP_ADDR).set(handlerAddress);
        if (handler.hasDefined(CLASS)) {
            handlerAdd.get(CLASS).set(handler.get(CLASS).asString());
        }
        return handlerAdd;
    }

}
