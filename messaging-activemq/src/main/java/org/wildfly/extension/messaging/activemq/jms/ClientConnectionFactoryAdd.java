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

package org.wildfly.extension.messaging.activemq.jms;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA;
import static org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers.isOutBoundSocketBinding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.DiscoveryGroupDefinition;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Update adding a connection factory to the subsystem. The
 * runtime action will create the {@link ClientConnectionFactoryService}.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ClientConnectionFactoryAdd extends AbstractAddStepHandler {

    public static final ClientConnectionFactoryAdd INSTANCE = new ClientConnectionFactoryAdd();

    private ClientConnectionFactoryAdd() {
        super(ClientConnectionFactoryDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(name);
        boolean ha = HA.resolveModelAttribute(context, model).asBoolean(ActiveMQClient.DEFAULT_HA);
        final ModelNode discoveryGroupName = Common.DISCOVERY_GROUP.resolveModelAttribute(context, model);
        JMSFactoryType jmsFactoryType = ConnectionFactoryType.valueOf(ConnectionFactoryAttributes.Regular.FACTORY_TYPE.resolveModelAttribute(context, model).asString()).getType();
        List<String> connectorNames = Common.CONNECTORS.unwrap(context, model);
        ServiceBuilder<?> builder;
        ClientConnectionFactoryService service;
        if (discoveryGroupName.isDefined()) {
            service = new ClientConnectionFactoryService(getDiscoveryGroup(context, discoveryGroupName.asString()), jmsFactoryType, ha);
            builder = context.getServiceTarget().addService(serviceName, service);
        } else {
            Set<String> connectorsSocketBindings = new HashSet<>();
            TransportConfiguration[] transportConfigurations = TransportConfigOperationHandlers.processConnectors(context, connectorNames, connectorsSocketBindings);
            service = new ClientConnectionFactoryService(transportConfigurations, jmsFactoryType, ha);
            builder = context.getServiceTarget().addService(serviceName, service);
            for (final String connectorSocketBinding : connectorsSocketBindings) {
                // find whether the connectorSocketBinding references a SocketBinding or an OutboundSocketBinding
                boolean outbound = isOutBoundSocketBinding(context, connectorSocketBinding);
                if (outbound) {
                    final ServiceName outboundSocketName = OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(connectorSocketBinding);
                     builder.addDependency(outboundSocketName, OutboundSocketBinding.class, service.getOutboundSocketBindingInjector(connectorSocketBinding));
                } else {
                    final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(connectorSocketBinding);
                    builder.addDependency(socketName, SocketBinding.class, service.getSocketBindingInjector(connectorSocketBinding));
                }
            }
        }
        builder.install();
        for (String entry : Common.ENTRIES.unwrap(context, model)) {
            MessagingLogger.ROOT_LOGGER.infof("Referencing %s with JNDI name %s", serviceName, entry);
            BinderServiceUtil.installBinderService(context.getServiceTarget(), entry, service, serviceName);
        }
    }

    static DiscoveryGroupConfiguration getDiscoveryGroup(final OperationContext context, final String name) throws OperationFailedException {
        Resource discoveryGroup = context.readResourceFromRoot(context.getCurrentAddress().getParent().append(PathElement.pathElement(CommonAttributes.DISCOVERY_GROUP, name)), true);
        if (discoveryGroup != null) {
            final long refreshTimeout = DiscoveryGroupDefinition.REFRESH_TIMEOUT.resolveModelAttribute(context, discoveryGroup.getModel()).asLong();
            final long initialWaitTimeout = DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.resolveModelAttribute(context, discoveryGroup.getModel()).asLong();

            return new DiscoveryGroupConfiguration()
                    .setName(name)
                    .setRefreshTimeout(refreshTimeout)
                    .setDiscoveryInitialWaitTimeout(initialWaitTimeout);
        }
        return null;
    }

}
