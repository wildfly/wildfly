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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_STACK;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hornetq.api.core.BroadcastEndpointFactoryConfiguration;
import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.JGroupsBroadcastGroupConfiguration;
import org.hornetq.api.core.UDPBroadcastGroupConfiguration;
import org.hornetq.core.config.Configuration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.interfaces.InetAddressUtil;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jgroups.JChannel;

/**
 * Handler for adding a discovery group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DiscoveryGroupAdd extends AbstractAddStepHandler {

    private static final OperationValidator VALIDATOR = new OperationValidator.AttributeDefinitionOperationValidator(DiscoveryGroupDefinition.ATTRIBUTES);

    public static final DiscoveryGroupAdd INSTANCE = new DiscoveryGroupAdd();

    private DiscoveryGroupAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
        VALIDATOR.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        ServiceRegistry registry = context.getServiceRegistry(false);
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = registry.getService(hqServiceName);
        if (hqService != null) {
            context.reloadRequired();
        } else {
            final ServiceTarget target = context.getServiceTarget();
            if(model.hasDefined(JGROUPS_STACK.getName())) {
                // nothing to do, in that case, the clustering.jgroups subsystem will have setup the stack
            } else if(model.hasDefined(RemoteTransportDefinition.SOCKET_BINDING.getName())) {
                final GroupBindingService bindingService = new GroupBindingService();
                target.addService(GroupBindingService.getDiscoveryBaseServiceName(hqServiceName).append(name), bindingService)
                        .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(model.get(SOCKET_BINDING).asString()), SocketBinding.class, bindingService.getBindingRef())
                        .install();
            } else {

                final ModelNode localAddrNode = CommonAttributes.LOCAL_BIND_ADDRESS.resolveModelAttribute(context, model);
                final String localAddress = localAddrNode.isDefined() ? localAddrNode.asString() : null;
                final ModelNode groupAddrNode = CommonAttributes.GROUP_ADDRESS.resolveModelAttribute(context, model);
                final String groupAddress = groupAddrNode.isDefined() ? groupAddrNode.asString() : null;
                final ModelNode groupPortNode = CommonAttributes.GROUP_PORT.resolveModelAttribute(context, model);
                final int groupPort = groupPortNode.isDefined() ? groupPortNode.asInt() : -1;

                try {

                    final InetAddress inet = localAddress != null ? InetAddress.getByName(localAddress) : InetAddressUtil.getLocalHost();
                    final NetworkInterface intf = NetworkInterface.getByInetAddress(inet);
                    final NetworkInterfaceBinding b = new NetworkInterfaceBinding(Collections.singleton(intf), inet);
                    final InetAddress group = InetAddress.getByName(groupAddress);

                    final SocketBinding socketBinding = new SocketBinding(name, -1, false, group, groupPort, b, null, null);

                    final GroupBindingService bindingService = new GroupBindingService();
                    target.addService(GroupBindingService.getDiscoveryBaseServiceName(hqServiceName).append(name), bindingService)
                            .addInjectionValue(bindingService.getBindingRef(), new ImmediateValue<SocketBinding>(socketBinding))
                            .install();

                } catch (Exception e) {
                    throw new OperationFailedException(e.getLocalizedMessage());
                }
            }
        }
    }

    static void addDiscoveryGroupConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.DISCOVERY_GROUP)) {
            Map<String, DiscoveryGroupConfiguration> configs = configuration.getDiscoveryGroupConfigurations();
            if (configs == null) {
                configs = new HashMap<String, DiscoveryGroupConfiguration>();
                configuration.setDiscoveryGroupConfigurations(configs);
            }
            for (Property prop : model.get(CommonAttributes.DISCOVERY_GROUP).asPropertyList()) {
                configs.put(prop.getName(), createDiscoveryGroupConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final long refreshTimeout = DiscoveryGroupDefinition.REFRESH_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final long initialWaitTimeout = DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.resolveModelAttribute(context, model).asLong();
        // Requires runtime service
        return new DiscoveryGroupConfiguration(name, refreshTimeout, initialWaitTimeout, null);
    }

    static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final String name, final DiscoveryGroupConfiguration config, final SocketBinding socketBinding) throws Exception {

        final String localAddress = socketBinding.getAddress().getHostAddress();
        final String groupAddress = socketBinding.getMulticastAddress().getHostAddress();
        final int groupPort = socketBinding.getMulticastPort();
        final long refreshTimeout = config.getRefreshTimeout();
        final long initialWaitTimeout = config.getDiscoveryInitialWaitTimeout();
        final UDPBroadcastGroupConfiguration endpointFactoryConfiguration = new UDPBroadcastGroupConfiguration(groupAddress, groupPort, localAddress, -1);

        return new DiscoveryGroupConfiguration(name, refreshTimeout, initialWaitTimeout, endpointFactoryConfiguration );
    }


    static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final String name, final DiscoveryGroupConfiguration config, final JChannel channel, final String channelName) throws Exception {
        final long refreshTimeout = config.getRefreshTimeout();
        final long initialWaitTimeout = config.getDiscoveryInitialWaitTimeout();
        final BroadcastEndpointFactoryConfiguration endpointFactoryConfiguration = new JGroupsBroadcastGroupConfiguration(channel, channelName);

        return new DiscoveryGroupConfiguration(name, refreshTimeout, initialWaitTimeout, endpointFactoryConfiguration );
    }

}
