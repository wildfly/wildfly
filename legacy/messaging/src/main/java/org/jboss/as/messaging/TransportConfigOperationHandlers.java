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
import static org.jboss.as.messaging.CommonAttributes.HTTP_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.HTTP_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
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
        if (params.hasDefined(HTTP_ACCEPTOR)) {
            for (final Property property : params.get(HTTP_ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config);
                parameters.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
                acceptors.put(acceptorName, new TransportConfiguration(NettyAcceptorFactory.class.getName(), parameters, acceptorName));
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
        if (params.hasDefined(HTTP_CONNECTOR)) {
            for (final Property property : params.get(HTTP_CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config);

                final String binding = HTTPConnectorDefinition.SOCKET_BINDING.resolveModelAttribute(context, config).asString();
                bindings.add(binding);
                parameters.put(HTTPConnectorDefinition.SOCKET_BINDING.getName(), binding);
                parameters.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
                connectors.put(connectorName, new TransportConfiguration(NettyConnectorFactory.class.getName(), parameters, connectorName));
            }
        }
        configuration.setConnectorConfigurations(connectors);
    }
}
