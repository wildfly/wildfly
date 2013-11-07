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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.FACTORY_CLASS;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
/**
 * Basic {@link TransportConfiguration} (Acceptor/Connector) related operations.
 *
 * @author Emanuel Muckenhuber
 */
class TransportConfigOperationHandlers {

    /**
     * Process the acceptor information.
     *
     * @param context       the operation context
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     * @param bindings      the referenced socket bindings
     * @throws OperationFailedException
     */
    static void processAcceptors(final OperationContext context, final Configuration configuration, final ModelNode params, final Set<String> bindings) throws OperationFailedException {
        final Map<String, TransportConfiguration> acceptors = new HashMap<String, TransportConfiguration>();
        if (params.hasDefined(ACCEPTOR)) {
            for (final Property property : params.get(ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config);
                final String clazz = config.get(FACTORY_CLASS.getName()).asString();
                acceptors.put(acceptorName, new TransportConfiguration(clazz, parameters, acceptorName));
            }
        }
        if (params.hasDefined(REMOTE_ACCEPTOR)) {
            for (final Property property : params.get(REMOTE_ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config);
                final String binding = config.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).asString();
                parameters.put(RemoteTransportDefinition.SOCKET_BINDING.getName(), binding);
                bindings.add(binding);
                acceptors.put(acceptorName, new TransportConfiguration(NettyAcceptorFactory.class.getName(), parameters, acceptorName));
            }
        }
        if (params.hasDefined(IN_VM_ACCEPTOR)) {
            for (final Property property : params.get(IN_VM_ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config);
                parameters.put(InVMTransportDefinition.SERVER_ID.getName(), InVMTransportDefinition.SERVER_ID.resolveModelAttribute(context, config).asInt());
                acceptors.put(acceptorName, new TransportConfiguration(InVMAcceptorFactory.class.getName(), parameters, acceptorName));
            }
        }
        configuration.setAcceptorConfigurations(new HashSet<TransportConfiguration>(acceptors.values()));
    }

    /**
     * Get the parameters.
     *
     * @param context the operation context
     * @param config the transport configuration
     * @return the extracted parameters
     * @throws OperationFailedException if an expression can not be resolved
     */
    static Map<String, Object> getParameters(final OperationContext context, final ModelNode config) throws OperationFailedException {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        if (config.hasDefined(PARAM)) {
            for (final Property parameter : config.get(PARAM).asPropertyList()) {
                String name = parameter.getName();
                String value = TransportParamDefinition.VALUE.resolveModelAttribute(context, parameter.getValue()).asString();
                parameters.put(name, value);
            }
        }
        return parameters;
    }

    /**
     * Process the connector information.
     *
     * @param context       the operation context
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     * @param bindings      the referenced socket bindings
     * @throws OperationFailedException
     */
    static void processConnectors(final OperationContext context, final Configuration configuration, final ModelNode params, final Set<String> bindings) throws OperationFailedException {
        final Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();
        if (params.hasDefined(CONNECTOR)) {
            for (final Property property : params.get(CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config);
                final String clazz = FACTORY_CLASS.resolveModelAttribute(context, config).asString();
                connectors.put(connectorName, new TransportConfiguration(clazz, parameters, connectorName));
            }
        }
        if (params.hasDefined(REMOTE_CONNECTOR)) {
            for (final Property property : params.get(REMOTE_CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config);
                final String binding = config.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).asString();
                parameters.put(RemoteTransportDefinition.SOCKET_BINDING.getName(), binding);
                bindings.add(binding);
                connectors.put(connectorName, new TransportConfiguration(NettyConnectorFactory.class.getName(), parameters, connectorName));
            }
        }
        if (params.hasDefined(IN_VM_CONNECTOR)) {
            for (final Property property : params.get(IN_VM_CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config);
                parameters.put(InVMTransportDefinition.SERVER_ID.getName(), InVMTransportDefinition.SERVER_ID.resolveModelAttribute(context, config).asInt());
                connectors.put(connectorName, new TransportConfiguration(InVMConnectorFactory.class.getName(), parameters, connectorName));
            }
        }
        configuration.setConnectorConfigurations(connectors);
    }

    static class BasicTransportConfigAdd extends HornetQReloadRequiredHandlers.AddStepHandler implements DescriptionProvider {

        private final AttributeDefinition[] attributes;
        private final boolean isAcceptor;

        BasicTransportConfigAdd(final boolean isAcceptor, final AttributeDefinition[] attributes) {
            this.isAcceptor = isAcceptor;
            this.attributes = attributes;
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (final AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource)
                throws OperationFailedException {
            super.populateModel(context, operation, resource);

            if(operation.hasDefined(CommonAttributes.PARAM)) {
                for(Property property : operation.get(CommonAttributes.PARAM).asPropertyList()) {
                    final Resource param = context.createResource(PathAddress.pathAddress(PathElement.pathElement(CommonAttributes.PARAM, property.getName())));
                    final ModelNode value = property.getValue();
                    if(! value.isDefined()) {
                        throw new OperationFailedException(new ModelNode().set(MESSAGES.parameterNotDefined(property.getName())));
                    }
                    param.getModel().get(ModelDescriptionConstants.VALUE).set(value);
                }
            }
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            if (isAcceptor) {
                return MessagingDescriptions.getAcceptorAdd(locale, attributes);
            } else {
                return MessagingDescriptions.getConnectorAdd(locale, attributes);
            }
        }
    }
}
